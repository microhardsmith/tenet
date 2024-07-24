package cc.zorcc.tenet.serde;

import java.lang.annotation.*;

/**
 * This annotation marks a class, record, or enum as compatible with serialization and deserialization.
 * When a type is annotated with {@code Serde}, it indicates that instances of the annotated type
 * can be serialized to and deserialized from a specific format, such as JSON, TOML, etc.
 *
 * <p>
 * The {@code Serde} annotation is intended to be used in conjunction with a serialization/deserialization
 * framework that utilizes {@code SerdeContext}. The {@code SerdeContext} provides the necessary context
 * and configuration for performing serialization and deserialization operations.
 * </p>
 *
 * <p>
 * By annotating a type with {@code Serde}, you enable the framework to recognize and process the
 * annotated type according to the metadata and rules defined by additional annotations such as {@link Attr}.
 * This allows for a flexible and powerful mechanism to handle data transformation without relying on
 * reflection at runtime.
 * </p>
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface Serde {

}
