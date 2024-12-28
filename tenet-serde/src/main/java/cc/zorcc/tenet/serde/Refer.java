package cc.zorcc.tenet.serde;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 *   Refer represents a reference for the target serde object, the implementation should be generated as source code at compilation phase
 *   All the methods shouldn't be renamed, as they are corresponding to the source code generation
 */
public interface Refer<T> {

    @NonNull List<String> fields();

    /**
     *   Create an assigner for the serde target, assigner's value could be filled with Col<T>, then construct a real target
     *   If referring an Enum with no field, this function would return a builder that always return null.
     */
    @NonNull Builder<T> builder();

    /**
     *   Get target column, return null if it doesn't exist
     *   If referring an Enum with no field, this function would return null
     */
    @Nullable Col<T> col(@NonNull String colName);

    /**
     *   Cet an Enum instance by name, note that this method is only valid for Enum classes, other classes would always return null
     */
    @Nullable T byName(@NonNull String name);
}
