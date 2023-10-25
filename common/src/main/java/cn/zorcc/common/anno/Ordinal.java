package cn.zorcc.common.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

/**
 *   Annotation used for determining field sequence
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Ordinal {
    int sequence() default 0;
}
