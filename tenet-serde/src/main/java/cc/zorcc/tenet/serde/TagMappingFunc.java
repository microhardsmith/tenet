package cc.zorcc.tenet.serde;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface TagMappingFunc {

    /**
     *   Global empty mapping function
     */
    @SuppressWarnings("unused")
    TagMappingFunc NULLIFY = _ -> null;

    /**
     * Maps a given tag key to its tag value
     */
    @Nullable String map(@NonNull String tagName);
}
