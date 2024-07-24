package cc.zorcc.tenet.core.toml;

/**
 *   TomlStrNode represents a string value in the toml document
 */
public record TomlStrValue(
        byte[] value
) implements TomlValue {

}
