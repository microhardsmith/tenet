package cc.zorcc.tenet.serde;

import org.jspecify.annotations.Nullable;

/**
 *   Builder is similar to Supplier<T> with more specified semantics used in source code generation
 */
@FunctionalInterface
public interface Builder<T> {
    /**
     *  Build target instance, this function should be guaranteed to be only invoke once, or a IllegalCallerException would be thrown
     *  The result could be null, for example, no enum could be matched for current serialization result
     */
    @Nullable T build();
}
