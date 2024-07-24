package cc.zorcc.tenet.core.json;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Map;

public sealed interface JsonNode {
    record JsonKeyNode(MemorySegment data) implements JsonNode {

    }

    record JsonNumberNode(MemorySegment data) implements JsonNode {

    }

    record JsonStrNode(MemorySegment data) implements JsonNode {

    }

    enum JsonConstantNode implements JsonNode {
        True,
        False,
        Null
    }

    record JsonArrNode(List<JsonNode> nodes) implements JsonNode {

    }

    record JsonObjNode(List<Map.Entry<JsonKeyNode, JsonNode>> nodes) implements JsonNode {

    }
}
