package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.exception.JsonParseException;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.common.util.ReflectUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.*;
import java.util.*;

public abstract class JsonReaderNode {
    private JsonReaderNode prev;
    private boolean notFirst = false;

    /**
     *   Unlink current JsonReaderNode with the prev node and return it
     */
    protected JsonReaderNode toPrev() {
        final JsonReaderNode p = this.prev;
        if(p == null) {
            return null;
        }
        p.setJsonObject(getJsonObject());
        this.prev = null;
        return p;
    }

    /**
     *   Link a new JsonReaderNode for the next and return it
     */
    protected JsonReaderNode toNext(JsonReaderNode n) {
        n.prev = this;
        return n;
    }

    protected abstract void setJsonObject(Object value);

    protected abstract Object getJsonObject();

    protected abstract JsonReaderNode tryDeserialize();

    public void deserialize() {
        JsonReaderNode p = this;
        while (p != null) {
            p = p.tryDeserialize();
        }
    }

    /**
     *   Check next separator, return true if it's a endByte
     */
    protected boolean checkSeparator(ReadBuffer readBuffer, byte endByte) {
        byte sep = readNextByte(readBuffer);
        if(notFirst) {
            if(sep == endByte) {
                return true;
            }else if(sep == Constants.COMMA) {
                return false;
            }else {
                throw new JsonParseException(readBuffer);
            }
        }else {
            notFirst = true;
            if(sep == endByte) {
                return true;
            }else {
                readBuffer.setReadIndex(readBuffer.readIndex() - 1);
                return false;
            }
        }
    }

    /**
     *   Determine whether should we parse a meaningful byte
     */
    public static boolean shouldParse(byte b) {
        return b != Constants.SPACE && (b < Constants.HT || b > Constants.CR);
    }

