package cc.zorcc.tenet.serde;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface Transform {
    String value();
}
