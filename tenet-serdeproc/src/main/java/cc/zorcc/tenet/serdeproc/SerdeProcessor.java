package cc.zorcc.tenet.serdeproc;

import cc.zorcc.tenet.serde.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 *   Serde processor for handling all the classes annotated with @Serde
 */
public final class SerdeProcessor extends AbstractProcessor {
    /// Threshold when converting if-else statements to switch statements
    private static final int SWITCH_THRESHOLD = 3;

    /// Override serde processor by manually assign generated classes
    /// Example format would be `serde.override=org.example.A:org.example.B,org.example.C:org.example.D`
    private static final String OVERRIDE = "serde.override";

    private Elements elements;

    private Types types;

    private Filer filer;
    /**
     *   Store all the generated classes names
     */
    private final List<String> generatedClasses = new ArrayList<>();

    /**
     *   Tenet-serde will always use the latest supported java version
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     *   Tenet-serde support manually overriding generated classes
     */
    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(OVERRIDE);
    }

    /**
     *   Only classes, records, enums with @Serde annotated would be processed
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Serde.class.getCanonicalName());
    }

    @Override
    public synchronized void init(@NonNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment roundEnv) {
        if(roundEnv.processingOver()) {
            writeSerdeHintToResources();
        } else {
            Map<String, String> overrideEntries = Map.of();
            String overrideOptions = processingEnv.getOptions().get(OVERRIDE);
            if(overrideOptions != null && !overrideOptions.isBlank()) {
                overrideEntries = Arrays.stream(overrideOptions.split(",")).map(SerdeConvention::toEntry).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            doProcessing(roundEnv, overrideEntries);
        }
        return true;
    }

    /**
     *   Create a serde.txt under the resources folder for SerdeContext to initialize generated-classes at runtime
     *   if serde.txt already exists in the resources folder, it would be first deleted then overwritten
     */
    private void writeSerdeHintToResources() {
        Filer filer = processingEnv.getFiler();
        try {
            FileObject currentFile = filer.getResource(StandardLocation.CLASS_OUTPUT, "", "serde.txt");
            if(currentFile != null) {
                currentFile.delete();
            }
        } catch (IOException e) {
            throw new SerdeException("Unable to remove the old serde.txt file", e);
        }

        if(generatedClasses.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Serde processor doesn't locate any classes, records or enums annotated with @Serde, skip generating serde.txt resource file");
            return ;
        }

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

    private static FieldData parseFieldData(VariableElement e) {
        String fieldName = SerdeConvention.elementName(e);
        String fieldType = SerdeConvention.typeName(e);
        String fieldClass = SerdeConvention.typeToClass(fieldType);
        List<Map.Entry<String, String>> attrList = SerdeConvention.attrList(e.getAnnotation(Attr.class));
        return new FieldData(e, fieldName, fieldType, fieldClass, attrList);
    }

    private SerdeData parseSerdeData(Element element) {
        String packageName = SerdeConvention.packageName(element, elements);
        String targetClassName = SerdeConvention.elementName(element);
        String generatedClassName = SerdeConvention.generateClassName(targetClassName);
        GenericData genericData = SerdeConvention.genericData(element);
        List<FieldData> fieldInfos = new ArrayList<>();
        List<FieldData> enumConstants = new ArrayList<>();
        for(VariableElement e : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            FieldData fieldInfo = parseFieldData(e);
            switch (e.getKind()) {
                case FIELD -> fieldInfos.add(fieldInfo);
                case ENUM_CONSTANT -> enumConstants.add(fieldInfo);
                default -> throw new SerdeException("Invalid field type for element: %s".formatted(element));
            }
        }
        return new SerdeData(element, packageName, targetClassName, generatedClassName, genericData, fieldInfos, enumConstants);
    }

    /**
     *   Processing the elements based on its type, only class, records, and enums are supported, they must be final and not being inner classes
     */
    private void doProcessing(RoundEnvironment roundEnv, Map<String, String> overrideEntries) {
        Set<? extends Element> allElements = roundEnv.getElementsAnnotatedWith(Serde.class);
        for (Element element : allElements) {
            // perform access checking first
            SerdeConvention.check(element, types, elements);
            SerdeData serdeData = parseSerdeData(element);
            // check if current element could be override
            String overrideClass = overrideEntries.get(serdeData.targetClassName());
            if(overrideClass != null && !overrideClass.isBlank()) {
                TypeElement overrideTypeElement = elements.getTypeElement(overrideClass);
                if(overrideTypeElement == null) {
                    throw new SerdeException("Override element not found : %s".formatted(overrideClass));
                }
                AimAt aimAt = overrideTypeElement.getAnnotation(AimAt.class);
                if(aimAt == null) {
                    throw new SerdeException("Override element must be annotated with aimAt : %s".formatted(overrideClass));
                }
                String target = aimAt.target();
                if(!serdeData.targetClassName().equals(target)) {
                    throw new SerdeException("Target class name mismatch : %s".formatted(target));
                }
                generatedClasses.add(overrideClass);
            } else {
                Source source = new Source(serdeData.packageName(), serdeData.generatedClassName());
                source.registerImports(MethodHandles.class, VarHandle.class, List.class, AtomicBoolean.class, Objects.class); // Register some essential classes
                source.registerPackageImports(SerdeContext.class.getPackageName()); // Register all the classes under serde package
                writeSource(element, serdeData, source);
                source.writeToFiler(filer);
                generatedClasses.add("%s.%s".formatted(serdeData.packageName(), serdeData.generatedClassName()));
            }
        }
    }

    /**
     *   Generating initialized and singleton block
     */
    private static Block singletonBlock(SerdeData s) {
        return new Block()
                .indent()
                .addLine("private static final AtomicBoolean initialized = new AtomicBoolean(false);")
                .addLine("private static final %s%s SINGLETON = new %s();".formatted(s.generatedClassName(), s.genericData().holderGenericType(), s.generatedClassName()))
                .newLine();
    }

    /**
     *   Generating constructor block
     */
    private static Block constructorBlock(SerdeData s) {
        return new Block()
                .indent()
                .addLine("private %s() {".formatted(s.generatedClassName()))
                .indent()
                .addLine("if(!initialized.compareAndSet(false, true)) {")
                .indent()
                .addLine("throw new IllegalStateException(\"%s already initialized\");".formatted(s.generatedClassName()))
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating objectHandle declaration
     */
    private static Block fieldHandleBlock(FieldData f) {
        return new Block()
                .indent()
                .addLine("private static final VarHandle %s;".formatted(SerdeConvention.generateHandleName(f)))
                .newLine();
    }

    /**
     *   Generating objectCol declaration
     */
    private static Block fieldColBlock(SerdeData s, FieldData f) {
        return new Block()
                .indent()
                .addLine("private final Col<%s%s> %s = %s;".formatted(
                    s.targetClassName(),
                    s.genericData().simpleGenericType(),
                    SerdeConvention.generateColName(f),
                    SerdeConvention.generateColExpression(f, s)
                ))
                .newLine();
    }

    private static Block fieldTagMappingBlock(FieldData f) {
        Block b = new Block()
                .indent()
                .addLine("private static String %s(String key) {".formatted(SerdeConvention.generateTagMappingName(f)))
                .indent();
        List<Map.Entry<String, String>> entries = f.attrList();
        if(entries.isEmpty()) {
            return Block.IGNORED;
        }else if (entries.size() < SWITCH_THRESHOLD) {
            for(int i = 0; i < entries.size(); i++) {
                Map.Entry<String, String> e = entries.get(i);
                if(i == 0) {
                    b.addLine("if(key.equals(\"%s\")) return \"%s\";".formatted(e.getKey(), e.getValue()));
                }else {
                    b.addLine("else if(key.equals(\"%s\")) return \"%s\";".formatted(e.getKey(), e.getValue()));
                }
            }
            b.addLine("else return null;");
        }else {
            // using switch statements when entries are many
            b.addLine("return switch (key) {")
                .indent();
            for(Map.Entry<String, String> entry : entries) {
                b.addLine("case \"%s\" -> \"%s\";".formatted(entry.getKey(), entry.getValue()));
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
     *   Generating objectAssign method
     */
    private static Block fieldAssignBlock(SerdeData s, FieldData f) {
        Block block = new Block().indent();
        if(!f.fieldType().equals(f.fieldClass())) {
            block.addLine("@SuppressWarnings(\"unchecked\")");
        }
        return block
                .addLine("private static %s void %s(Builder<%s%s> builder, Object value) {".formatted(s.genericData().fullGenericType(), SerdeConvention.generateAssignName(f), s.targetClassName(), s.genericData().simpleGenericType()))
                .indent()
                .addLine("if(builder instanceof Wrapper%s wrapper) {".formatted(s.genericData().simpleGenericType()))
                .indent()
                .addLine(SerdeConvention.generateAssignExpression(f, s))
                .unindent()
                .addLine("} else {")
                .indent()
                .addLine("throw new ClassCastException(\"class %s cannot be cast to class wrapper\".formatted(builder.getClass()));")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating objectGet method
     */
    private static Block fieldGetBlock(SerdeData s, FieldData f) {
        Block block = new Block().indent();
        if(!f.fieldType().equals(f.fieldClass())) {
            block.addLine("@SuppressWarnings(\"unchecked\")");
        }
        return block
                .addLine("private static %s %s %s(%s%s instance) {".formatted(
                        s.genericData().fullGenericType(),
                        f.fieldType(),
                        SerdeConvention.generateGetName(f),
                        s.targetClassName(),
                        s.genericData().simpleGenericType()
                    ))
                .indent()
                .addLine("return (%s) %s.get(instance);".formatted(f.fieldType(), SerdeConvention.generateHandleName(f)))
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating objectSet method
     */
    private static Block fieldSetBlock(SerdeData s, FieldData f) {
        return new Block()
                .indent()
                .addLine("private static %s void %s(%s%s instance, %s value) {".formatted(
                        s.genericData().fullGenericType(),
                        SerdeConvention.generateSetName(f),
                        s.targetClassName(),
                        s.genericData().simpleGenericType(),
                        f.fieldType()
                ))
                .indent()
                .addLine("%s.set(instance, value);".formatted(SerdeConvention.generateHandleName(f)))
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating class wrapper block
     */
    private static Block classWrapperBlock(SerdeData s) {
        return new Block()
                .indent()
                .addLine("private record Wrapper%s(%s%s instance, AtomicBoolean flag) implements Builder<%s%s> {".formatted(
                        s.genericData().fullGenericType(),
                        s.targetClassName(),
                        s.genericData().simpleGenericType(),
                        s.targetClassName(),
                        s.genericData().simpleGenericType()
                ))
                .indent()
                .addLine("Wrapper() {")
                .indent()
                .addLine("this(new %s%s(), new AtomicBoolean(false));".formatted(s.targetClassName(), s.genericData().emptyGenericType()))
                .unindent()
                .addLine("}")
                .newLine()
                .addLine("@Override")
                .addLine("public %s%s build() {".formatted(s.targetClassName(), s.genericData().simpleGenericType()))
                .indent()
                .addLine("if(flag.compareAndSet(false, true)) return instance;")
                .addLine("else throw new IllegalCallerException(\"build() should only be invoked once\");")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating record wrapper block
     */
    private static Block recordWrapperBlock(SerdeData s) {
        Block b = new Block()
                .indent()
                .addLine("private static final class Wrapper%s implements Builder<%s%s> {".formatted(
                        s.genericData().fullGenericType(),
                        s.targetClassName(),
                        s.genericData().simpleGenericType()
                ))
                .indent()
                .addLine("private final AtomicBoolean flag = new AtomicBoolean(false);");
        for (FieldData f : s.fieldDataList()) {
            b.addLine("private %s %s;".formatted(f.fieldType(), f.fieldName()));
        }
        return b.newLine()
                .addLine("@Override")
                .addLine("public %s%s build() {".formatted(s.targetClassName(), s.genericData().simpleGenericType()))
                .indent()
                .addLine("if(flag.compareAndSet(false, true)) return new %s%s(%s);".formatted(
                        s.targetClassName(),
                        s.genericData().emptyGenericType(),
                        s.fieldDataList().stream().map(FieldData::fieldName).collect(Collectors.joining(", "))
                ))
                .addLine("else throw new IllegalCallerException(\"build() should only be invoked once\");")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private static Block enumWrapperBlock(SerdeData s) {
        if(s.fieldDataList().isEmpty()) {
            return Block.IGNORED;
        } else {
            Block block = new Block()
                    .indent()
                    .addLine("private static final class Wrapper implements Builder<%s> {".formatted(s.targetClassName()))
                    .indent()
                    .addLine("private static final %s[] values = %s.values();".formatted(s.targetClassName(), s.targetClassName()))
                    .addLine("private final AtomicBoolean flag = new AtomicBoolean(false);");
            List<String> eqList = new ArrayList<>();
            for (FieldData f : s.fieldDataList()) {
                block.addLine("private %s %s;".formatted(f.fieldType(), f.fieldName()));
                eqList.add("Objects.equals(%s(value), %s)".formatted(SerdeConvention.generateGetName(f), f.fieldName()));
            }
            return block.addLine("@Override")
                    .addLine("public %s build() {".formatted(s.targetClassName()))
                    .indent()
                    .addLine("if(flag.compareAndSet(false, true)) {")
                    .indent()
                    .addLine("for (%s value : values) {".formatted(s.targetClassName()))
                    .indent()
                    .addLine("if(%s) {".formatted(String.join(" && \n", eqList)))
                    .indent()
                    .addLine("return value;")
                    .unindent()
                    .addLine("}")
                    .unindent()
                    .addLine("}")
                    .addLine("return null;")
                    .unindent()
                    .addLine("}else throw new IllegalCallerException(\"build() should only be invoked once\");")
                    .unindent()
                    .addLine("}")
                    .unindent()
                    .addLine("}")
                    .newLine();
        }
    }

    private static Block fieldsBlock(SerdeData s) {
        return new Block()
                .indent()
                .addLine("private static final List<String> FIELDS = List.of(%s);".formatted(
                        s.fieldDataList().stream().map(f -> "\"%s\"".formatted(f.fieldName())).collect(Collectors.joining(", ")))
                )
                .newLine()
                .addLine("@Override")
                .addLine("public List<String> fields() {")
                .indent()
                .addLine("return FIELDS;")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private static Block builderBlock(SerdeData s) {
        return new Block()
                .indent()
                .addLine("@Override")
                .addLine("public Builder<%s%s> builder() {".formatted(s.targetClassName(), s.genericData().simpleGenericType()))
                .indent()
                .addLine(s.element().getKind() == ElementKind.ENUM && s.fieldDataList().isEmpty() ? "return () -> null;" : "return new Wrapper();")
                .unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating col method
     */
    private static Block colBlock(SerdeData s) {
        Block b = new Block()
                .indent()
                .addLine("@Override")
                .addLine("public Col<%s%s> col(String colName) {".formatted(s.targetClassName(), s.genericData().simpleGenericType()))
                .indent();
        List<FieldData> fs = s.fieldDataList();
        if(fs.isEmpty()) {
            b.addLine("return null;");
        }else if(fs.size() < SWITCH_THRESHOLD) {
            for(int i = 0; i < fs.size(); i++) {
                FieldData f = fs.get(i);
                if(i == 0) {
                    b.addLine("if(colName.equals(\"%s\")) return %s;".formatted(f.fieldName(), SerdeConvention.generateColName(f)));
                }else {
                    b.addLine("else if(colName.equals(\"%s\")) return %s;".formatted(f.fieldName(), SerdeConvention.generateColName(f)));
                }
            }
            b.addLine("else return null;");
        } else {
            b.addLine("return switch (colName) {")
                    .indent();
            for(FieldData f : fs) {
                b.addLine("case \"%s\" -> %s;".formatted(f.fieldName(), SerdeConvention.generateColName(f)));
            }
            b.addLine("default -> null;")
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
    private static Block byNameBlock(SerdeData s) {
        Block block = new Block()
                .indent()
                .addLine("@Override")
                .addLine("public %s byName(String name) {".formatted(s.targetClassName()))
                .indent();
        List<FieldData> enumConstants = s.enumConstantList();
        if(enumConstants.isEmpty()) {
            block.addLine("return null;");
        }else if(enumConstants.size() < SWITCH_THRESHOLD) {
            for(int i = 0; i < enumConstants.size(); i++) {
                FieldData f = enumConstants.get(i);
                if(i == 0) {
                    block.addLine("if(name.equals(\"%s\")) return %s;".formatted(f.fieldName(), SerdeConvention.generateEnumConstantExpression(f, s)));
                }else {
                    block.addLine("else if(name.equals(\"%s\")) return %s;".formatted(f.fieldName(), SerdeConvention.generateEnumConstantExpression(f, s)));
                }
            }
            block.addLine("else return null;");
        }else {
            block.addLine("return switch (name) {")
                    .indent();
            for (FieldData f : enumConstants) {
                block.addLine("case \"%s\" -> %s;".formatted(f.fieldName(), SerdeConvention.generateEnumConstantExpression(f, s)));
            }
            block.addLine("default -> null;")
                    .unindent()
                    .addLine("};");
        }
        return block.unindent()
                .addLine("}")
                .newLine();
    }

    /**
     *   Generating static block statement
     */
    private static Block staticBlock(SerdeData s) {
        Block block = new Block().indent().addLine("static {").indent();
        List<FieldData> fs = s.fieldDataList();
        if(fs.isEmpty()) {
            block.addLine("SerdeContext.registerRefer(%s.class, SINGLETON);".formatted(s.targetClassName()));
        } else {
            block.addLine("try {")
                    .indent()
                    .addLine("var lookup = MethodHandles.privateLookupIn(%s.class, MethodHandles.lookup());".formatted(s.targetClassName()));
            for (FieldData f : fs) {
                block.addLine("%s = lookup.findVarHandle(%s.class, \"%s\", %s.class);".formatted(
                        SerdeConvention.generateHandleName(f),
                        s.targetClassName(),
                        f.fieldName(),
                        f.fieldClass()
                ));
            }
            block.addLine("SerdeContext.registerRefer(%s.class, SINGLETON);".formatted(s.targetClassName()))
                    .unindent()
                    .addLine("} catch (ReflectiveOperationException e) {")
                    .indent()
                    .addLine("throw new ExceptionInInitializerError(e);")
                    .unindent()
                    .addLine("}");
        }
        return block.unindent().addLine("}").newLine();
    }

    private static Block definitionBlock(SerdeData s) {
        return new Block()
                .addLine("@AimAt(target = \"%s\")".formatted(s.packageName() + '.' + s.targetClassName()))
                .addLine("public final class %s%s implements Refer<%s%s> {".formatted(
                        s.generatedClassName(),
                        s.genericData().fullGenericType(),
                        s.targetClassName(),
                        s.genericData().simpleGenericType()
                    ))
                .newLine();
    }

    private static Block closureBlock() {
        return new Block().addLine("}").newLine();
    }

    private void writeSource(Element element, SerdeData s, Source source) {
        Block wrapperBlock = switch (element.getKind()) {
            case ElementKind.CLASS -> classWrapperBlock(s);
            case ElementKind.RECORD -> recordWrapperBlock(s);
            case ElementKind.ENUM -> enumWrapperBlock(s);
            default -> throw new SerdeException("Unexpected element kind: " + element.getKind());
        };
        source.registerBlock(definitionBlock(s));
        source.registerBlocks(List.of(
            singletonBlock(s),
            constructorBlock(s),
            staticBlock(s),
            wrapperBlock,
            fieldsBlock(s),
            builderBlock(s),
            colBlock(s),
            byNameBlock(s)
        ));
        for (FieldData fieldInfo : s.fieldDataList()) {
            source.registerBlocks(List.of(
                fieldHandleBlock(fieldInfo),
                fieldColBlock(s, fieldInfo),
                fieldTagMappingBlock(fieldInfo),
                fieldAssignBlock(s, fieldInfo),
                fieldGetBlock(s, fieldInfo),
                fieldSetBlock(s, fieldInfo)
            ));
        }
        source.registerBlock(closureBlock());
    }
}
