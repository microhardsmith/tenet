package cn.zorcc.common.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

/**
 *   Annotation for determining serialization format
 *   When the @Format annotation is applied to an array or Collection, it will be applied to all elements.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Format {
    /**
     *   Expected serialization type
     */
    Class<?> expectedType() default Void.class;

    /**
     *   Used for specify List or Map element type for deserialization
     */
    Class<?> elementType() default Void.class;

    /**
     *   Expected serialization string pattern
     */
    String expectedPattern() default Constants.EMPTY_STRING;
}
