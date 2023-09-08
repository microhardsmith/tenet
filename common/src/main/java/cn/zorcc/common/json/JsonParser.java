package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Meta;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.Writer;
import cn.zorcc.common.anno.Format;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.exception.JsonParseException;
import cn.zorcc.common.util.ReflectUtil;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *   JsonParser implementation, primitive types and primitive arrays are not supported
 */
public final class JsonParser {
    private static final byte[] TRUE = "true".getBytes(StandardCharsets.UTF_8);
    private static final byte[] FALSE = "false".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INFINITY = "Infinity".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NAN = "NaN".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL = "null".getBytes(StandardCharsets.UTF_8);
    private JsonParser() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public static <T> void serializeObject(Writer writer, T obj) {
        Class<T> type = (Class<T>) obj.getClass();
        serializeObject(writer, obj, type);
    }

    public static <T> void serializeObject(Writer writer, T obj, Class<T> type) {
        if(type.isPrimitive() || type.isAnnotation() || type.isRecord() || type.isMemberClass()) {
            throw new FrameworkException(ExceptionType.JSON, "Unsupported type");
        }
        new JsonWriterObjectNode(writer, obj, type).startProcessing();
    }

    public static <T> void serializeCollection(Writer writer, Collection<T> collection) {
        serializeCollection(writer, collection, null);
    }

    public static <T> void serializeCollection(Writer writer, Collection<T> collection, Format format) {
        new JsonWriterCollectionNode(writer, collection.iterator(), format).startProcessing();
    }

    /**
     *   Deserialize a object from the data
     */
    public static <T> T deserializeAsObject(ReadBuffer readBuffer, Class<T> type) {
        // TODO
        return null;
    }

    /**
     * Deserialize a list from the data
     */
    public static <T> List<T> deserializeAsList(ReadBuffer readBuffer, Class<T> type) {
        // TODO
        return null;
    }

    /**
     *   Write null into the writer
     */
    public static void writeNull(Writer writer) {
        writer.writeBytes(NULL);
    }

