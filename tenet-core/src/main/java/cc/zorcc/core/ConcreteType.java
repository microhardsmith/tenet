package cc.zorcc.core;

import java.util.List;

/**
 *   ConcreteType representing class with generic type information recursively, this class shouldn't be renamed as it's used in source code generation
 */
public record ConcreteType(
        Class<?> type,
        List<ConcreteType> genericTypes
) {
    public ConcreteType {
        if(type == null || genericTypes == null) {
            throw new FrameworkException(ExceptionType.MISUSE, Constants.UNREACHED);
        }
    }
}
