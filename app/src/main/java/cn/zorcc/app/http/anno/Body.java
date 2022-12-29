package cn.zorcc.app.http.anno;

import java.lang.annotation.*;

/**
 * http body参数
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Body {

}
