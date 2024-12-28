package cc.zorcc.tenet.serdeproc;

import cc.zorcc.tenet.serde.Attr;
import cc.zorcc.tenet.serde.SerdeException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *   Annotation processor convention helper utilities
 */
public final class SerdeConvention {
    /**
     *   Element helper shouldn't be initialized
     */
    private SerdeConvention() {
        throw new UnsupportedOperationException();
    }

    /// check if current element is legal for @Serde
    public static void check(@NonNull Element e, @NonNull Types types, @NonNull Elements elements) {
        switch (e.getKind()) {
            case CLASS -> checkSerdeClass(e, types, elements);
            case RECORD -> checkSerdeRecord(e);
            case ENUM -> checkSerdeEnum(e);
            default -> throw new SerdeException("Unsupported serde element kind: %s".formatted(e));
        }
    }

    /// Check if current element is a legal class for @Serde
    private static void checkSerdeClass(@NonNull Element e, @NonNull Types types, @NonNull Elements elements) {
        // check if this is a public final class
        if (!e.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.FINAL))) {
           throw new SerdeException("Class annotated with @Serde must be public final : %s".formatted(e));
        }
        // check if this is a top-level class
        if(isInnerClass(e)) {
            throw new SerdeException("Serde class can't be inner class: %s".formatted(e));
        }
        // check if this class has any member
        if (e.getEnclosedElements().isEmpty()) {
            throw new SerdeException("No enclosed element found: %s".formatted(e));
        }
        // check if this class has any super class
        if (e instanceof TypeElement typeElement) {
            TypeMirror superclassType = typeElement.getSuperclass();
            TypeMirror objectClassType = elements.getTypeElement(Object.class.getCanonicalName()).asType();
            if(!types.isSameType(superclassType, objectClassType)) {
                throw new SerdeException("Class annotated with @Serde can't have any super class: %s".formatted(e));
            }
        } else {
            throw new SerdeException("Not a typeElement: %s".formatted(e));
        }
        // check if there is a public no-arg constructor
        for (Element enclosedElement : e.getEnclosedElements()) {
            if(enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR)
                    && enclosedElement instanceof ExecutableElement executableElement
                    && executableElement.getParameters().isEmpty()
                    && executableElement.getModifiers().contains(Modifier.PUBLIC)) {
                return ;
            }
        }
       throw new SerdeException("Serde class must have a no-arg constructor: %s".formatted(e));
    }

    /// check if current element is a legal record for @Serde
    private static void checkSerdeRecord(@NonNull Element e) {
        // check if this is a public record
        if(!e.getModifiers().contains(Modifier.PUBLIC)) {
            throw new SerdeException("Record must be public: %s".formatted(e));
        }
        // check if this record has any member
        if (e.getEnclosedElements().isEmpty()) {
            throw new SerdeException("No enclosed element found: %s".formatted(e));
        }
        // check if this is a top-level record
        if(isInnerClass(e)) {
            throw new SerdeException("Serde record can't be inner class: %s".formatted(e));
        }
    }

    /// check if current element is a legal enum for @Serde
    private static void checkSerdeEnum(@NonNull Element e) {
        // check if this is a public enum
        if(!e.getModifiers().contains(Modifier.PUBLIC)) {
            throw new SerdeException("Record must be public: %s".formatted(e));
        }
        // check if this is a top-level enum
        if(isInnerClass(e)) {
            throw new SerdeException("Serde enum can't be inner class: %s".formatted(e));
        }
        // check if this enum has any enum constant
        if (e.getEnclosedElements().stream().noneMatch(element -> element.getKind().equals(ElementKind.ENUM_CONSTANT))) {
            throw new SerdeException("No enum constant element found: %s".formatted(e));
        }
        // check if all the fields are final
        for (Element item : e.getEnclosedElements()) {
            if(item.getKind() == ElementKind.FIELD && item instanceof VariableElement v) {
                if(!v.getModifiers().contains(Modifier.FINAL)) {
                    throw new SerdeException("All the variables in a enum must be final: %s".formatted(item));
                }
            }
        }
    }

    /**
     *   Check if current element is a top-level class-file
     */
    private static boolean isInnerClass(Element e) {
        Element enclosingElement = e.getEnclosingElement();
        return enclosingElement != null && enclosingElement.getKind() != ElementKind.PACKAGE;
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
    public static @NonNull List<Map.Entry<String, String>> attrList(@Nullable Attr attr) {
        if(attr != null) {
            return Arrays.stream(attr.value()).map(SerdeConvention::toEntry).toList();
        } else {
            return List.of();
        }
    }

    public static Map.Entry<String, String> toEntry(@Nullable String item) {
        if(item == null || item.isBlank()) {
            throw new SerdeException("Item is empty");
        }
        String[] s = item.split(":", 2);
        if(s.length != 2) {
            throw new SerdeException("Wrong attr format for %s".formatted(item));
        }
        String key = s[0];
        if(key == null || key.isBlank()) {
            throw new SerdeException("Missing required attribute key for %s".formatted(item));
        }
        String value = s[1];
        if(value == null || value.isBlank()) {
            throw new SerdeException("Missing required attribute value for %s".formatted(item));
        }
        return Map.entry(key, value);
    }

    public static @NonNull String packageName(@NonNull Element e, @NonNull Elements elements) {
        return elements.getPackageOf(e).getQualifiedName().toString();
    }

    public static @NonNull String elementName(@NonNull Element e) {
        return e.toString();
    }

    public static @NonNull String typeName(@NonNull Element e) {
        return e.asType().toString();
    }

    public static @NonNull String generateClassName(@NonNull String targetClassName) {
        if(targetClassName.contains("$") || targetClassName.contains("_")) {
            throw new SerdeException("Illegal targetClassName identifiers found for %s, element can't contain '$' or '_' ".formatted(targetClassName));
        }
        int index = targetClassName.lastIndexOf(".");
        return "_%s$$Serde".formatted(index == -1 ? targetClassName : targetClassName.substring(index + 1));
    }

    public static GenericData genericData(@NonNull Element e) {
        if (e.asType() instanceof DeclaredType declaredType) {
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (typeArguments.isEmpty()) {
                return GenericData.EMPTY;
            } else {
                List<String> fullGenericTypes = new ArrayList<>();
                List<String> genericTypes = new ArrayList<>();
                List<String> holderGenericTypes = new ArrayList<>();
                for (TypeMirror typeArgument : typeArguments) {
                    if(typeArgument instanceof TypeVariable typeVariable) {
                        String simpleGenericType = typeVariable.toString();
                        genericTypes.add(simpleGenericType);
                        String upperBoundType = typeVariable.getUpperBound().toString();
                        fullGenericTypes.add(upperBoundType.equals(Object.class.getCanonicalName()) ? simpleGenericType : "%s extends %s".formatted(simpleGenericType, upperBoundType));
                        holderGenericTypes.add("?");
                    } else {
                        throw new SerdeException("Not a type variable: %s".formatted(typeArgument));
                    }
                }
                return new GenericData(
                        genericTypes,
                        "<%s>".formatted(String.join(", ", fullGenericTypes)),
                        "<%s>".formatted(String.join(", ", genericTypes)),
                        "<%s>".formatted(String.join(", ", holderGenericTypes)), "<>");
            }
        } else {
            throw new SerdeException("Not a declared type: %s".formatted(e));
        }
    }

    public static String generateHandleName(FieldData f) {
        return "_%sHandle".formatted(f.fieldName());
    }

    public static String generateColName(FieldData f) {
        return "_%sCol".formatted(f.fieldName());
    }

    public static String generateTagMappingName(FieldData f) {
        return "_%sTagMapping".formatted(f.fieldName());
    }

    public static String generateAssignName(FieldData f) {
        return "_%sAssign".formatted(f.fieldName());
    }

    public static String generateSetName(FieldData f) {
        return "_%sSet".formatted(f.fieldName());
    }

    public static String generateGetName(FieldData f) {
        return "_%sGet".formatted(f.fieldName());
    }

    public static String generateFormExpression(FieldData f, SerdeData s) {
        List<String> allowedGenerics = s.genericData().genericTypes();
        byte[] exps = typeName(f.field()).getBytes(StandardCharsets.UTF_8);
        List<String> r = new ArrayList<>();
        int index = 0;
        for(int i = 0; i < exps.length; ++i) {
            byte b = exps[i];
            switch (b) {
                case '<' -> {
                    String str = new String(exps, index, i - index, StandardCharsets.UTF_8).trim();
                    r.add(allowedGenerics.contains(str) ? "java.lang.Object.class" : "%s.class".formatted(str));
                    r.add("Form.L()");
                    index = i + 1;
                }
                case ',' -> {
                    String str = new String(exps, index, i - index, StandardCharsets.UTF_8).trim();
                    r.add(allowedGenerics.contains(str) ? "java.lang.Object.class" : "%s.class".formatted(str));
                    index = i + 1;
                }
                case '>' -> {
                    String str = new String(exps, index, i - index, StandardCharsets.UTF_8).trim();
                    r.add(allowedGenerics.contains(str) ? "java.lang.Object.class" : "%s.class".formatted(str));
                    r.add("Form.R()");
                    index = i + 1;
                }
                default -> {
                    // continue the loop
                }
            }
        }
        return "Form.of(%s)".formatted(String.join(", ", r));
    }

    public static String generateMethodReference(SerdeData s, String methodName) {
        return "%s::%s".formatted(s.generatedClassName(), methodName);
    }

    public static String generateColExpression(FieldData f, SerdeData s) {
        return "new Col<>(\"%s\", %s, %s, %s, %s)"
                .formatted(
                        f.fieldName(),
                        generateFormExpression(f, s),
                        f.attrList().isEmpty() ? "TagMappingFunc.NULLIFY" : generateMethodReference(s, generateTagMappingName(f)),
                        generateMethodReference(s, generateAssignName(f)),
                        generateMethodReference(s, generateGetName(f))
                    );
    }

    public static String generateAssignExpression(FieldData f, SerdeData s) {
        return switch (s.element().getKind()) {
            case RECORD, ENUM -> "wrapper.%s = (%s) value;".formatted(f.fieldName(), f.fieldType());
            case CLASS -> "%s(wrapper.instance(), (%s) value);".formatted(generateSetName(f), f.fieldType());
            default -> throw new SerdeException("Unsupported serde element kind: %s".formatted(s.element()));
        };
    }

    public static String generateEnumConstantExpression(FieldData f, SerdeData s) {
        return "%s.%s".formatted(s.targetClassName(), f.fieldName());
    }

    public static @NonNull String typeToClass(@NonNull String type) {
        int firstIndex = type.indexOf('<');
        return firstIndex == -1 ? type : type.substring(0, firstIndex);
    }
}
