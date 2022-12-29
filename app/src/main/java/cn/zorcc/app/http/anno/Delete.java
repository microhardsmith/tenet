package cn.zorcc.app.http.anno;

import java.lang.annotation.*;

/**
 * http delete请求
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Delete {
    String path() default "";
}
