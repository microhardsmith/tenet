package cn.zorcc.common.serde;

import java.util.function.Supplier;

/**
 *   Handle represents a reference for the target serde object, the implementation should be generated by source code at compilation phase
 *   All the methods shouldn't be renamed
 */
public interface Handle<T> {

    /**
     *   Create an assigner for the serde target, assigner's value could be filled with Column<T>, then construct a real target
     */
    Supplier<T> createAssigner();

    /**
     *   Get target column, return null if it doesn't exist
     */
    Column<T> col(String columnName);

    /**
     *   Cet an Enum instance by name, note that this method is only valid for Enum classes, other classes would throw an UnsupportedOperationException
     */
    T byName(String name);
}
