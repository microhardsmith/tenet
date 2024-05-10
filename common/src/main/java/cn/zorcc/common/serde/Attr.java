package cn.zorcc.common.serde;

import java.lang.annotation.*;

/**
 *   This annotation is only useful when used on class/record/enum fields with @Serde annotated to provide metadata information without using reflection
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface Attr {
    /**
     *   Attribute values, should be separated by ':', for example "json:str"
     */
    String[] values() default {};
}
