package cc.zorcc.tenet.core.toml;

/**
 *   TomlNode represents a data structure that are used in TomlTable
 */
public interface TomlValue {
    /**
     *   Global empty node
     */
    TomlValue EMPTY = new TomlEmptyValue();

    /**
     *   Retrieve the global empty tomlNode
     */
    static TomlValue empty() {
        return EMPTY;
    }

    /**
     *   TomlEmptyNode, used as null equivalent tomlNode
     */
    record TomlEmptyValue() implements TomlValue {

    }
}
