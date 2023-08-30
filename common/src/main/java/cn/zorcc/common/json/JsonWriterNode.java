package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Gt;
import cn.zorcc.common.MetaInfo;
import cn.zorcc.common.Writer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class JsonWriterNode<T> {
    private final Gt<T> gt;
    private final List<MetaInfo> metaInfoList;
    private int index = Constants.ZERO;
    private boolean notFirst = false;

    public JsonWriterNode(Class<T> clazz) {
        this.gt = Gt.of(clazz);
        this.metaInfoList = gt.metaInfoList();
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
//                    writeKey(resizableByteArray, metaInfo.name(), notFirst);
//                    writeValue(resizableByteArray, field);
//                    notFirst = true;
//                }
//            }
//            resizableByteArray.writeByte(Constants.RCB);
//            writer.writeBytes(resizableByteArray.rawArray(), Constants.ZERO, resizableByteArray.writeIndex());
//        }
    }

    private static void writeKey(Writer writer, String key, boolean notFirst) {
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
    private static <T> void writeValue(Writer writer, T value) {
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
                JsonSerializer<T> serializer = (JsonSerializer<T>) getJsonSerializer(value.getClass());
                if(serializer == null) {
                    serializeAsObject(writer, value);
                }else {
                    serializer.serialize(writer, value);
                }
            }
        }
    }

    private static void writeShort(Writer writer, Short s) {
        short ss = s;
        while (ss > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + ss % 10));
            ss /= 10;
        }
    }

    private static void writeInteger(Writer writer, Integer i) {
        int ii = i;
        while (ii > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + ii % 10));
            ii /= 10;
        }
    }

    private static void writeLong(Writer writer, Long l) {
        long ll = l;
        while (ll > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + ll % 10));
            ll /= 10;
        }
    }

    private static void writeString(Writer writer, String s) {
        writeStrBytes(writer, s.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeStrBytes(Writer writer, byte[] bytes) {
        writer.writeByte(Constants.QUOTE);
        writer.writeBytes(bytes);
        writer.writeByte(Constants.QUOTE);
    }

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
