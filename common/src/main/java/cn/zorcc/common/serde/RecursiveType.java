package cn.zorcc.common.serde;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.List;

/**
 *   Representing class with generic type information recursively, this class shouldn't be renamed as it's used in source code generation
 */
public record RecursiveType(
        Class<?> currentClass,
        List<RecursiveType> recursiveTypes
) {
    public RecursiveType {
        if(currentClass == null || recursiveTypes == null) {
            throw new FrameworkException(ExceptionType.ANNO, Constants.UNREACHED);
        }
    }

}
