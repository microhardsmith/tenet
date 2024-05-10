package cn.zorcc.common.serde;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.List;

/**
 *   Representing class with generic type information recursively, this class shouldn't be renamed as it's used in source code generation
 */
public record RecurClass(
        Class<?> currentClass,
        List<RecurClass> recurClasses
) {
    public RecurClass {
        if(currentClass == null || recurClasses == null) {
            throw new FrameworkException(ExceptionType.ANNO, Constants.UNREACHED);
        }
    }

}