    /**
     *   Convert unicode to utf-8 bytes
     */
    public static void readUnicode(ReadBuffer readBuffer, WriteBuffer writeBuffer) {
        if(readBuffer.available() < 4) {
            throw new JsonParseException(readBuffer);
        }
        int data = 0;
        for(int i = 0; i < 4; i++) {
            data = data << 4 + readHexByte(readBuffer);
        }
        writeBuffer.writeUtf8Data(data);
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
     *   Read and ignore empty bytes until counter expected byte, if other bytes occur, throw an exception
     */
    protected static void readExpected(ReadBuffer readBuffer, byte expected) {
        while (readBuffer.readIndex() < readBuffer.size()) {
            byte b = readBuffer.readByte();
            if(shouldParse(b)) {
                if(b == expected){
                    return ;
                }else {
                    throw new JsonParseException(readBuffer);
                }
            }
        }
    }

    /**
     *   Read until the expected byte, convert the escape characters and write them into the writeBuffer
     */
    public static String readStringUntil(ReadBuffer readBuffer, WriteBuffer writeBuffer, byte expected) {
        while (readBuffer.readIndex() < readBuffer.size()) {
            byte b = readBuffer.readByte();
            if(b == Constants.ESCAPE) {
                readEscape(readBuffer, writeBuffer);
            }else if(b == expected) {
                return writeBuffer.toString();
            }else {
                writeBuffer.writeByte(b);
            }
        }
        throw new JsonParseException(readBuffer);
    }

    private static void readEscape(ReadBuffer readBuffer, WriteBuffer writeBuffer) {
        if(readBuffer.available() < 1) {
            throw new JsonParseException(readBuffer);
        }
        switch (readBuffer.readByte()) {
            case Constants.ESCAPE -> writeBuffer.writeByte(Constants.ESCAPE);
            case Constants.QUOTE -> writeBuffer.writeByte(Constants.QUOTE);
            case Constants.SLASH -> writeBuffer.writeByte(Constants.SLASH);
            case (byte) 'b' -> writeBuffer.writeByte(Constants.BS);
            case (byte) 'f' -> writeBuffer.writeByte(Constants.FF);
            case (byte) 'n' -> writeBuffer.writeByte(Constants.LF);
            case (byte) 'r' -> writeBuffer.writeByte(Constants.CR);
            case (byte) 't' -> writeBuffer.writeByte(Constants.HT);
            case (byte) 'u' -> readUnicode(readBuffer, writeBuffer);
        }
    }

    /**
     *   Read until next meaningful byte, throw an exception if not found
     */
    public static byte readNextByte(ReadBuffer readBuffer) {
        while (readBuffer.readIndex() < readBuffer.size()) {
            byte b = readBuffer.readByte();
            if(shouldParse(b)) {
                return b;
            }
        }
        throw new JsonParseException(readBuffer);
    }

    /**
     *   Read until next byte matches expected, return the matched one, throw an exception if not found
     */
    public static byte readUntilMatch(ReadBuffer readBuffer, WriteBuffer writeBuffer, byte... expected) {
        while (readBuffer.readIndex() < readBuffer.size()) {
            byte b = readBuffer.readByte();
            for (byte b1 : expected) {
                if(b == b1) {
                    return b;
                }
            }
            writeBuffer.writeByte(b);
        }
        throw new JsonParseException(readBuffer);
    }


    /**
     *   If first byte being t, then the value could be true
     */
    public static void readFollowingTrueValue(ReadBuffer readBuffer) {
        if (readBuffer.available() < 3) {
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
     *   If first byte being f, then the value could be false
     */
    public static void readFollowingFalseValue(ReadBuffer readBuffer) {
        if (readBuffer.available() < 4) {
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
     *   If first byte is ['n'], then the value could be null
     */
    public static void readFollowingNullValue(ReadBuffer readBuffer) {
        if (readBuffer.available() < 3) {
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
        if(type.isPrimitive()) {
            type = ReflectUtil.getWrapperClass(type);
        }
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
        }else if(type == Character.class) {
            return stringToCharacter(str);
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
        }else if(type.isEnum()) {
            return JsonParser.getMeta(type).enumMap().get(str);
        }else {
            throw new JsonParseException("Unsupported string type : %s".formatted(type.getName()));
        }
    }

    private static Character stringToCharacter(String str) {
        char[] charArray = str.toCharArray();
        if(charArray.length != 1) {
            throw new JsonParseException("String can't be converted to Character : %s".formatted(str));
        }
        return charArray[0];
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
            throw new JsonParseException("Unsupported number type : %s".formatted(type.getName()));
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

    protected JsonReaderNode newObjectOrRecordNode(ReadBuffer readBuffer, Class<?> type, Type genericType) {
        if(Map.class.isAssignableFrom(type) && genericType instanceof ParameterizedType pt) {
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            if(actualTypeArguments[0] instanceof Class<?> keyClass && keyClass == String.class) {
                return toNext(new JsonReaderMapNode(readBuffer, type, actualTypeArguments[1]));
            }else {
                throw new JsonParseException(Constants.JSON_KEY_TYPE_ERR);
            }
        }else if(type.isRecord()){
            return toNext(new JsonReaderRecordNode(readBuffer, type));
        }else {
            return toNext(new JsonReaderObjectNode(readBuffer, type));
        }
    }

    protected JsonReaderNode newObjectOrRecordNode(ReadBuffer readBuffer, Type elementType) {
        if(elementType instanceof Class<?> c) {
            if(c.isRecord()) {
                return toNext(new JsonReaderRecordNode(readBuffer, c));
            }else {
                return toNext(new JsonReaderObjectNode(readBuffer, c));
            }
        }else if(elementType instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?> c && Map.class.isAssignableFrom(c)) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if(actualTypeArguments[0] instanceof Class<?> keyClass && keyClass == String.class) {
                return toNext(new JsonReaderMapNode(readBuffer, c, actualTypeArguments[1]));
            }else {
                throw new JsonParseException(Constants.JSON_KEY_TYPE_ERR);
            }
        }else {
            throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
        }
    }

    protected JsonReaderNode newCollectionNode(ReadBuffer readBuffer, Class<?> type, Type genericType) {
        if(type.isArray()) {
            return toNext(new JsonReaderCollectionNode(readBuffer, type, type.componentType()));
        }else if(Collection.class.isAssignableFrom(type) && genericType instanceof ParameterizedType pt ) {
            return toNext(new JsonReaderCollectionNode(readBuffer, type, pt.getActualTypeArguments()[0]));
        }else {
            throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
        }
    }

    protected JsonReaderNode newCollectionNode(ReadBuffer readBuffer, Type elementType) {
        if(elementType instanceof Class<?> c && c.isArray()) {
            return toNext(new JsonReaderCollectionNode(readBuffer, c, c.componentType()));
        }else if(elementType instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c && Collection.class.isAssignableFrom(c)) {
            return toNext(new JsonReaderCollectionNode(readBuffer, c, pt.getActualTypeArguments()[0]));
        }else {
            throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
        }
    }
}
