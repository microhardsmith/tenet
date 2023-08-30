package cn.zorcc.common.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

/**
 *   Annotation for determining serialization format
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
     *   Expected serialization string pattern
     */
    String expectedPattern() default Constants.EMPTY_STRING;
}
