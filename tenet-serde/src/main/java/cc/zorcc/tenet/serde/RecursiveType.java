package cc.zorcc.tenet.serde;

import java.util.List;

/**
 *   Recursively represents the field type for runtime accessing control
 */
public record RecursiveType(
        Class<?> currentClass,
        List<RecursiveType> recursiveTypes
) {
    public RecursiveType {
        if(currentClass == null || recursiveTypes == null) {
            throw new SerdeException("Recursive type information cannot be null");
        }
    }
}
