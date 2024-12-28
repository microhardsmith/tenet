package cc.zorcc.tenet.serde;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface FetcherFunc<T> {
    /**
     *   Fetching value from the target instance
     */
    @Nullable Object fetch(@NonNull T t);
}
