package cc.zorcc.tenet.serdeproc;

import cc.zorcc.tenet.serde.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 *   Serde processor for handling all the classes annotated with @Serde
 */
public final class SerdeProcessor extends AbstractProcessor {

    /**
     *   Store all the generated classes names
     */
    private final List<String> generatedClasses = new ArrayList<>();

    /**
     *   Tenet will always use the latest supported java version
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     *   Only classes, records, enums with @Serde annotated would be processed
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Serde.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(roundEnv.processingOver()) {
            writeSerdeHintToResources();
        } else {
            doProcessing(roundEnv);
        }
        return true;
    }

    /**
     *   Create a serde.txt under the resources folder for SerdeContext to initialize generated-classes at runtime
     *   if serde.txt already exists in the resources folder, it would be overwritten
     */
    private void writeSerdeHintToResources() {
        if(generatedClasses.isEmpty()) {
            return ;
        }
        Filer filer = processingEnv.getFiler();
        try{
            FileObject target = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "serde.txt");
            Path serdeFilePath = Paths.get(target.toUri());
            try(BufferedWriter writer = Files.newBufferedWriter(serdeFilePath)) {
                for (String c : generatedClasses) {
                    writer.write(c);
                    writer.newLine();
                }
                writer.flush();
            }
        } catch (IOException e) {
            throw new SerdeException("Unable to write hint information to resources folder", e);
        }
    }

    /**
     * The SerdeInfo record represents serialization and deserialization metadata
     * for a specific class. It contains information about the target class, the
     * generated class for serialization/deserialization purposes, and details
     * about the fields and enum constants.
     * @param packageName the package name where the target class is located
     * @param targetClassName the name of the target class to be serialized/deserialized
     * @param generatedClassName the name of the generated class for serialization/deserialization
     * @param fieldInfos the details of the fields in the target class
     * @param enumConstants the details of the enum constants in the target class
     */
    record SerdeInfo(
            String packageName,
            String targetClassName,
            String generatedClassName,
            List<FieldInfo> fieldInfos,
            List<FieldInfo> enumConstants
    ) {

    }

    /**
     * The FieldInfo record represents metadata about a field in a class,
     * including its name, type, and associated attributes.
     * @param fieldName the name of the field
     * @param fieldClass the type of the field
     * @param attrList the attributes associated with the field
     */
    record FieldInfo(
            String fieldName,
            String fieldClass,
            List<Map.Entry<String, String>> attrList
    ) {

    }

    /**
     * Parses the {@code Attr} annotation and splits its values into a list of {@code Map.Entry<String, String>}.
     * Each value in the annotation is expected to be in the format "key:value". If the format is incorrect,
     * a {@code SerdeException} is thrown.
     *
     * @param attr the {@code Attr} annotation containing key-value pairs as strings in the format "key:value"
     * @return a list of {@code Map.Entry<String, String>} representing the parsed key-value pairs
     * @throws SerdeException if a value in the annotation is not in the correct format or if a key or value is missing or blank
     */
    private static List<Map.Entry<String, String>> attrList(Attr attr) {
        if(attr != null) {
            return Arrays.stream(attr.value()).map(attribute -> {
                String[] s = attribute.split(":", 2);
                if(s.length != 2) {
                    throw new SerdeException("Wrong attr format for %s".formatted(attribute));
                }
                String key = s[0];
                if(key == null || key.isBlank()) {
                    throw new SerdeException("Missing required attribute key for %s".formatted(attribute));
                }
                String value = s[1];
                if(value == null || value.isBlank()) {
                    throw new SerdeException("Missing required attribute value for %s".formatted(attribute));
                }
                return Map.entry(key, value);
            }).toList();
        } else {
            return List.of();
        }
    }

    /**
     * Parses a {@code FieldInfo} object from the provided {@code VariableElement}.
     * This method extracts the field name, field type, and associated attributes from
     * the {@code VariableElement}, and constructs a {@code FieldInfo} record.
     *
     * @param element the {@code VariableElement} representing the field to be parsed
     * @return a {@code FieldInfo} record containing the field name, field type, and associated attributes
     * @throws SerdeException if the field name ends with a disallowed suffix or if the {@code Attr} annotation has an incorrect format
     */
    private static FieldInfo parseFieldInfo(VariableElement element) {
        String fieldName = element.getSimpleName().toString();
        if(fieldName.endsWith("Get") || fieldName.endsWith("Set") || fieldName.endsWith("Assign") || fieldName.endsWith("TagMapping")) {
            throw new SerdeException("Invalid field name for element %s with (%s), which conflicts with serde generation".formatted(element, fieldName));
        }
        String fieldClass = element.asType().toString();
        List<Map.Entry<String, String>> attrList = attrList(element.getAnnotation(Attr.class));
        return new FieldInfo(fieldName, fieldClass, attrList);
    }

    /**
     * Parses a {@code SerdeInfo} object from the provided {@code Element}.
     * This method extracts the package name, target class name, and fields (including enum constants)
     * from the {@code Element}, and constructs a {@code SerdeInfo} record.
     *
     * @param element the {@code Element} representing the class or interface to be parsed
     * @return a {@code SerdeInfo} record containing the package name, target class name, generated class name, field information, and enum constants
     * @throws SerdeException if the package name is not found, if the target class name contains illegal characters, or if no fields or enum constants are found
     */
    private SerdeInfo parseSerdeInfo(Element element) {
        String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
        if(packageName == null || packageName.isBlank()) {
            throw new SerdeException("Package name not found");
        }
        String targetClassName = element.getSimpleName().toString();
        if(targetClassName.contains("$") || targetClassName.contains("_")) {
            throw new SerdeException("Illegal targetClassName identifiers found, element can't contain '$' or '_'");
        }
        String generatedClassName = "%s$$Serde".formatted(targetClassName);
        List<FieldInfo> fieldInfos = new ArrayList<>();
        List<FieldInfo> enumConstants = new ArrayList<>();
        for(VariableElement e : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            FieldInfo fieldInfo = parseFieldInfo(e);
            if (e.getKind().equals(ElementKind.ENUM_CONSTANT)) {
                enumConstants.add(fieldInfo);
            } else {
                fieldInfos.add(fieldInfo);
            }
        }
        if(fieldInfos.isEmpty() && enumConstants.isEmpty()) {
            throw new SerdeException("No field or enum constants found for %s, thus making serialization and deserialization meaningless".formatted(targetClassName));
        }
        return new SerdeInfo(packageName, targetClassName, generatedClassName, fieldInfos, enumConstants);
    }

    /**
     *   Processing the elements based on its type, only class, records, and enums are supported, they must be final and not being inner classes
     */
    private void doProcessing(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Serde.class);
        for (Element element : elements) {
            if(!isFinalClass(element)) {
                throw new SerdeException("Element annotated with @Serde must be final : %s".formatted(element));
            }
            if(isInnerClass(element)) {
                throw new SerdeException("Element annotated with @Serde can not be inner class : %s".formatted(element));
            }
            SerdeInfo serdeInfo = parseSerdeInfo(element);
            Source source = new Source(serdeInfo.packageName(), serdeInfo.generatedClassName(), Set.of("Refer<%s>".formatted(serdeInfo.targetClassName())));
            source.registerImport(Refer.class);
            switch (element.getKind()) {
                case ElementKind.CLASS -> {
                    if(!hasNoArgsConstructor(element)) {
                        throw new SerdeException("Default no-arg constructor not found for element : %s".formatted(element));
                    }
                    if(serdeInfo.fieldInfos().isEmpty()) {
                        throw new SerdeException("Fields not found for element : %s".formatted(element));
                    }
                    doProcessingClass(serdeInfo, source);
                }
                case ElementKind.RECORD -> {
                    if(serdeInfo.fieldInfos().isEmpty()) {
                        throw new SerdeException("Fields not found for element : %s".formatted(element));
                    }
                    doProcessingRecord(serdeInfo, source);
                }
                case ElementKind.ENUM -> doProcessingEnum(serdeInfo, source);
                case null, default -> throw new SerdeException("Wrong element kind : %s, class, record, or enum expected".formatted(element.getKind()));
            }
            source.writeToFiler(processingEnv.getFiler());
            generatedClasses.add("%s.%s".formatted(serdeInfo.packageName(), serdeInfo.generatedClassName()));
        }
    }

    /**
     *   Check if current element is a top-level class-file
     */
    private static boolean isInnerClass(Element element) {
        Element enclosingElement = element.getEnclosingElement();
        return enclosingElement != null && enclosingElement.getKind() != ElementKind.PACKAGE;
    }

    /**
     *   Check if current element is a final class
     */
    private static boolean isFinalClass(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    /**
     *   Check if current class has public no-arg constructor
     */
    private static boolean hasNoArgsConstructor(Element element) {
        for (Element enclosedElement : element.getEnclosedElements()) {
            if(enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR) && enclosedElement instanceof ExecutableElement executableElement) {
                if(executableElement.getParameters().isEmpty() && executableElement.getModifiers().contains(Modifier.PUBLIC)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *   Generating singletonBlock
     */
    private static Block singletonBlock(SerdeInfo serdeInfo) {
        return new Block()
                .addLine("private static final %s SINGLETON = new %s();".formatted(serdeInfo.generatedClassName(), serdeInfo.generatedClassName()))
                .newLine()
                .addLine("private %s() {".formatted(serdeInfo.generatedClassName()))
                .newLine()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating objectHandle declaration
     */
    private static Block fieldHandleBlock(FieldInfo fieldInfo, Source source) {
        source.registerImport(VarHandle.class);
        return new Block()
                .addLine("private static final VarHandle %sHandle;".formatted(fieldInfo.fieldName()))
                .newLine();
    }

    /**
     *   Generating objectCol declaration
     */
    private static Block fieldColBlock(SerdeInfo serdeInfo, FieldInfo fieldInfo, Source source) {
        source.registerImport(Col.class);
        return new Block()
                .addLine("private static final Col<%s> %sCol;".formatted(serdeInfo.targetClassName(), fieldInfo.fieldName()))
                .newLine();
    }

    /**
     *   Generating objectTagMapping method
     */
    private static Block fieldTagMappingBlock(FieldInfo fieldInfo) {
        List<Map.Entry<String, String>> entries = fieldInfo.attrList();
        if(entries.isEmpty()) {
            return Block.IGNORED;
        } else {
            Block b = new Block()
                    .addLine("private static String %sTagMapping(String key) {".formatted(fieldInfo.fieldName()))
                    .indent()
                    .addLine("return switch (key) {")
                    .indent();
            for(Map.Entry<String, String> entry : entries) {
                b.addLine("case \"%s\" -> \"%s\";".formatted(entry.getKey(), entry.getValue()));
            }
            return b.addLine("case null, default -> null;")
                    .unindent()
                    .addLine("};")
                    .unindent()
                    .addLine("}")
                    .newLine();
        }
    }

    /**
     *   Generating objectAssign method
     */
    private static Block fieldAssignBlock(SerdeInfo serdeInfo, FieldInfo fieldInfo, Source source, boolean usingSetter) {
        source.registerImport(SerdeException.class);
        return new Block()
                .addLine("private static void %sAssign(Builder<%s> builder, Object value) {".formatted(fieldInfo.fieldName(), serdeInfo.targetClassName()))
                .indent()
                .addLine("if(builder instanceof Wrapper wrapper && value instanceof %s v) {".formatted(fieldInfo.fieldClass()))
                .indent()
                .addLine(usingSetter ?
                        "%sSet(wrapper.instance(), v);".formatted(fieldInfo.fieldName()) :
                        "wrapper.%s = v;".formatted(fieldInfo.fieldName()))
                .unindent()
                .addLine("} else {")
                .indent()
                .addLine("throw new SerdeException(\"Type mismatch\");")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating objectGet method
     */
    private static Block fieldGetBlock(SerdeInfo serdeInfo, FieldInfo fieldInfo, Source source) {
        source.registerImport(SerdeException.class);
        return new Block()
                .addLine("private static %s %sGet(%s instance) {".formatted(fieldInfo.fieldClass(), fieldInfo.fieldName(), serdeInfo.targetClassName()))
                .indent()
                .addLine("return (%s) %sHandle.get(instance);".formatted(fieldInfo.fieldClass(), fieldInfo.fieldName()))
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating objectSet method
     */
    private static Block fieldSetBlock(SerdeInfo serdeInfo, FieldInfo fieldInfo, Source source) {
        source.registerImport(SerdeException.class);
        return new Block()
                .addLine("private static void %sSet(%s instance, %s value) {".formatted(fieldInfo.fieldName(), serdeInfo.targetClassName(), fieldInfo.fieldClass()))
                .indent()
                .addLine("%sHandle.set(instance, value);".formatted(fieldInfo.fieldName()))
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating class wrapper block
     */
    private static Block classWrapperBlock(SerdeInfo serdeInfo) {
        return new Block()
                .addLine("record Wrapper(%s instance) implements Builder<%s> {".formatted(serdeInfo.targetClassName(), serdeInfo.targetClassName()))
                .indent()
                .addLine("Wrapper() {")
                .indent()
                .addLine("this(new %s());".formatted(serdeInfo.targetClassName()))
                .unindent()
                .addLine("}")
                .newLine()
                .addLine("@Override")
                .addLine("public %s build() {".formatted(serdeInfo.targetClassName()))
                .indent()
                .addLine("return instance;")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating record wrapper block
     */
    private static Block recordWrapperBlock(SerdeInfo serdeInfo) {
        Block b = new Block()
                .addLine("private static final class Wrapper implements Builder<%s> {".formatted(serdeInfo.targetClassName()))
                .indent();
        for (FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            b.addLine("private %s %s;".formatted(fieldInfo.fieldClass(), fieldInfo.fieldName()));
        }
        return b.newLine()
                .addLine("@Override")
                .addLine("public %s build() {".formatted(serdeInfo.targetClassName()))
                .indent()
                .addLine("return new %s(%s);".formatted(serdeInfo.targetClassName(), serdeInfo.fieldInfos().stream().map(FieldInfo::fieldName).collect(Collectors.joining(", "))))
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating enum wrapper block
     */
    private static Block enumWrapperBlock(SerdeInfo serdeInfo, Source source) {
        if(serdeInfo.fieldInfos().isEmpty()) {
            return Block.IGNORED;
        }
        source.registerImport(Objects.class);
        Block b = new Block()
                .addLine("private static final class Wrapper implements Builder<%s> {".formatted(serdeInfo.targetClassName()))
                .indent()
                .addLine("private static final %s[] values = %s.values();".formatted(serdeInfo.targetClassName(), serdeInfo.targetClassName()));
        for (FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            b.addLine("private %s %s;".formatted(fieldInfo.fieldClass(), fieldInfo.fieldName()));
        }
        return b.newLine()
                .addLine("@Override")
                .addLine("public %s build() {".formatted(serdeInfo.targetClassName()))
                .indent()
                .addLine("for (%s e : values) {".formatted(serdeInfo.targetClassName()))
                .indent()
                .addLine("if(%s) {".formatted(serdeInfo.fieldInfos().stream().map(f -> "Objects.equals(%sGet(e), %s)".formatted(f.fieldName(), f.fieldName())).collect(Collectors.joining(" && "))))
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
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating builder method
     */
    private static Block builderBlock(SerdeInfo serdeInfo, Source source) {
        source.registerImport(Builder.class);
        return new Block()
                .addLine("@Override")
                .addLine("public Builder<%s> builder() {".formatted(serdeInfo.targetClassName()))
                .indent()
                .addLine(serdeInfo.fieldInfos().isEmpty() ? "return null;" : "return new Wrapper();")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating col method
     */
    private static Block colBlock(SerdeInfo serdeInfo, Source source) {
        source.registerImport(Col.class);
        Block b = new Block()
                .addLine("@Override")
                .addLine("public Col<%s> col(String colName) {".formatted(serdeInfo.targetClassName()))
                .indent();
        if(serdeInfo.fieldInfos().isEmpty()) {
            b.addLine("return null;");
        }else {
            b.addLine("return switch (colName) {")
                    .indent();
            for(FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
                b.addLine("case \"%s\" -> %sCol;".formatted(fieldInfo.fieldName(), fieldInfo.fieldName()));
            }
            b.addLine("case null, default -> null;")
                    .unindent()
                    .addLine("};");
        }
        return b.unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating byName method
     */
    private static Block byNameBlock(SerdeInfo serdeInfo) {
        Block b = new Block()
                .addLine("@Override")
                .addLine("public %s byName(String name) {".formatted(serdeInfo.targetClassName()))
                .indent();
        if(serdeInfo.enumConstants().isEmpty()) {
            b.addLine("return null;");
        }else {
            b.addLine("return switch (name) {")
                    .indent();
            for (FieldInfo fieldInfo : serdeInfo.enumConstants()) {
                b.addLine("case \"%s\" -> %s.%s;".formatted(fieldInfo.fieldName(), serdeInfo.targetClassName(), fieldInfo.fieldName()));
            }
            b.addLine("case null, default -> null;")
                    .unindent()
                    .addLine("};");
        }
        return b.unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating varHandle initialization str
     */
    private static String varHandleStr(SerdeInfo serdeInfo, FieldInfo fieldInfo) {
        return "%sHandle = lookup.findVarHandle(%s.class, \"%s\", %s.class);"
                .formatted(fieldInfo.fieldName(), serdeInfo.targetClassName(), fieldInfo.fieldName(), fieldInfo.fieldClass());
    }

    /**
     *   Generating tagMapping str
     */
    private static String tagMappingStr(SerdeInfo serdeInfo, FieldInfo fieldInfo, Source source) {
        if(fieldInfo.attrList().isEmpty()) {
            source.registerImport(TagMappingFunc.class);
            return "TagMappingFunc.NULLIFY";
        } else {
            return "%s::%sTagMapping".formatted( serdeInfo.generatedClassName(), fieldInfo.fieldName());
        }
    }

    /**
     *   Generating recursiveType construction str
     */
    private static String recursiveTypeStr(String fieldClass) {
        String current = fieldClass;
        String recur = "";
        int len = fieldClass.length();
        if(fieldClass.charAt(len - 1) == '>') {
            // it's a generic type
            int index = fieldClass.indexOf('<');
            if(index <= 0) {
                throw new SerdeException("Invalid field class found: %s".formatted(fieldClass));
            }
            current = fieldClass.substring(0, index);
            recur = recursiveTypeStr(fieldClass.substring(index + 1, len - 1));
        }
        return "new RecursiveType(%s.class, List.of(%s))".formatted(current, recur);
    }

    /**
     *   Generating col construction str
     */
    private static String colStr(SerdeInfo serdeInfo, FieldInfo fieldInfo, Source source) {
        source.registerImports(List.of(List.class, RecursiveType.class));
        return "%sCol = new Col<>(\"%s\", %s, %s, %s::%sAssign, %s::%sGet);"
                .formatted(fieldInfo.fieldName(), fieldInfo.fieldName(), recursiveTypeStr(fieldInfo.fieldClass()),
                        tagMappingStr(serdeInfo, fieldInfo, source), serdeInfo.generatedClassName(), fieldInfo.fieldName(), serdeInfo.generatedClassName(), fieldInfo.fieldName());
    }

    /**
     *   Generating static block statement
     */
    private static Block staticBlock(SerdeInfo serdeInfo, Source source) {
        if(serdeInfo.fieldInfos().isEmpty()) {
            source.registerImport(SerdeContext.class);
            return new Block()
                    .addLine("static {")
                    .indent()
                    .addLine("SerdeContext.registerRefer(%s.class, SINGLETON);".formatted(serdeInfo.targetClassName()))
                    .unindent()
                    .addLine("}")
                    .newLine();
        }
        source.registerImports(List.of(MethodHandles.class, SerdeContext.class, Col.class));
        Block b = new Block()
                .addLine("static {")
                .indent()
                .addLine("try{")
                .indent()
                .addLine("MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(%s.class, MethodHandles.lookup());".formatted(serdeInfo.targetClassName()));
        for(FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            b.addLine(varHandleStr(serdeInfo, fieldInfo));
            b.addLine(colStr(serdeInfo, fieldInfo, source));
        }
        return b.addLine("SerdeContext.registerRefer(%s.class, SINGLETON);".formatted(serdeInfo.targetClassName()))
                .unindent()
                .addLine("} catch (ReflectiveOperationException e) {")
                .indent()
                .addLine("throw new ExceptionInInitializerError(e);")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Processing class based on target serdeInfo
     */
    private void doProcessingClass(SerdeInfo serdeInfo, Source source) {
        source.registerBlocks(List.of(
                singletonBlock(serdeInfo),
                staticBlock(serdeInfo, source),
                classWrapperBlock(serdeInfo),
                builderBlock(serdeInfo, source),
                colBlock(serdeInfo, source),
                byNameBlock(serdeInfo)
            ));
        for (FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            source.registerBlocks(List.of(
                    fieldHandleBlock(fieldInfo, source),
                    fieldColBlock(serdeInfo, fieldInfo, source),
                    fieldTagMappingBlock(fieldInfo),
                    fieldAssignBlock(serdeInfo, fieldInfo, source, true),
                    fieldGetBlock(serdeInfo, fieldInfo, source),
                    fieldSetBlock(serdeInfo, fieldInfo, source)
            ));
        }
    }

    /**
     *   Processing class based on target serdeInfo
     */
    private void doProcessingRecord(SerdeInfo serdeInfo, Source source) {
        source.registerBlocks(List.of(
                singletonBlock(serdeInfo),
                staticBlock(serdeInfo, source),
                recordWrapperBlock(serdeInfo),
                builderBlock(serdeInfo, source),
                colBlock(serdeInfo, source),
                byNameBlock(serdeInfo)
        ));
        for (FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            source.registerBlocks(List.of(
                    fieldHandleBlock(fieldInfo, source),
                    fieldColBlock(serdeInfo, fieldInfo, source),
                    fieldTagMappingBlock(fieldInfo),
                    fieldAssignBlock(serdeInfo, fieldInfo, source ,false),
                    fieldGetBlock(serdeInfo, fieldInfo, source)
            ));
        }
    }

    /**
     *   Processing class based on target serdeInfo
     */
    private void doProcessingEnum(SerdeInfo serdeInfo, Source source) {
        source.registerBlocks(List.of(
                singletonBlock(serdeInfo),
                staticBlock(serdeInfo, source),
                enumWrapperBlock(serdeInfo, source),
                builderBlock(serdeInfo, source),
                colBlock(serdeInfo, source),
                byNameBlock(serdeInfo)
        ));
        for (FieldInfo fieldInfo : serdeInfo.fieldInfos()) {
            source.registerBlocks(List.of(
                    fieldHandleBlock(fieldInfo, source),
                    fieldColBlock(serdeInfo, fieldInfo, source),
                    fieldTagMappingBlock(fieldInfo),
                    fieldAssignBlock(serdeInfo, fieldInfo, source, false),
                    fieldGetBlock(serdeInfo, fieldInfo, source)
            ));
        }
    }

}
