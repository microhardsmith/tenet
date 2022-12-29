package cn.zorcc.app.http.anno;

import java.lang.annotation.*;

/**
 * http put请求
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Put {
    String path() default "";
}
