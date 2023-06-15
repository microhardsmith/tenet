package cn.zorcc.orm.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    String name() default Constants.EMPTY_STRING;
}
