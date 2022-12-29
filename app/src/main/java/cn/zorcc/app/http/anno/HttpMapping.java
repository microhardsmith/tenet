package cn.zorcc.app.http.anno;

import cn.zorcc.common.Constants;

import java.lang.annotation.*;

/**
 * http请求响应类
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface HttpMapping {
    /**
     * http url前缀
     */
    String prefix() default Constants.EMPTY_STRING;
}