    /**
     *   Write a byte value into the writer
     */
    public static void writeByte(Writer writer, Byte b, Format format) {
        if(format == null) {
            writer.writeByte(b);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writer, b);
            }else if(expectedType == Boolean.class) {
                writePrimitiveBoolean(writer, b > Constants.ZERO);
            }else {
                throw new JsonParseException(format, b);
            }
        }
    }
    /**
     *   Write a short value into the writer
     */
    public static void writeChar(Writer writer, Character c, Format format) {
        if(format == null) {
            writePrimitiveChar(writer, c);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writer, String.valueOf(c).getBytes(StandardCharsets.UTF_8));
            }else if(expectedType == Boolean.class) {
                writePrimitiveBoolean(writer, c > Constants.ZERO);
            }else {
                throw new JsonParseException(format, c);
            }
        }
    }

    public static void writePrimitiveChar(Writer writer, char c) {
        writeUtf8Bytes(writer, c);
    }

    /**
     *   Write a short value into the writer
     */
    public static void writeShort(Writer writer, Short s, Format format) {
        if(format == null) {
            writePrimitiveShort(writer ,s);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writer, String.valueOf(s).getBytes(StandardCharsets.UTF_8));
            }else if(expectedType == Boolean.class) {
                writePrimitiveBoolean(writer, s > Constants.ZERO);
            }else {
                throw new JsonParseException(format, s);
            }
        }
    }

    public static void writePrimitiveShort(Writer writer, short s) {
        while (s > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + s % 10));
            s /= 10;
        }
    }

    /**
     *   Write an Integer value into the writer
     */
    public static void writeInteger(Writer writer, Integer i, Format format) {
        if(format == null) {
            writePrimitiveInt(writer, i);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writer, String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }else if(expectedType == Boolean.class) {
                writePrimitiveBoolean(writer, i > Constants.ZERO);
            }else {
                throw new JsonParseException(format, i);
            }
        }
    }

    public static void writePrimitiveInt(Writer writer, int i) {
        while (i > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + i % 10));
            i /= 10;
        }
    }

    /**
     *   Write a Long value into the writer
     */
    public static void writeLong(Writer writer, Long l, Format format) {
        if(format == null) {
            writePrimitiveLong(writer, l);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writer, String.valueOf(l).getBytes(StandardCharsets.UTF_8));
            }else if(expectedType == Boolean.class) {
                writePrimitiveBoolean(writer, l > Constants.ZERO);
            }else {
                throw new JsonParseException(format, l);
            }
        }
    }

    public static void writePrimitiveLong(Writer writer, long l) {
        while (l > 0) {
            writer.writeByte((byte) (Constants.B_ZERO + l % 10));
            l /= 10;
        }
    }

    /**
     *   Write a String value into the writer
     */
    public static void writeString(Writer writer, String s) {
        writeStrBytes(writer, s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     *   Write String bytes into the writer
     */
    public static void writeStrBytes(Writer writer, byte... bytes) {
        writer.writeByte(Constants.QUOTE);
        writer.writeBytes(bytes);
        writer.writeByte(Constants.QUOTE);
    }

    /**
     *   Write a Float value into the writer
     */
    public static void writeFloat(Writer writer, Float f, Format format) {
        if(format == null) {
            writePrimitiveFloat(writer, f);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writer, String.valueOf(f).getBytes(StandardCharsets.UTF_8));
            }else if(expectedType == Boolean.class) {
                writePrimitiveBoolean(writer, f > Constants.ZERO);
            }else {
                throw new JsonParseException(format, f);
            }
        }
    }

    public static void writePrimitiveFloat(Writer writer, float f) {
        if(Float.isInfinite(f)) {
            writeStrBytes(writer, INFINITY);
        }else if(Float.isNaN(f)) {
            writeStrBytes(writer, NAN);
        }else {
            writer.writeBytes(String.valueOf(f).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a Double value into the writer
     */
    public static void writeDouble(Writer writer, Double d, Format format) {
        if(format == null) {
            writePrimitiveDouble(writer, d);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writer, String.valueOf(d).getBytes(StandardCharsets.UTF_8));
            }else if(expectedType == Boolean.class) {
                writePrimitiveBoolean(writer, d > Constants.ZERO);
            }else {
                throw new JsonParseException(format, d);
            }
        }
    }

    public static void writePrimitiveDouble(Writer writer, double d) {
        if(Double.isInfinite(d)) {
            writeStrBytes(writer, INFINITY);
        }else if(Double.isNaN(d)) {
            writeStrBytes(writer, NAN);
        }else {
            writer.writeBytes(String.valueOf(d).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a Boolean value into the writer
     */
    public static void writeBoolean(Writer writer, Boolean b, Format format) {
        if(format == null) {
            writePrimitiveBoolean(writer, b);
        }else {
            Class<?> expectedType = format.expectedType();
            if(expectedType == String.class) {
                writeStrBytes(writer, Boolean.TRUE.equals(b) ? TRUE : FALSE);
            }else {
                throw new JsonParseException(format, b);
            }
        }
    }

    public static void writePrimitiveBoolean(Writer writer, boolean b) {
        if(b) {
            writer.writeBytes(TRUE);
        }else {
            writer.writeBytes(FALSE);
        }
    }

    /**
     *   Write a Enum value into the writer
     */
    public static void writeEnum(Writer writer, Enum<?> e) {
        writeStrBytes(writer, e.name().getBytes(StandardCharsets.UTF_8));
    }

    /**
     *   Write a LocalDate value into the writer
     */
    public static void writeLocalDate(Writer writer, LocalDate ld, Format format) {
        if(format == null) {
            writeStrBytes(writer, ld.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writer, ld.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a LocalTime value into the writer
     */
    public static void writeLocalTime(Writer writer, LocalTime lt, Format format) {
        if(format == null) {
            writeStrBytes(writer, lt.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writer, lt.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a OffsetTime value into the writer
     */
    public static void writeOffsetTime(Writer writer, OffsetTime ot, Format format) {
        if(format == null) {
            writeStrBytes(writer, ot.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writer, ot.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a LocalDateTime value into the writer
     */
    public static void writeLocalDateTime(Writer writer, LocalDateTime ldt, Format format) {
        if(format == null) {
            writeStrBytes(writer, ldt.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writer, ldt.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a ZonedDateTime value into the writer
     */
    public static void writeZonedDateTime(Writer writer, ZonedDateTime zdt, Format format) {
        if(format == null) {
            writeStrBytes(writer, zdt.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writer, zdt.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a OffsetDateTime value into the writer
     */
    public static void writeOffsetDateTime(Writer writer, OffsetDateTime odt, Format format) {
        if(format == null) {
            writeStrBytes(writer, odt.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String pattern = format.expectedPattern();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            writeStrBytes(writer, odt.format(dateTimeFormatter).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     *   Write a primitive type array into the writer
     */
    public static void writePrimitiveArray(Writer writer, Object value, Format format) {
        switch (value) {
            case byte[] byteArray -> writeByteArray(writer, byteArray, format);
            case boolean[] booleanArray -> writeBooleanArray(writer, booleanArray, format);
            case char[] charArray -> writeCharArray(writer, charArray, format);
            case short[] shortArray -> writeShortArray(writer, shortArray, format);
            case int[] intArray -> writeIntArray(writer, intArray, format);
            case long[] longArray -> writeLongArray(writer, longArray, format);
            case float[] floatArray -> writeFloatArray(writer, floatArray, format);
            case double[] doubleArray -> writeDoubleArray(writer, doubleArray, format);
            default -> throw new JsonParseException(Constants.UNREACHED);
        }
    }

    private static void writeByteArray(Writer writer, byte[] arr, Format format) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (byte b : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writer.writeByte(b);
                notFirst = true;
            }
        }else {
            for (byte b : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writeByte(writer, b, format);
                notFirst = true;
            }
        }
        writer.writeByte(Constants.RSB);
    }

    private static void writeCharArray(Writer writer, char[] arr, Format format) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (char c : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writePrimitiveChar(writer, c);
                notFirst = true;
            }
        }else {
            for (char c : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writeChar(writer, c, format);
                notFirst = true;
            }
        }
        writer.writeByte(Constants.RSB);
    }

    private static void writeBooleanArray(Writer writer, boolean[] arr, Format format) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (boolean b : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writePrimitiveBoolean(writer, b);
                notFirst = true;
            }
        }else {
            for (boolean b : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writeBoolean(writer, b, format);
                notFirst = true;
            }
        }
        writer.writeByte(Constants.RSB);
    }

    private static void writeShortArray(Writer writer, short[] arr, Format format) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (short s : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                JsonParser.writePrimitiveShort(writer, s);
                notFirst = true;
            }
        }else {
            for (short s : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                JsonParser.writeShort(writer, s, format);
                notFirst = true;
            }
        }
        writer.writeByte(Constants.RSB);
    }

    private static void writeIntArray(Writer writer, int[] arr, Format format) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (int i : arr) {
                if(notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writePrimitiveInt(writer, i);
                notFirst = true;
            }
        }else {
            for (int i : arr) {
                if(notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writeInteger(writer, i, format);
                notFirst = true;
            }
        }
        writer.writeByte(Constants.RSB);
    }

    private static void writeLongArray(Writer writer, long[] arr, Format format) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (long l : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writePrimitiveLong(writer, l);
                notFirst = true;
            }
        }else {
            for (long l : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writeLong(writer, l, format);
                notFirst = true;
            }
        }
        writer.writeByte(Constants.RSB);
    }
    private static void writeFloatArray(Writer writer, float[] arr, Format format) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (float f : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writePrimitiveFloat(writer, f);
                notFirst = true;
            }
        }else {
            for (float f : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writeFloat(writer, f, format);
                notFirst = true;
            }
        }
        writer.writeByte(Constants.RSB);
    }

    private static void writeDoubleArray(Writer writer, double[] arr, Format format) {
        writer.writeByte(Constants.LSB);
        boolean notFirst = false;
        if(format == null) {
            for (double d : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writePrimitiveDouble(writer, d);
                notFirst = true;
            }
        }else {
            for (double d : arr) {
                if (notFirst) {
                    writer.writeByte(Constants.COMMA);
                }
                writeDouble(writer, d, format);
                notFirst = true;
            }
        }
        writer.writeByte(Constants.RSB);
    }

    /**
     *   Write unicode as utf-8 bytes into the writer
     */
    public static void writeUtf8Bytes(Writer writer, int data) {
        if(data < 0x80) {
            writer.writeByte((byte) data);
        }else if(data < 0x800) {
            writer.writeByte((byte) (0xC0 | (data >> 6)));
            writer.writeByte((byte) (0x80 | (data & 0x3F)));
        }else {
            writer.writeByte((byte) (0xE0 | (data >> 12)));
            writer.writeByte((byte) (0x80 | ((data >> 6) & 0x3F)));
            writer.writeByte((byte) (0x80 | (data & 0x3F)));
        }
    }


    /**
     *   Write a key into the writer
     */
    public static void writeKey(Writer writer, String key, boolean notFirst) {
        if(notFirst) {
            writer.writeByte(Constants.COMMA);
        }
        writer.writeByte(Constants.QUOTE);
        writer.writeBytes(key.getBytes(StandardCharsets.UTF_8));
        writer.writeByte(Constants.QUOTE);
        writer.writeByte(Constants.COLON);
        writer.writeByte(Constants.SPACE);
    }

    public static JsonNode writeValue(JsonNode current, Writer writer, Object value) {
        return writeValue(current, writer, value, null);
    }

    /**
     *   Write a value into the writer based on its type and format, return null if success, return next JsonNode if needs to create a new one
     */
    public static JsonNode writeValue(JsonNode current, Writer writer, Object value, Format format) {
        JsonNode result = null;
        switch (value) {
            case null -> JsonParser.writeNull(writer);
            case Byte b -> JsonParser.writeByte(writer, b, format);
            case Short s -> JsonParser.writeShort(writer, s, format);
            case Integer i -> JsonParser.writeInteger(writer, i, format);
            case Long l -> JsonParser.writeLong(writer, l, format);
            case String s -> JsonParser.writeString(writer, s);
            case Float f -> JsonParser.writeFloat(writer, f, format);
            case Double d -> JsonParser.writeDouble(writer, d, format);
            case Boolean b -> JsonParser.writeBoolean(writer, b, format);
            case Character c -> JsonParser.writeChar(writer, c, format);
            case Enum<?> e -> JsonParser.writeEnum(writer, e);
            case LocalDate ld -> JsonParser.writeLocalDate(writer, ld, format);
            case LocalTime lt -> JsonParser.writeLocalTime(writer, lt, format);
            case OffsetTime ot -> JsonParser.writeOffsetTime(writer, ot, format);
            case LocalDateTime ldt -> JsonParser.writeLocalDateTime(writer, ldt, format);
            case ZonedDateTime zdt -> JsonParser.writeZonedDateTime(writer, zdt, format);
            case OffsetDateTime odt -> JsonParser.writeOffsetDateTime(writer, odt, format);
            default -> result = writeObject(current, writer, value, format);
        }
        return result;
    }

    public static JsonNode writeObject(JsonNode current, Writer writer, Object value, Format format) {
        Class<?> type = value.getClass();
        if(value instanceof Collection<?> collection) {
            return current.toNext(new JsonWriterCollectionNode(writer, collection.iterator(), format));
        }else if(type.isArray()) {
            return writeArray(current, writer, value, type, format);
        }else if(value instanceof Map<?,?> map) {
            return current.toNext(new JsonWriterMapNode(writer, map));
        }else {
            return current.toNext(new JsonWriterObjectNode(writer, value, type));
        }
    }

    public static JsonNode writeArray(JsonNode current, Writer writer, Object value, Class<?> arrayType, Format format) {
        Class<?> componentType = arrayType.componentType();
        if(componentType.isPrimitive()) {
            JsonParser.writePrimitiveArray(writer, value, format);
            return null;
        }else {
            return current.toNext(new JsonWriterCollectionNode(writer, Arrays.stream((Object[]) value).iterator(), format));
        }
    }


    /**
     *   Determine whether or not should we skip a byte
     */
    public static boolean shouldSkip(byte b) {
        return b == Constants.SPACE || (b >= Constants.HT && b <= Constants.CR);
    }

    /**
     *   Convert unicode to utf-8 bytes
     */
    public static void readUnicode(ReadBuffer readBuffer, Writer writer) {
        if(!readBuffer.checkRemainingLength(4)) {
            throw new JsonParseException(readBuffer);
        }
        int data = Constants.ZERO;
        for(int i = 0; i < 4; i++) {
            data = data << 4 + readHexByte(readBuffer);
        }
        JsonParser.writeUtf8Bytes(writer, data);
    }


    private static int readHexByte(ReadBuffer readBuffer) {
        byte b = readBuffer.readByte();
        if(b >= Constants.B_ZERO && b <= Constants.B_NINE) {
            return b - Constants.B_ZERO;
        }else if(b >= Constants.B_a && b <= Constants.B_f) {
            return b - Constants.B_a + 10;
        }else if(b >= Constants.B_A && b <= Constants.B_F) {
            return b - Constants.B_A + 10;
        }else {
            throw new JsonParseException(readBuffer);
        }
    }

    /**
     *   Read and ignore empty bytes until counter expected byte, if other bytes occur, throw a exception
     */
    public static void readExpected(ReadBuffer readBuffer, byte expected) {
        while (readBuffer.readIndex() < readBuffer.size()) {
            byte b = readBuffer.readByte();
            if(!shouldSkip(b)) {
                if(b == expected){
                    return ;
                }else {
                    throw new JsonParseException(readBuffer);
                }
            }
        }
    }

    /**
     *   Read until the expected byte, convert the escape characters and write them into the writer
     */
    public static String readStringUntil(ReadBuffer readBuffer, Writer writer, byte expected) {
        while (readBuffer.readIndex() < readBuffer.size()) {
            byte b = readBuffer.readByte();
            if(b == Constants.ESCAPE) {
                readEscape(readBuffer, writer);
            }else if(b == expected) {
                return writer.asString();
            }else {
                writer.writeByte(b);
            }
        }
        throw new JsonParseException(readBuffer);
    }

    private static void readEscape(ReadBuffer readBuffer, Writer writer) {
        if(!readBuffer.checkRemainingLength(Constants.ONE)) {
            throw new JsonParseException(readBuffer);
        }
        switch (readBuffer.readByte()) {
            case Constants.ESCAPE -> writer.writeByte(Constants.ESCAPE);
            case Constants.QUOTE -> writer.writeByte(Constants.QUOTE);
            case Constants.SLASH -> writer.writeByte(Constants.SLASH);
            case (byte) 'b' -> writer.writeByte(Constants.BS);
            case (byte) 'f' -> writer.writeByte(Constants.FF);
            case (byte) 'n' -> writer.writeByte(Constants.LF);
            case (byte) 'r' -> writer.writeByte(Constants.CR);
            case (byte) 't' -> writer.writeByte(Constants.HT);
            case (byte) 'u' -> readUnicode(readBuffer, writer);
        }
    }

    /**
     *   Read until next meaningful byte, throw a exception if not found
     */
    public static byte readNextByte(ReadBuffer readBuffer) {
        while (readBuffer.readIndex() < readBuffer.size()) {
            byte b = readBuffer.readByte();
            if(!shouldSkip(b)) {
                return b;
            }
        }
        throw new JsonParseException(readBuffer);
    }

    /**
     *   Read until next byte matches expected, return the matched one, throw a exception if not found
     */
    public static byte readUntilMatch(ReadBuffer readBuffer, Writer writer, byte... expected) {
        while (readBuffer.readIndex() < readBuffer.size()) {
            byte b = readBuffer.readByte();
            for (byte b1 : expected) {
                if(b == b1) {
                    return b;
                }
            }
            writer.writeByte(b);
        }
        throw new JsonParseException(readBuffer);
    }


    /**
     *   If first byte is 't', then the value could be true
     */
    public static void readFollowingTrueValue(ReadBuffer readBuffer) {
        if (!readBuffer.checkRemainingLength(3)) {
            throw new JsonParseException(readBuffer);
        }
        byte b1 = readBuffer.readByte();
        byte b2 = readBuffer.readByte();
        byte b3 = readBuffer.readByte();
        if(b1 != (byte) 'r' || b2 != (byte) 'u' || b3 != (byte) 'e') {
            throw new JsonParseException(readBuffer);
        }
    }

    /**
     *   If first byte is 'f', then the value could be false
     */
    public static void readFollowingFalseValue(ReadBuffer readBuffer) {
        if (!readBuffer.checkRemainingLength(4)) {
            throw new JsonParseException(readBuffer);
        }
        byte b1 = readBuffer.readByte();
        byte b2 = readBuffer.readByte();
        byte b3 = readBuffer.readByte();
        byte b4 = readBuffer.readByte();
        if(b1 != (byte) 'a' || b2 != (byte) 'l' || b3 != (byte) 's' || b4 != (byte) 'e') {
            throw new JsonParseException(readBuffer);
        }
    }

    /**
     *   If first byte is 'n', then the value could be null
     */
    public static void readFollowingNullValue(ReadBuffer readBuffer) {
        if (!readBuffer.checkRemainingLength(3)) {
            throw new JsonParseException(readBuffer);
        }
        byte b1 = readBuffer.readByte();
        byte b2 = readBuffer.readByte();
        byte b3 = readBuffer.readByte();
        if(b1 != (byte) 'u' || b2 != (byte) 'l' || b3 != (byte) 'l') {
            throw new JsonParseException(readBuffer);
        }
    }

    public static Object convertJsonStringValue(Class<?> type, String str) {
        if(type == String.class) {
            return str;
        }else if(type == Byte.class) {
            return Byte.valueOf(str);
        }else if(type == Short.class) {
            return Short.valueOf(str);
        }else if(type == Integer.class) {
            return Integer.valueOf(str);
        }else if(type == Long.class) {
            return Long.valueOf(str);
        }else if(type == Float.class) {
            return Float.valueOf(str);
        }else if(type == Double.class) {
            return Double.valueOf(str);
        }else if(type == LocalDate.class) {
            return LocalDate.parse(str);
        }else if(type == LocalTime.class) {
            return LocalTime.parse(str);
        }else if(type == OffsetTime.class) {
            return OffsetTime.parse(str);
        }else if(type == LocalDateTime.class) {
            return LocalDateTime.parse(str);
        }else if(type == ZonedDateTime.class) {
            return ZonedDateTime.parse(str);
        }else if(type == OffsetDateTime.class) {
            return OffsetDateTime.parse(str);
        }else if(type == Character.class) {
            char[] charArray = str.toCharArray();
            if(charArray.length > Constants.ONE) {
                throw new JsonParseException("Unable to parse char value");
            }
            return charArray[Constants.ZERO];
        }else if(type.isEnum()) {
            return Meta.of(type).enumMap().get(str);
        }else {
            throw new JsonParseException("Unsupported string type");
        }
    }

    /**
     *   Set a json Number value into the target
     */
    public static Object convertJsonNumberValue(Class<?> type, String str) {
        if(type.isPrimitive()) {
            type = ReflectUtil.getWrapperClass(type);
        }
        if(type == Byte.class) {
            return Byte.valueOf(str);
        }else if(type == Short.class) {
            return Short.valueOf(str);
        }else if(type == Integer.class) {
            return Integer.valueOf(str);
        }else if(type == Long.class) {
            return Long.valueOf(str);
        }else if(type == Float.class) {
            return Float.valueOf(str);
        }else if(type == Double.class) {
            return Double.valueOf(str);
        }else {
            throw new JsonParseException("Unsupported number type");
        }
    }

    /**
     *   Set a json Collection value into the target
     */
    public static Object convertJsonCollectionValue(Class<?> type, List<Object> list) {
        if(type.isArray()) {
            return list.toArray();
        }else if(SortedSet.class.isAssignableFrom(type)) {
            return new TreeSet<>(list);
        }else if(Set.class.isAssignableFrom(type)) {
            return new HashSet<>(list);
        }else if(Queue.class.isAssignableFrom(type)) {
            return new ArrayDeque<>(list);
        }else if(List.class.isAssignableFrom(type)) {
            return list;
        }else {
            throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
        }
    }

    public static Object convertJsonMapValue(Class<?> type, Map<String, Object> map) {
        if(NavigableMap.class.isAssignableFrom(type)) {
            return new TreeMap<>(map);
        }else if(Map.class.isAssignableFrom(type)) {
            return map;
        }else {
            throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
        }
    }
}
