package cn.zorcc.app.http.anno;

import java.lang.annotation.*;

/**
 * http 路径参数
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {
    /**
     *  是否必须，在缺失时会抛出ServiceException异常
     */
    boolean required() default false;
}
