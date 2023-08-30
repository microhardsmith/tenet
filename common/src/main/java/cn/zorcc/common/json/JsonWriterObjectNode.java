package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Gt;
import cn.zorcc.common.GtInfo;
import cn.zorcc.common.Writer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class JsonWriterObjectNode<T> extends JsonNode {
    private static final byte[] TRUE = "true".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FALSE = "false".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFINITY = "Infinity".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NAN = "NaN".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL = "null".getBytes(StandardCharsets.UTF_8);

    private final Writer writer;
    private final Gt<T> gt;
    private final List<GtInfo> gtInfoList;
    private final T obj;
    private int index = -1;
    private boolean notFirst = false;

    public JsonWriterObjectNode(Writer writer, T obj, Class<T> type) {
        this.writer = writer;
        this.gt = Gt.of(type);
        this.gtInfoList = gt.gtInfoList();
        this.obj = obj;
    }

    @Override
    protected JsonNode process() {
        if(index < Constants.ZERO) {
            writer.writeByte(Constants.LCB);
            index = Constants.ZERO;
            return this;
        }else if(index == gtInfoList.size()) {
            return popNode();
        }else {
            return processInternal();
        }
    }

    private JsonNode processInternal() {
        for(int i = index; i < gtInfoList.size(); i++) {
            GtInfo gtInfo = gtInfoList.get(i);
            Object value = gtInfo.getter().apply(obj);
            if(value != null) {
                writeKey(writer, gtInfo.fieldName());
                notFirst = true;
                JsonNode appended = writeValue(writer, value);
                if(appended != null) {
                    return appended;
                }
            }
        }
        return null;
    }

    private JsonNode popNode() {
        writer.writeByte(Constants.RCB);
        JsonNode prevNode = this.prev;
        this.prev = null;
        prevNode.next = null;
        return prevNode;
    }

    public void serialize(Writer writer) {
        for(int i = index; i < metaInfoList.size(); i++) {
            writeKey();
        }
//        Gt<T> gt = Gt.of(objClass);
//        List<MetaInfo> metaInfoList = gt.metaInfoList();
//        try(ResizableByteArray resizableByteArray = new ResizableByteArray()) {
//            resizableByteArray.writeByte(Constants.LCB);
//            boolean notFirst = false;
//            for (MetaInfo metaInfo : metaInfoList) {
//                Object field = metaInfo.getter().apply(obj);
//                if(field != null) {
//                    writeKey(resizableByteArray, metaInfo.fieldName(), notFirst);
//                    writeValue(resizableByteArray, field);
//                    notFirst = true;
//                }
//            }
//            resizableByteArray.writeByte(Constants.RCB);
//            writer.writeBytes(resizableByteArray.rawArray(), Constants.ZERO, resizableByteArray.writeIndex());
//        }
    }

    private void writeKey(Writer writer, String key) {
        if(notFirst) {
            writer.writeByte(Constants.COMMA);
        }
        writer.writeByte(Constants.QUOTE);
        writer.writeBytes(key.getBytes(StandardCharsets.UTF_8));
        writer.writeByte(Constants.QUOTE);
        writer.writeByte(Constants.COLON);
        writer.writeByte(Constants.SPACE);
    }

    @SuppressWarnings("unchecked")
    private <E> JsonNode writeValue(Writer writer, E value) {
        switch (value) {
            case null -> writer.writeBytes(NULL);
            case Byte b -> writer.writeByte(b);
            case Short s -> writeShort(writer, s);
            case Integer i -> writeInteger(writer, i);
            case Long l -> writeLong(writer, l);
            case String s -> writeString(writer, s);
            case Float f -> writeFloat(writer, f);
            case Double d -> writeDouble(writer, d);
            case Boolean b -> writeBoolean(writer, b);
            case Collection<?> c -> writeCollection(writer, c);
            case Map<?, ?> map -> writeMap(writer, map);
            case Object[] array -> writeArray(writer, array);
            case Enum<?> e -> writeEnum(writer, e);
            case LocalDate date -> writeLocalDate(writer, date);
            case LocalDateTime time -> writeLocalDateTime(writer, time);
            default -> {
                Class<E> type = (Class<E>) value.getClass();
                return writeObject(writer, value, type);
            }
        }
        return null;
    }

    private <E> JsonNode writeObject(Writer writer, E value, Class<E> type) {
        JsonSerializer<E> serializer = JsonParser.getJsonSerializer(type);
        if(serializer == null) {
            if(Collection.class.isAssignableFrom(type) || type.isArray()) {
                // TODO 添加数组节点
                return null;
            }else {
                JsonWriterObjectNode<E> nextNode = new JsonWriterObjectNode<>(writer, value, type);
                this.next = nextNode;
                nextNode.prev = this;
                return nextNode;
            }
        }else {
            serializer.serialize(writer, value);
            return null;
        }
    }

    /**
     *   Write a short value into the writer
     */
    private void writeShort(Writer writer, Short s) {
        short ss = s;
        while (ss > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + ss % 10));
            ss /= 10;
        }
    }

    /**
     *   Write an Integer value into the writer
     */
    private static void writeInteger(Writer writer, Integer i) {
        int ii = i;
        while (ii > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + ii % 10));
            ii /= 10;
        }
    }

    /**
     *   Write a Long value into the writer
     */
    private static void writeLong(Writer writer, Long l) {
        long ll = l;
        while (ll > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + ll % 10));
            ll /= 10;
        }
    }

    /**
     *   Write a String value into the writer
     */
    private static void writeString(Writer writer, String s) {
        writeStrBytes(writer, s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     *   Write String bytes into the writer
     */
    private static void writeStrBytes(Writer writer, byte[] bytes) {
        writer.writeByte(Constants.QUOTE);
        writer.writeBytes(bytes);
        writer.writeByte(Constants.QUOTE);
    }

    /**
     *   Write a Float value into the writer
     */
    private static void writeFloat(Writer writer, Float f) {
        if(f.isInfinite()) {
            writeStrBytes(writer, INFINITY);
        }else if(f.isNaN()) {
            writeStrBytes(writer, NAN);
        }else {
            writer.writeBytes(f.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writeDouble(Writer writer, Double d) {
        if(d.isInfinite()) {
            writeStrBytes(writer, INFINITY);
        }else if(d.isNaN()) {
            writeStrBytes(writer, NAN);
        }else {
            writer.writeBytes(d.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writeBoolean(Writer writer, Boolean b) {
        if(b.equals(Boolean.TRUE)) {
            writer.writeBytes(TRUE);
        }else if(b.equals(Boolean.FALSE)) {
            writer.writeBytes(FALSE);
        }else {
            throw new FrameworkException(ExceptionType.JSON, Constants.UNREACHED);
        }
    }

    private static void writeCollection(Writer writer, Collection<?> collection) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        for (Object o : collection) {
            if(notFirst) {
                writer.writeByte(Constants.COMMA);
            }
            if(o == null) {
                writer.writeBytes(NULL);
            }else {
                writeValue(writer, o);
            }
            notFirst = true;
        }
        writer.writeByte(Constants.RSB);
    }

    private static void writeMap(Writer writer, Map<?,?> map) {
        writer.writeByte(Constants.LCB);
        boolean notFirst = false;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if(notFirst) {
                writer.writeByte(Constants.COMMA);
            }
            writeKey(writer, entry.getKey().toString(), notFirst);
            writeValue(writer, entry.getValue());
            notFirst = true;
        }
        writer.writeByte(Constants.RCB);
    }

    private static void writeArray(Writer writer, Object[] array) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        for (Object o : array) {
            if(notFirst) {
                writer.writeByte(Constants.COMMA);
            }
            writeValue(writer, o);
            notFirst = true;
        }
        writer.writeByte(Constants.RSB);
    }

    private static void writeEnum(Writer writer, Enum<?> e) {
        writeStrBytes(writer, e.name().getBytes(StandardCharsets.UTF_8));
    }


    private static void writeLocalDate(Writer writer, LocalDate date) {
        writeStrBytes(writer, date.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeLocalDateTime(Writer writer, LocalDateTime time) {
        writeStrBytes(writer, time.toString().getBytes(StandardCharsets.UTF_8));
    }
}
