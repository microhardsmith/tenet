package cn.zorcc.common.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    /**
     *   The table name in the database, should be a string with underline separated
     */
    String name() default Constants.EMPTY_STRING;
}
