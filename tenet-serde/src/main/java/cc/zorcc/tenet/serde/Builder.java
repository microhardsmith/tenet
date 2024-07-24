package cc.zorcc.tenet.serde;

/**
 *   Builder is similar to Supplier<T> with more specified semantics used in source code generation
 */
@FunctionalInterface
public interface Builder<T> {
    /**
     *  Build target instance
     */
    T build();
}
