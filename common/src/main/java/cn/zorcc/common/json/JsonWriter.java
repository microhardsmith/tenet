package cn.zorcc.common.json;

import cn.zorcc.common.ResizableByteArray;
import cn.zorcc.common.Writer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class JsonWriter {
    private final Deque<JsonWriterNode<?>> deque = new ArrayDeque<>();
    private final ResizableByteArray resizableByteArray = new ResizableByteArray();

    public <T> void serializeAsObject(Writer writer, Object obj, Class<T> objClass) {
        JsonWriterNode<T> node = new JsonWriterNode<>(objClass);

    }

    public void serializeAsList(Writer writer, List<Object> list) {

    }
}
