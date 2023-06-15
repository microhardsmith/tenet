package cn.zorcc.orm.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Col {

    /**
     *   Column value
     */
    String value();
    /**
     *   Column ordinal
     */
    int ordinal() default Constants.ZERO;
}
