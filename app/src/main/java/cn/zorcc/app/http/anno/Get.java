package cn.zorcc.app.http.anno;


import java.lang.annotation.*;

/**
 * http get请求
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Get {
    String path() default "";
}
