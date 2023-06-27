package cn.zorcc.common.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Del {

    /**
     *   Column value
     */
    String value();
    /**
     *   Column ordinal
     */
    int ordinal() default Constants.ZERO;
}
