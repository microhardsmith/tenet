package cn.zorcc.common.json;

import cn.zorcc.common.ResizableByteArray;
import cn.zorcc.common.Writer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class JsonWriter {
    private final Deque<JsonWriterObjectNode<?>> deque = new ArrayDeque<>();
    private final ResizableByteArray resizableByteArray = new ResizableByteArray();

    public <T> void serializeAsObject(Writer writer, T obj, Class<T> type) {
        JsonWriterObjectNode<T> node = new JsonWriterObjectNode<>(writer, obj, type);

    }

    public <T> void serializeAsList(Writer writer, List<T> list, Class<T> type) {

    }
}
