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

/**
 *   JsonReaderNode represents a linked-node reader pattern for JSON specified deserialization
 */
public abstract class JsonReaderNode {

    /**
     *   Prev linkedNode
     */
    private JsonReaderNode prev;

    /**
     *   Determines if it's the first linkedNode in the linkedList
     */
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

    /**
     *   Assign the value to current node
     */
    protected abstract void setJsonObject(Object value);

    /**
     *   Get the deserialization result
     */
    protected abstract Object getJsonObject();


    /**
     *   Try deserialization from current Node, return the next Node linked to it, or null if deserialization is complete
     */
    protected abstract JsonReaderNode tryDeserialize();

    /**
     *   Start looping and deserializing from current JsonReaderNode
     */
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
                readBuffer.setReadIndex(readBuffer.currentIndex() - 1L);
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
        while (readBuffer.currentIndex() < readBuffer.size()) {
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
        while (readBuffer.currentIndex() < readBuffer.size()) {
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
        while (readBuffer.currentIndex() < readBuffer.size()) {
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
        while (readBuffer.currentIndex() < readBuffer.size()) {
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
        return switch (type) {
            case Class<?> t when t == String.class -> str;
            case Class<?> t when t == Byte.class -> Byte.valueOf(str);
            case Class<?> t when t == Short.class -> Short.valueOf(str);
            case Class<?> t when t == Integer.class -> Integer.valueOf(str);
            case Class<?> t when t == Long.class -> Long.valueOf(str);
            case Class<?> t when t == Float.class -> Float.valueOf(str);
            case Class<?> t when t == Double.class -> Double.valueOf(str);
            case Class<?> t when t == Character.class -> stringToCharacter(str);
            case Class<?> t when t == LocalDate.class -> LocalDate.parse(str);
            case Class<?> t when t == LocalTime.class -> LocalTime.parse(str);
            case Class<?> t when t == OffsetTime.class -> OffsetTime.parse(str);
            case Class<?> t when t == LocalDateTime.class -> LocalDateTime.parse(str);
            case Class<?> t when t == ZonedDateTime.class -> ZonedDateTime.parse(str);
            case Class<?> t when t == OffsetDateTime.class -> OffsetDateTime.parse(str);
            case Class<?> t when t.isEnum() -> JsonParser.getMeta(type).enumMap().get(str);
            default -> throw new JsonParseException("Unsupported string type : %s".formatted(type.getName()));
        };
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
        return switch (type) {
            case Class<?> t when t == Byte.class -> Byte.valueOf(str);
            case Class<?> t when t == Short.class -> Short.valueOf(str);
            case Class<?> t when t == Integer.class -> Integer.valueOf(str);
            case Class<?> t when t == Long.class -> Long.valueOf(str);
            case Class<?> t when t == Float.class -> Float.valueOf(str);
            case Class<?> t when t == Double.class -> Double.valueOf(str);
            default -> throw new JsonParseException("Unsupported number type : %s".formatted(type.getName()));
        };
    }

    /**
     *   Set a json Collection value into the target
     */
    public static Object convertJsonCollectionValue(Class<?> type, List<Object> list) {
        return switch (type) {
            case Class<?> t when t.isArray() -> list.toArray();
            case Class<?> t when SortedSet.class.isAssignableFrom(t) -> new TreeSet<>(list);
            case Class<?> t when Set.class.isAssignableFrom(t) -> new HashSet<>(list);
            case Class<?> t when Queue.class.isAssignableFrom(t) -> new ArrayDeque<>(list);
            case Class<?> t when List.class.isAssignableFrom(t) -> list;
            default -> throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
        };
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
