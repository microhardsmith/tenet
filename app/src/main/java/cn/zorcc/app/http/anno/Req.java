package cn.zorcc.app.http.anno;

import java.lang.annotation.*;

/**
 * 标识该参数为HttpReq
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Req {

}
