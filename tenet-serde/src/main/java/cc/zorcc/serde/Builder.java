package cc.zorcc.serde;

/**
 *   Builder is similar to Supplier<T> with more specified semantics
 */
@FunctionalInterface
public interface Builder<T> {
    /**
     *  Build target instance
     */
    T build();
}
