package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Format;
import cn.zorcc.common.exception.JsonParseException;
import cn.zorcc.common.structure.WriteBuffer;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public abstract class JsonWriterNode {
    /**
     *   This variable is used to limit the maximum value of the nodes in a doubly linked list, thereby detecting potential cases of circular references
     */
    private static final int CIRCULAR_REFERENCE_LIMITATION = 4 * Constants.KB;
    private static final byte[] TRUE = "true".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FALSE = "false".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFINITY = "Infinity".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NAN = "NaN".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL = "null".getBytes(StandardCharsets.UTF_8);
    private JsonWriterNode prev;
    private boolean notFirst = false;
    private int pos = 0;

    /**
     *   Unlink current JsonWriterNode with the prev node and return it
     */
    protected JsonWriterNode toPrev() {
        final JsonWriterNode p = this.prev;
        if(p == null) {
            return null;
        }
        this.prev = null;
        return p;
    }

    /**
     *   Link a new JsonWriterNode for the next and return it
     */
    protected JsonWriterNode toNext(JsonWriterNode n) {
        if(pos > CIRCULAR_REFERENCE_LIMITATION) {
            throw new JsonParseException("Possibly circular reference detected");
        }
        n.pos = this.pos + 1;
        n.prev = this;
        return n;
    }

    protected abstract JsonWriterNode trySerialize();
    
    public void serialize() {
        JsonWriterNode p = this;
        while (p != null) {
            p = p.trySerialize();
        }
    }
    
    /**
     *   Write null into the writeBuffer
     */
    private static void writeNull(WriteBuffer writeBuffer) {
        writeBuffer.writeBytes(NULL);
    }

    /**
     *   Write a byte value into the writeBuffer
     */
    private static void writeByte(WriteBuffer writeBuffer, Byte b, Format format) {
        if(format == null) {
            writeBuffer.writeByte(b);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writeBuffer, b);
            }else if(expectedType == boolean.class || expectedType == Boolean.class) {
                writePrimitiveBoolean(writeBuffer, b > 0);
            }else {
                throw new JsonParseException(format, b);
            }
        }
    }
    /**
     *   Write a short value into the writeBuffer
     */
    private static void writeChar(WriteBuffer writeBuffer, Character c, Format format) {
        if(format == null) {
            writePrimitiveChar(writeBuffer, c);
        }else {
            switch (format.expectedType()) {
                case Class<?> t when t == String.class -> writeStrBytes(writeBuffer, String.valueOf(c).getBytes(StandardCharsets.UTF_8));
                case Class<?> t when t == boolean.class || t == Boolean.class -> writePrimitiveBoolean(writeBuffer, c != '\u0000');
                default -> throw new JsonParseException(format, c);
            }
        }
    }

    private static void writePrimitiveChar(WriteBuffer writeBuffer, char c) {
        writeBuffer.writeByte(Constants.QUOTE);
        writeBuffer.writeUtf8Data(c);
        writeBuffer.writeByte(Constants.QUOTE);
    }

    /**
     *   Write a short value into the writeBuffer
     */
    private static void writeShort(WriteBuffer writeBuffer, Short s, Format format) {
        if(format == null) {
            writePrimitiveShort(writeBuffer ,s);
        }else {
            switch (format.expectedType()) {
                case Class<?> t when t == String.class -> writeStrBytes(writeBuffer, String.valueOf(s).getBytes(StandardCharsets.UTF_8));
                case Class<?> t when t == boolean.class || t == Boolean.class -> writePrimitiveBoolean(writeBuffer, s > (short) 0);
                default -> throw new JsonParseException(format, s);
            }
        }
    }

    private static void writePrimitiveShort(WriteBuffer writeBuffer, short s) {
        while (s > 0) {
            writeBuffer.writeByte((byte) (Constants.B_ZERO + s % 10));
            s /= 10;
        }
    }

    /**
     *   Write an Integer value into the writeBuffer
     */
    private static void writeInteger(WriteBuffer writeBuffer, Integer i, Format format) {
        if(format == null) {
            writePrimitiveInt(writeBuffer, i);
        }else {
            switch (format.expectedType()) {
                case Class<?> t when t == String.class -> writeStrBytes(writeBuffer, String.valueOf(i).getBytes(StandardCharsets.UTF_8));
                case Class<?> t when t == boolean.class || t == Boolean.class -> writePrimitiveBoolean(writeBuffer, i > 0);
                default -> throw new JsonParseException(format, i);
            }
        }
    }

    private static void writePrimitiveInt(WriteBuffer writeBuffer, int i) {
        writeBuffer.writeBytes(Integer.toString(i).getBytes(StandardCharsets.UTF_8));
    }

    /**
     *   Write a Long value into the writeBuffer
     */
    private static void writeLong(WriteBuffer writeBuffer, Long l, Format format) {
        if(format == null) {
            writePrimitiveLong(writeBuffer, l);
        }else {
            switch (format.expectedType()) {
                case Class<?> t when t == String.class -> writeStrBytes(writeBuffer, String.valueOf(l).getBytes(StandardCharsets.UTF_8));
                case Class<?> t when t == boolean.class || t == Boolean.class -> writePrimitiveBoolean(writeBuffer, l > 0L);
                default -> throw new JsonParseException(format, l);
            }
        }
    }

    private static void writePrimitiveLong(WriteBuffer writeBuffer, long l) {
        writeBuffer.writeBytes(Long.toString(l).getBytes(StandardCharsets.UTF_8));
    }

    /**
     *   Write a String value into the writeBuffer
     */
    private static void writeString(WriteBuffer writeBuffer, String s) {
        writeStrBytes(writeBuffer, s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     *   Write String bytes into the writeBuffer
     */
    private static void writeStrBytes(WriteBuffer writeBuffer, byte... bytes) {
        writeBuffer.writeByte(Constants.QUOTE);
        writeBuffer.writeBytes(bytes);
        writeBuffer.writeByte(Constants.QUOTE);
    }

    /**
     *   Write a Float value into the writeBuffer
     */
    private static void writeFloat(WriteBuffer writeBuffer, Float f, Format format) {
        if(format == null) {
            writePrimitiveFloat(writeBuffer, f);
        }else {
            switch (format.expectedType()) {
                case Class<?> t when t == String.class -> writeStrBytes(writeBuffer, String.valueOf(f).getBytes(StandardCharsets.UTF_8));
                case Class<?> t when t == boolean.class || t == Boolean.class -> writePrimitiveBoolean(writeBuffer, Math.signum(f) > 0.0f);
                default -> throw new JsonParseException(format, f);
            }
        }
    }

    private static void writePrimitiveFloat(WriteBuffer writeBuffer, float f) {
        if(Float.isInfinite(f)) {
            writeStrBytes(writeBuffer, INFINITY);
        }else if(Float.isNaN(f)) {
            writeStrBytes(writeBuffer, NAN);
        }else {
            writeBuffer.writeBytes(String.valueOf(f).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a Double value into the writeBuffer
     */
    private static void writeDouble(WriteBuffer writeBuffer, Double d, Format format) {
        if(format == null) {
            writePrimitiveDouble(writeBuffer, d);
        }else {
            switch (format.expectedType()) {
                case Class<?> t when t == String.class -> writeStrBytes(writeBuffer, String.valueOf(d).getBytes(StandardCharsets.UTF_8));
                case Class<?> t when t == boolean.class || t == Boolean.class -> writePrimitiveBoolean(writeBuffer, Math.signum(d) > 0.0d);
                default -> throw new JsonParseException(format, d);
            }
        }
    }

    private static void writePrimitiveDouble(WriteBuffer writeBuffer, double d) {
        if(Double.isInfinite(d)) {
            writeStrBytes(writeBuffer, INFINITY);
        }else if(Double.isNaN(d)) {
            writeStrBytes(writeBuffer, NAN);
        }else {
            writeBuffer.writeBytes(String.valueOf(d).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a Boolean value into the writeBuffer
     */
    private static void writeBoolean(WriteBuffer writeBuffer, Boolean b, Format format) {
        if(format == null) {
            writePrimitiveBoolean(writeBuffer, b);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writeBuffer, Boolean.TRUE.equals(b) ? TRUE : FALSE);
            }else {
                throw new JsonParseException(format, b);
            }
        }
    }

    private static void writePrimitiveBoolean(WriteBuffer writeBuffer, boolean b) {
        if(b) {
            writeBuffer.writeBytes(TRUE);
        }else {
            writeBuffer.writeBytes(FALSE);
        }
    }

    /**
     *   Write an Enum value into the writeBuffer
     */
    private static void writeEnum(WriteBuffer writeBuffer, Enum<?> e) {
        writeStrBytes(writeBuffer, e.name().getBytes(StandardCharsets.UTF_8));
    }

    /**
     *   Write a LocalDate value into the writeBuffer
     */
    private static void writeLocalDate(WriteBuffer writeBuffer, LocalDate ld, Format format) {
        if(format == null) {
            writeStrBytes(writeBuffer, ld.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writeBuffer, ld.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a LocalTime value into the writeBuffer
     */
    private static void writeLocalTime(WriteBuffer writeBuffer, LocalTime lt, Format format) {
        if(format == null) {
            writeStrBytes(writeBuffer, lt.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writeBuffer, lt.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a OffsetTime value into the writeBuffer
     */
    private static void writeOffsetTime(WriteBuffer writeBuffer, OffsetTime ot, Format format) {
        if(format == null) {
            writeStrBytes(writeBuffer, ot.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writeBuffer, ot.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a LocalDateTime value into the writeBuffer
     */
    private static void writeLocalDateTime(WriteBuffer writeBuffer, LocalDateTime ldt, Format format) {
        if(format == null) {
            writeStrBytes(writeBuffer, ldt.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writeBuffer, ldt.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a ZonedDateTime value into the writeBuffer
     */
    private static void writeZonedDateTime(WriteBuffer writeBuffer, ZonedDateTime zdt, Format format) {
        if(format == null) {
            writeStrBytes(writeBuffer, zdt.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writeBuffer, zdt.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a OffsetDateTime value into the writeBuffer
     */
    private static void writeOffsetDateTime(WriteBuffer writeBuffer, OffsetDateTime odt, Format format) {
        if(format == null) {
            writeStrBytes(writeBuffer, odt.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writeBuffer, odt.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a primitive type array into the writeBuffer
     */
    private static void writePrimitiveArray(WriteBuffer writeBuffer, Object value, Format format) {
        switch (value) {
            case byte[] byteArray -> writeByteArray(writeBuffer, byteArray, format);
            case boolean[] booleanArray -> writeBooleanArray(writeBuffer, booleanArray, format);
            case char[] charArray -> writeCharArray(writeBuffer, charArray, format);
            case short[] shortArray -> writeShortArray(writeBuffer, shortArray, format);
            case int[] intArray -> writeIntArray(writeBuffer, intArray, format);
            case long[] longArray -> writeLongArray(writeBuffer, longArray, format);
            case float[] floatArray -> writeFloatArray(writeBuffer, floatArray, format);
            case double[] doubleArray -> writeDoubleArray(writeBuffer, doubleArray, format);
            default -> throw new JsonParseException(Constants.UNREACHED);
        }
    }

    private static void writeByteArray(WriteBuffer writeBuffer, byte[] arr, Format format) {
        writeBuffer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (byte b : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeBuffer.writeByte(b);
                notFirst = true;
            }
        }else {
            for (byte b : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeByte(writeBuffer, b, format);
                notFirst = true;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
    }

    private static void writeCharArray(WriteBuffer writeBuffer, char[] arr, Format format) {
        writeBuffer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (char c : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writePrimitiveChar(writeBuffer, c);
                notFirst = true;
            }
        }else {
            for (char c : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeChar(writeBuffer, c, format);
                notFirst = true;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
    }

    private static void writeBooleanArray(WriteBuffer writeBuffer, boolean[] arr, Format format) {
        writeBuffer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (boolean b : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writePrimitiveBoolean(writeBuffer, b);
                notFirst = true;
            }
        }else {
            for (boolean b : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeBoolean(writeBuffer, b, format);
                notFirst = true;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
    }

    private static void writeShortArray(WriteBuffer writeBuffer, short[] arr, Format format) {
        writeBuffer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (short s : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writePrimitiveShort(writeBuffer, s);
                notFirst = true;
            }
        }else {
            for (short s : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeShort(writeBuffer, s, format);
                notFirst = true;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
    }

    private static void writeIntArray(WriteBuffer writeBuffer, int[] arr, Format format) {
        writeBuffer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (int i : arr) {
                if(notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writePrimitiveInt(writeBuffer, i);
                notFirst = true;
            }
        }else {
            for (int i : arr) {
                if(notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeInteger(writeBuffer, i, format);
                notFirst = true;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
    }

    private static void writeLongArray(WriteBuffer writeBuffer, long[] arr, Format format) {
        writeBuffer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (long l : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writePrimitiveLong(writeBuffer, l);
                notFirst = true;
            }
        }else {
            for (long l : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeLong(writeBuffer, l, format);
                notFirst = true;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
    }
    private static void writeFloatArray(WriteBuffer writeBuffer, float[] arr, Format format) {
        writeBuffer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (float f : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writePrimitiveFloat(writeBuffer, f);
                notFirst = true;
            }
        }else {
            for (float f : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeFloat(writeBuffer, f, format);
                notFirst = true;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
    }

    private static void writeDoubleArray(WriteBuffer writeBuffer, double[] arr, Format format) {
        writeBuffer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (double d : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writePrimitiveDouble(writeBuffer, d);
                notFirst = true;
            }
        }else {
            for (double d : arr) {
                if (notFirst) {
                    writeBuffer.writeByte(Constants.COMMA);
                }
                writeDouble(writeBuffer, d, format);
                notFirst = true;
            }
        }
        writeBuffer.writeByte(Constants.RSB);
    }

    protected void writeSep(WriteBuffer writeBuffer) {
        if(notFirst) {
            writeBuffer.writeByte(Constants.COMMA);
        }
        notFirst = true;
    }

    protected void writeKey(WriteBuffer writeBuffer, String key) {
        writeBuffer.writeByte(Constants.QUOTE);
        writeBuffer.writeBytes(key.getBytes(StandardCharsets.UTF_8));
        writeBuffer.writeByte(Constants.QUOTE);
        writeBuffer.writeByte(Constants.COLON);
        writeBuffer.writeByte(Constants.SPACE);
    }

    protected JsonWriterNode writeValue(WriteBuffer writeBuffer, Object value) {
        return writeValue(writeBuffer, value, null);
    }

    /**
     *   Write a value into the writeBuffer based on its type and format, return null if success, return next JsonWriterNode if it needs to create a new one
     */
    protected JsonWriterNode writeValue(WriteBuffer writeBuffer, Object value, Format format) {
        JsonWriterNode result = null;
        switch (value) {
            case null -> writeNull(writeBuffer);
            case Byte b -> writeByte(writeBuffer, b, format);
            case Short s -> writeShort(writeBuffer, s, format);
            case Integer i -> writeInteger(writeBuffer, i, format);
            case Long l -> writeLong(writeBuffer, l, format);
            case String s -> writeString(writeBuffer, s);
            case Float f -> writeFloat(writeBuffer, f, format);
            case Double d -> writeDouble(writeBuffer, d, format);
            case Boolean b -> writeBoolean(writeBuffer, b, format);
            case Character c -> writeChar(writeBuffer, c, format);
            case Enum<?> e -> writeEnum(writeBuffer, e);
            case LocalDate ld -> writeLocalDate(writeBuffer, ld, format);
            case LocalTime lt -> writeLocalTime(writeBuffer, lt, format);
            case OffsetTime ot -> writeOffsetTime(writeBuffer, ot, format);
            case LocalDateTime ldt -> writeLocalDateTime(writeBuffer, ldt, format);
            case ZonedDateTime zdt -> writeZonedDateTime(writeBuffer, zdt, format);
            case OffsetDateTime odt -> writeOffsetDateTime(writeBuffer, odt, format);
            default -> result = writeObject(writeBuffer, value, format);
        }
        return result;
    }

    protected JsonWriterNode writeObject(WriteBuffer writeBuffer, Object value, Format format) {
        Class<?> type = value.getClass();
        if(value instanceof Collection<?> collection) {
            return toNext(new JsonWriterCollectionNode(writeBuffer, collection.iterator(), format));
        }else if(type.isArray()) {
            return writeArray(writeBuffer, value, type, format);
        }else if(value instanceof Map<?,?> map) {
            return toNext(new JsonWriterMapNode(writeBuffer, map));
        }else if(type.isRecord()) {
            return toNext(new JsonWriterRecordNode(writeBuffer, value, type));
        }else {
            return toNext(new JsonWriterObjectNode(writeBuffer, value, type));
        }
    }

    protected JsonWriterNode writeArray(WriteBuffer writeBuffer, Object value, Class<?> arrayType, Format format) {
        Class<?> componentType = arrayType.componentType();
        if(componentType.isPrimitive()) {
            writePrimitiveArray(writeBuffer, value, format);
            return null;
        }else {
            return toNext(new JsonWriterCollectionNode(writeBuffer, Arrays.stream((Object[]) value).iterator(), format));
        }
    }
}
