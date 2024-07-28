package cn.zorcc.common.serde;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *   We could review each field in a class as a column, where we could modify it as we want
 */
public record Column<T> (
        String name,
        RecursiveType type,
        Function<String, String> tagMapping,
        BiConsumer<Supplier<T>, Object> assigner,
        Function<T, Object> fetcher
) {
    /**
     *   EMPTY_TAG would be used in source-code generation, never rename or remove it
     */
    @SuppressWarnings("unused")
    public static final Function<String, String> EMPTY_TAG = _ -> null;

    public String tag(String key) {
        return tagMapping.apply(key);
    }

    public void assign(Supplier<T> supplier, Object value) {
        assigner.accept(supplier, value);
    }

    public Object fetch(T value) {
        return fetcher.apply(value);
    }
}
