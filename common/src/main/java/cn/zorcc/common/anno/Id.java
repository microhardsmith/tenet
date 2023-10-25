package cn.zorcc.common.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Id {
    /**
     *   Whether the table Id column was auto generated by the database
     */
    boolean auto() default true;

    /**
     *   Column value
     */
    String value() default "id";

    /**
     *   Column ordinal
     */
    int ordinal() default 0;
}
