package cn.zorcc.common.serde;

import java.lang.annotation.*;

/**
 *   Mark a class as serialization and deserialization compatible
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface Serde {

}
