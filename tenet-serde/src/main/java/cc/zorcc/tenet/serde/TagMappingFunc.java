package cc.zorcc.tenet.serde;

@FunctionalInterface
public interface TagMappingFunc {

    TagMappingFunc NULLIFY = str -> null;

    /**
     * Maps a given tag key to its tag value
     */
    String map(String tagName);
}
