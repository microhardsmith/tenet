package cc.zorcc.tenet.core.toml;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *   Toml configuration table representation
 *   Note that TomlTable should only be represented as a serialization mechanism, that helps tenet-framework itself parsing all kinds of toml-based configuration files
 *   It's not intended to be used as a production-ready toml serialization/deserialization framework
 */
public final class TomlTable {
    /**
     *   Global empty tomlTable
     */
    private static final TomlTable EMPTY = new TomlTable(List.of(), List.of());

    /**
     * Returns an empty TomlTable instance.
     *
     * @return an empty TomlTable
     */
    public static TomlTable empty() {
        return EMPTY;
    }

    private final Map<String, TomlValue> nodeMap;
    private final Map<String, TomlTable> tableMap;

    /**
     * Constructs a TomlTable with the given global nodes and table nodes.
     *
     * @param globalNodes a list of global nodes (key-value pairs)
     * @param tableNodes a list of table nodes (key-value pairs)
     */
    public TomlTable(List<Map.Entry<String, TomlValue>> globalNodes, List<Map.Entry<String, TomlTable>> tableNodes) {
        this.nodeMap = globalNodes.stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        this.tableMap = tableNodes.stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Retrieves a TomlNode by its name.
     *
     * @param nodeName the name of the node
     * @return the corresponding TomlNode, or an empty node if not found
     */
    public TomlValue retrieveNode(String nodeName) {
        TomlValue n = nodeMap.get(nodeName);
        if(n == null) {
            return TomlValue.empty();
        }
        return n;
    }

    /**
     * Retrieves a TomlTable by its name.
     *
     * @param tableName the name of the table
     * @return the corresponding TomlTable, or an empty table if not found
     */
    public TomlTable retrieveTable(String tableName) {
        TomlTable t = tableMap.get(tableName);
        if(t == null) {
            return empty();
        }
        return t;
    }

}
