package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

/**
 *   JsonNode abstraction class, JsonNodes are represented as double linked list.
 *   When all the nodes were removed, the serialization or deserialization are completed
 */
public abstract class JsonNode {
    private JsonNode prevNode;

    private JsonNode nextNode;

    /**
     *   Unlink current JsonNode with the prev node and return it
     */
    protected JsonNode toPrev() {
        if(this.nextNode != null) {
            throw new FrameworkException(ExceptionType.JSON, Constants.UNREACHED);
        }
        final JsonNode prevNode = this.prevNode;
        if(prevNode == null) {
            return null;
        }
        this.prevNode = null;
        prevNode.nextNode = null;
        return prevNode;
    }

    /**
     *   Link a new JsonNode for the next and return it
     */
    protected JsonNode toNext(final JsonNode nextNode) {
        if(this.nextNode != null) {
            throw new FrameworkException(ExceptionType.JSON, Constants.UNREACHED);
        }
        this.nextNode = nextNode;
        nextNode.prevNode = this;
        return nextNode;
    }

    /**
     *   Processing current JsonNode
     */
    public abstract JsonNode process();

    /**
     *   Constantly call process() until serialization or deserialization are completed
     *   This mechanism helps to avoid the problem of stack overflow that may occur during recursive parsing, and provides better error message prompts.
     */
    public void startProcessing() {
        JsonNode current = process();
        while (current != null) {
            current = current.process();
        }
    }
}
