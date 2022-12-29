package cn.zorcc.app.http.anno;

import java.lang.annotation.*;

/**
 * http post请求
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Post {
    String path() default "";
}
