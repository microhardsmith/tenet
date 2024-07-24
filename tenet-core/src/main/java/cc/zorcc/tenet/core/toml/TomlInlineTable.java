package cc.zorcc.tenet.core.toml;

import java.util.Map;

/**
 *   Represents a toml inline table
 */
public record TomlInlineTable(
        Map<String, TomlValue> nodeMap
) implements TomlValue {

}
