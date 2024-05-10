package cn.zorcc.common.serde;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class SerdeProcessor extends AbstractProcessor {

    private final List<String> generatedClasses = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(roundEnv.processingOver()) {
            writeSerdeHintToResources();
        }else {
            doProcessing(roundEnv);
        }
        return true;
    }

    /**
     *   check if the serde.txt already exists, if so, delete it first
     */
    private void removeExistingSerdeFile() {
        Filer filer = processingEnv.getFiler();
        try {
            FileObject resource = filer.getResource(StandardLocation.CLASS_OUTPUT, "", "serde.txt");
            Path serdeFilePath = Paths.get(resource.toUri());
            if(Files.exists(serdeFilePath)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "serde.txt already exists, replacing it now");
                Files.delete(serdeFilePath);
            }
        }catch (IOException e) {
            // we could just ignore it since the file doesn't exist
        }
    }

    /**
     *   Create a serde.txt under the resources folder for SerdeContext to initialize generated-classes at runtime
     */
    private void writeSerdeHintToResources() {
        removeExistingSerdeFile();
        try{
            Filer filer = processingEnv.getFiler();
            if(!generatedClasses.isEmpty()) {
                FileObject target = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "serde.txt");
                Path serdeFilePath = Paths.get(target.toUri());
                try(BufferedWriter writer = Files.newBufferedWriter(serdeFilePath)) {
                    for (String c : generatedClasses) {
                        writer.write(c);
                        writer.newLine();
                    }
                    writer.flush();
                }
            }
        } catch (IOException e) {
            throw new FrameworkException(ExceptionType.ANNO, "Unable to write hint information to resources folder", e);
        }
    }

    private void doProcessing(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Serde.class);
        for (Element element : elements) {
            if(!element.getModifiers().contains(Modifier.FINAL)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Element must be final", element);
            }
            switch (element.getKind()) {
                case ElementKind.CLASS -> processClass(parseSerdeInfo(element));
                case ElementKind.RECORD -> processRecord(parseSerdeInfo(element));
                case ElementKind.ENUM -> processEnum(parseSerdeInfo(element));
                case null, default -> throw new FrameworkException(ExceptionType.ANNO, "Wrong element kind, class, record, or enum expected");
            }
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Serde.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     *   Creating static block initialization logic
     */
    private static CodeBlock staticBlock(String targetType, String className, List<CodeBlock> accessorBlocks) {
        return new CodeBlock()
                .addLine("static { ")
                .indent()
                .addLine("try {")
                .indent()
                .addLine("MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(%s.class, MethodHandles.lookup());".formatted(targetType))
                .addBlocks(accessorBlocks)
                .addLine("SerdeContext.registerHandle(%s.class, new %s());".formatted(targetType, className))
                .unindent()
                .addLine("} catch (Throwable throwable) { ")
                .indent()
                .addLine("throw new ExceptionInInitializerError(throwable);")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating accessor static field
     */
    private static CodeBlock accessorBlock(String fieldName, String targetType) {
        return new CodeBlock()
                .addLine("private static final Accessor<%s> %sAccessor;".formatted(targetType, fieldName))
                .newLine();
    }

    /**
     *   Creating getter method
     */
    private static CodeBlock getterBlock(String fieldType, String fieldName, String targetType) {
        return new CodeBlock()
                .addLine("private static %s %sGet(%s instance) { ".formatted(fieldType, fieldName, targetType))
                .indent()
                .addLine("try {")
                .indent()
                .addLine("return (%s) %sAccessor.getter().invokeExact(instance);".formatted(fieldType, fieldName))
                .unindent()
                .addLine("} catch (Throwable throwable) { ")
                .indent()
                .addLine("throw new RuntimeException(\"Should never be reached\", throwable);")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating setter method
     */
    private static CodeBlock setterBlock(String fieldType, String fieldName, String targetType) {
        return new CodeBlock()
                .addLine("private static void %sSet(%s instance, %s value) { ".formatted(fieldName, targetType, fieldType))
                .indent()
                .addLine("try {")
                .indent()
                .addLine("%sAccessor.setter().invokeExact(instance, value);".formatted(fieldName))
                .unindent()
                .addLine("} catch (Throwable throwable) { ")
                .indent()
                .addLine("throw new RuntimeException(\"Should never be reached\", throwable);")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating assign method
     */
    private static CodeBlock assignBlock(String fieldType, String fieldName, String targetType, boolean direct) {
        return new CodeBlock()
                .addLine("private static void %sAssign(Supplier<%s> supplier, Object o) { ".formatted(fieldName, targetType))
                .indent()
                .addLine("if(supplier instanceof Wrapper wrapper) { ")
                .indent()
                .addLine(direct ? "%sSet(wrapper.instance(), (%s) o);".formatted(fieldName, fieldType) : "wrapper.%s = (%s) o;".formatted(fieldName, fieldType))
                .unindent()
                .addLine("} else { ")
                .indent()
                .addLine("throw new RuntimeException(\"Should never be reached\");")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating tag method, field without @Attr() annotated won't be generated by returning CodeBlock.IGNORED
     */
    private static CodeBlock tagBlock(String fieldName, List<Map.Entry<String, String>> attrs) {
        if(attrs.isEmpty()) {
            return CodeBlock.IGNORED;
        }
        CodeBlock c = new CodeBlock()
                .addLine("private static String %sTag(String key) { ".formatted(fieldName))
                .indent()
                .addLine("return switch(key) { ")
                .indent();
        for (Map.Entry<String, String> attr : attrs) {
            c.addLine("case \"%s\" -> \"%s\";".formatted(attr.getKey(), attr.getValue()));
        }
        c.addLine("case null, default -> null;")
                .unindent()
                .addLine("};")
                .unindent()
                .addLine("}")
                .newLine();
        return c;
    }

    /**
     *   Creating wrapper for normal class
     */
    private static CodeBlock classWrapperBlock(String targetType) {
        return new CodeBlock()
                .addLine("record Wrapper(%s instance) implements Supplier<%s> { ".formatted(targetType, targetType))
                .indent()
                .addLine("@Override")
                .addLine("public %s get() {".formatted(targetType))
                .indent()
                .addLine("return instance;")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating wrapper for record
     */
    private static CodeBlock recordWrapperBlock(String targetType, List<FieldInfo> fieldInfos) {
        CodeBlock codeBlock = new CodeBlock()
                .addLine("private static final class Wrapper implements Supplier<%s> { ".formatted(targetType))
                .indent();
        for (FieldInfo fieldInfo : fieldInfos) {
            codeBlock.addLine("private %s %s;".formatted(fieldInfo.fieldType(), fieldInfo.fieldName()));
        }
        return codeBlock.newLine()
                .addLine("@Override")
                .addLine("public %s get() { ".formatted(targetType))
                .indent()
                .addLine("return new %s(%s);".formatted(targetType, fieldInfos.stream().map(FieldInfo::fieldName).collect(Collectors.joining(", "))))
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating wrapper for enum
     */
    private static CodeBlock enumWrapperBlock(String targetType, List<FieldInfo> fieldInfos) {
        CodeBlock codeBlock = new CodeBlock()
                .addLine("private static final class Wrapper implements Supplier<%s> { ".formatted(targetType))
                .indent()
                .addLine("private static final %s[] values = %s.values();".formatted(targetType, targetType));
        List<String> expr = new ArrayList<>();
        for (FieldInfo fieldInfo : fieldInfos) {
            codeBlock.addLine("private %s %s;".formatted(fieldInfo.fieldType(), fieldInfo.fieldName()));
            expr.add("Objects.equals(%sGet(e), %s)".formatted(fieldInfo.fieldName(), fieldInfo.fieldName()));
        }
        return codeBlock.newLine()
                .addLine("@Override")
                .addLine("public %s get() { ".formatted(targetType))
                .indent()
                .addLine("for (%s e : values) { ".formatted(targetType))
                .indent()
                .addLine("if(%s) { ".formatted(String.join(" && ", expr)))
                .indent()
                .addLine("return e;")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .addLine("return null;")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}");
    }

    /**
     *   Creating assigner function for class
     */
    private static CodeBlock classAssignerBlock(String targetType) {
        return new CodeBlock()
                .addLine("@Override")
                .addLine("public Supplier<%s> createAssigner() {".formatted(targetType))
                .indent()
                .addLine("return new Wrapper(new %s());".formatted(targetType))
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating createAssigner() function for record or enum
     */
    private static CodeBlock recordAndEnumAssigner(String targetType) {
        return new CodeBlock()
                .addLine("@Override")
                .addLine("public Supplier<%s> createAssigner() {".formatted(targetType))
                .indent()
                .addLine("return new Wrapper();")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating col() function
     */
    private static CodeBlock colBlock(String targetType, List<FieldInfo> fieldInfos) {
        CodeBlock c = new CodeBlock()
                .addLine("@Override")
                .addLine("public Column<%s> col(String columnName) { ".formatted(targetType))
                .indent()
                .addLine("return switch (columnName) {")
                .indent();
        for (FieldInfo f : fieldInfos) {
            c.addLine("case \"%s\" -> %sAccessor.column();".formatted(f.fieldName(), f.fieldName()));
        }
        c.addLine("case null, default -> null;")
                .unindent()
                .addLine("};")
                .unindent()
                .addLine("}")
                .newLine();
        return c;
    }

    /**
     *   Creating byName() function for class or record
     */
    private static CodeBlock byNameBlock(String targetType) {
        return new CodeBlock()
                .addLine("@Override")
                .addLine("public %s byName(String name) { ".formatted(targetType))
                .indent()
                .addLine("throw new UnsupportedOperationException();")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Creating byName() function for enum
     */
    private static CodeBlock enumByNameBlock(String targetType, List<FieldInfo> enumInfos) {
        CodeBlock c = new CodeBlock()
                .addLine("@Override")
                .addLine("public %s byName(String name) { ".formatted(targetType))
                .indent()
                .addLine("return switch (name) { ")
                .indent();
        for (FieldInfo fieldInfo : enumInfos) {
            c.addLine("case \"%s\" -> %s.%s;".formatted(fieldInfo.fieldName(), targetType, fieldInfo.fieldName()));
        }
        return c.addLine("case null, default -> null;")
                .unindent()
                .addLine("};")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *
     */
    private static List<Map.Entry<String, String>> attrList(Attr attr) {
        if(attr != null) {
            return Arrays.stream(attr.values()).map(attribute -> {
                String[] s = attribute.split(":");
                if(s.length != 2) {
                    throw new FrameworkException(ExceptionType.ANNO, "Wrong attr format for %s".formatted(attribute));
                }
                return Map.entry(s[0], s[1]);
            }).toList();
        } else {
            return List.of();
        }
    }

    private static String toRecurClass(String fieldType) {
        String current = fieldType;
        String recur = "";
        int len = fieldType.length();
        if(fieldType.charAt(len - 1) == '>') {
            // it's a generic type
            int index = fieldType.indexOf('<');
            if(index <= 0) {
                throw new FrameworkException(ExceptionType.ANNO, Constants.UNREACHED);
            }
            current = fieldType.substring(0, index);
            recur = toRecurClass(fieldType.substring(index + 1, len - 1));
        }
        return "new RecurClass(%s.class, List.of(%s))".formatted(current, recur);
    }

    private static String getterStr(String fieldType, String fieldName, String targetType) {
        return "lookup.findGetter(%s.class, \"%s\", %s.class),".formatted(targetType, fieldName, fieldType);
    }

    private static String setterStr(String fieldType, String fieldName, String targetType) {
        return "lookup.findSetter(%s.class, \"%s\", %s.class),".formatted(targetType, fieldName, fieldType);
    }

    private static String columnStr(String fieldType, String fieldName, String className, boolean missingTag) {
        return "new Column<>(\"%s\", %s, %s, %s, %s)".formatted(fieldName,
                toRecurClass(fieldType),
                missingTag ? "Column.EMPTY_TAG" : "%s::%sTag".formatted(className, fieldName),
                "%s::%sAssign".formatted(className, fieldName),
                "%s::%sGet".formatted(className, fieldName));
    }

    private static CodeBlock accessorWithGetterAndSetter(String fieldType, String fieldName, String targetType, String className, boolean missingTag) {
        String getterStr = getterStr(fieldType, fieldName, targetType);
        String setterStr = setterStr(fieldType, fieldName, targetType);
        String columnStr = columnStr(fieldType, fieldName, className, missingTag);
        return new CodeBlock()
                .addLine("%sAccessor = new Accessor<>( ".formatted(fieldName))
                .indent()
                .addLine(getterStr)
                .addLine(setterStr)
                .addLine(columnStr)
                .unindent()
                .addLine(");");
    }

    private static CodeBlock accessorWithGetter(String fieldType, String fieldName, String targetType, String className, boolean missingTag) {
        String getterStr = getterStr(fieldType, fieldName, targetType);
        String columnStr = columnStr(fieldType, fieldName, className, missingTag);
        return new CodeBlock()
                .addLine("%sAccessor = new Accessor<>( ".formatted(fieldName))
                .indent()
                .addLine(getterStr)
                .addLine(columnStr)
                .unindent()
                .addLine(");");
    }

    record FieldInfo(
            String fieldName,
            String fieldType,
            List<Map.Entry<String, String>> attrList
    ) {

    }

    /**
     *   Parsing fieldInfo, there is no limitation on fieldName or fieldType, as long as Javac allow it
     */
    private static FieldInfo parseFieldInfo(VariableElement element) {
        String fieldName = element.getSimpleName().toString();
        String fieldType = element.asType().toString();
        List<Map.Entry<String, String>> attrList = attrList(element.getAnnotation(Attr.class));
        return new FieldInfo(fieldName, fieldType, attrList);
    }

    record SerdeInfo(
            String packageName,
            String targetType,
            String className,
            List<FieldInfo> fieldInfos,
            List<FieldInfo> enumInfos
    ) {

        /**
         *   Generating a common generation skeleton
         */
        Source createSource() {
            Source source = new Source(packageName(), className(), Set.of("Handle<%s>".formatted(targetType())));
            source.registerImports(Handle.class, MethodHandle.class, MethodHandles.class, List.class, Supplier.class, Accessor.class, Column.class, RecurClass.class, SerdeContext.class, Objects.class);
            return source;
        }
    }

    private SerdeInfo parseSerdeInfo(Element element) {
        String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
        if(packageName == null || packageName.isBlank()) {
            throw new FrameworkException(ExceptionType.ANNO, "Empty package name found");
        }
        String targetType = element.getSimpleName().toString();
        if(targetType.contains("$") || targetType.contains("_")) {
            throw new FrameworkException(ExceptionType.ANNO, "Illegal targetType found");
        }
        String className = "%s$$Serde".formatted(targetType);
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        List<FieldInfo> enumInfoList = new ArrayList<>();
        for(VariableElement e : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            FieldInfo fieldInfo = parseFieldInfo(e);
            if (e.getKind().equals(ElementKind.ENUM_CONSTANT)) {
                enumInfoList.add(fieldInfo);
            } else {
                fieldInfoList.add(fieldInfo);
            }
        }
        if(fieldInfoList.isEmpty()) {
            throw new FrameworkException(ExceptionType.ANNO, "No field found for %s, thus making serialization and deserialization meaningless", targetType);
        }
        return new SerdeInfo(packageName, targetType, className, fieldInfoList, enumInfoList);
    }

    private void processClass(SerdeInfo serdeInfo) {
        Source source = serdeInfo.createSource();
        List<CodeBlock> accessors = new ArrayList<>();
        for (FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            source.registerBlock(accessorBlock(fieldInfo.fieldName(), serdeInfo.targetType()));
            source.registerBlock(getterBlock(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType()));
            source.registerBlock(setterBlock(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType()));
            source.registerBlock(assignBlock(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType(), true));
            source.registerBlock(tagBlock(fieldInfo.fieldName(), fieldInfo.attrList()));
            accessors.add(accessorWithGetterAndSetter(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType(), serdeInfo.className(), fieldInfo.attrList().isEmpty()));
        }
        source.registerBlock(classWrapperBlock(serdeInfo.targetType()));
        source.registerBlock(classAssignerBlock(serdeInfo.targetType()));
        source.registerBlock(colBlock(serdeInfo.targetType(), serdeInfo.fieldInfos()));
        source.registerBlock(byNameBlock(serdeInfo.targetType()));
        source.registerBlock(staticBlock(serdeInfo.targetType(), serdeInfo.className(), accessors));
        source.writeToFiler(processingEnv.getFiler());
        generatedClasses.add("%s.%s".formatted(serdeInfo.packageName(), serdeInfo.className()));
    }

    private void processRecord(SerdeInfo serdeInfo) {
        Source source = serdeInfo.createSource();
        List<CodeBlock> accessors = new ArrayList<>();
        for (FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            source.registerBlock(accessorBlock(fieldInfo.fieldName(), serdeInfo.targetType()));
            source.registerBlock(getterBlock(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType()));
            source.registerBlock(assignBlock(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType(), false));
            source.registerBlock(tagBlock(fieldInfo.fieldName(), fieldInfo.attrList()));
            accessors.add(accessorWithGetter(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType(), serdeInfo.className(), fieldInfo.attrList().isEmpty()));
        }
        source.registerBlock(recordWrapperBlock(serdeInfo.targetType(), serdeInfo.fieldInfos()));
        source.registerBlock(recordAndEnumAssigner(serdeInfo.targetType()));
        source.registerBlock(colBlock(serdeInfo.targetType(), serdeInfo.fieldInfos()));
        source.registerBlock(byNameBlock(serdeInfo.targetType()));
        source.registerBlock(staticBlock(serdeInfo.targetType(), serdeInfo.className(), accessors));
        source.writeToFiler(processingEnv.getFiler());
        generatedClasses.add("%s.%s".formatted(serdeInfo.packageName(), serdeInfo.className()));
    }

    private void processEnum(SerdeInfo serdeInfo) {
        Source source = serdeInfo.createSource();
        List<CodeBlock> accessors = new ArrayList<>();
        for (FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            source.registerBlock(accessorBlock(fieldInfo.fieldName(), serdeInfo.targetType()));
            source.registerBlock(getterBlock(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType()));
            source.registerBlock(assignBlock(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType(), false));
            source.registerBlock(tagBlock(fieldInfo.fieldName(), fieldInfo.attrList()));
            accessors.add(accessorWithGetter(fieldInfo.fieldType(), fieldInfo.fieldName(), serdeInfo.targetType(), serdeInfo.className(), fieldInfo.attrList().isEmpty()));
        }
        source.registerBlock(enumWrapperBlock(serdeInfo.targetType(), serdeInfo.fieldInfos()));
        source.registerBlock(recordAndEnumAssigner(serdeInfo.targetType()));
        source.registerBlock(colBlock(serdeInfo.targetType(), serdeInfo.fieldInfos()));
        source.registerBlock(enumByNameBlock(serdeInfo.targetType(), serdeInfo.enumInfos()));
        source.registerBlock(staticBlock(serdeInfo.targetType(), serdeInfo.className(), accessors));
        source.writeToFiler(processingEnv.getFiler());
        generatedClasses.add("%s.%s".formatted(serdeInfo.packageName(), serdeInfo.className()));
    }
}
