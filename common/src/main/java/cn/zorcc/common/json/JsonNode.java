package cn.zorcc.common.json;

/**
 *   JsonNode abstraction class, JsonNodes are represented as double linked list.
 *   When all the nodes were removed, the serialization or deserialization are completed
 */
public abstract class JsonNode {
    protected JsonNode prev;
    protected JsonNode next;

    /**
     *   Processing current JsonNode
     */
    protected abstract JsonNode process();

    /**
     *   Constantly call loop() until serialization or deserialization are completed
     */
    public void start() {
        JsonNode current = process();
        while (current != null) {
            current = current.process();
        }
    }
}
