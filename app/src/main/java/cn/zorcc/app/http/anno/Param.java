package cn.zorcc.app.http.anno;

import java.lang.annotation.*;

/**
 * 解析http占位符参数
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {
    /**
     *  参数值
     */
    String value() default "";
    /**
     *  是否必须,在缺失时会抛出ServiceException异常
     */
    boolean required() default false;
}
