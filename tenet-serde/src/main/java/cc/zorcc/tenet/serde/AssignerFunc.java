package cc.zorcc.tenet.serde;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface AssignerFunc<T> {
    /**
     *   Assigning value to the builder instance
     */
    void assign(@NonNull Builder<T> builder, @Nullable Object value);
}
