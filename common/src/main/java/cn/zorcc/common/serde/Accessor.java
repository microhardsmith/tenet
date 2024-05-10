package cn.zorcc.common.serde;

import java.lang.invoke.MethodHandle;

/**
 *   This record is used in source code generation, all the variable names and functions shouldn't be renamed
 */
public record Accessor<T> (
        MethodHandle getter,
        MethodHandle setter,
        Column<T> column
) {
    public Accessor(MethodHandle getAccessor, Column<T> column) {
        this(getAccessor, null, column);
    }
}
