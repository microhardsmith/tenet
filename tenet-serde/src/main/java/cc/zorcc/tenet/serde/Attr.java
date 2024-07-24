package cc.zorcc.tenet.serde;

import java.lang.annotation.*;

/**
 * This annotation is used to provide metadata information for fields in classes, records, or enums.
 * It is typically used in conjunction with the {@link Serde} annotation to facilitate the serialization
 * and deserialization processes without the need for reflection.
 *
 * <p>
 * This annotation is only applicable to fields of classes, records, or enums that are also annotated
 * with {@link Serde}. It is not intended for use with other types of elements.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 * {@literal @Serde}
 * public class Example {
 *
 *     {@literal @Attr}(values = {"json:str", "xml:int"})
 *     private String exampleField;
 *
 *     // Other fields and methods...
 * }
 * </pre>
 * </p>
 *
 * <p>
 * The {@code values} element is used to define a list of attributes, which should be formatted
 * as strings separated by a colon (:). Each string specifies an attribute name and its corresponding
 * value. For example, a value of {@code "json:str"} indicates that the field should be treated as
 * a string in JSON format.
 * </p>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface Attr {
    /**
     * Attribute values that provide metadata for the field. Each value should be formatted as
     * "attributeName:attributeValue". For example, a value of "json:str" indicates that the field
     * is a string in JSON format.
     *
     * <p>
     * This array can contain multiple attribute definitions, which will be used to generate metadata
     * during the compile-time processing.
     * </p>
     *
     * <p>
     * Example:
     * <pre>
     * {@literal @Attr}(value = {"json:str", "toml:int"})
     * </pre>
     * </p>
     *
     * @return An array of attribute definitions, each formatted as "attributeName:attributeValue".
     */
    String[] value() default {};
}
