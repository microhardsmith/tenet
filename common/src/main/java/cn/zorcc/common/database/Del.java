package cn.zorcc.common.database;

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
    int ordinal() default 0;
}
