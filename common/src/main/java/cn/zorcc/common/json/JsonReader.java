package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.MetaInfo;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.ResizableByteArray;
import cn.zorcc.common.exception.JsonParseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class JsonReader<T> {
    private static final int SMALL_SIZE = 64;
    private static final int MIDDLE_SIZE = 256;
    private static final int BIG_SIZE = 1024;
    private final ReadBuffer rb;
    private final ResizableByteArray wb;
    private final Deque<JsonNode<?>> jsonNodes = new ArrayDeque<>();

    public JsonReader(ReadBuffer readBuffer, Class<T> clazz) {
        this.rb = readBuffer;
        this.wb = new ResizableByteArray(estimateSize(readBuffer));
        byte b = readBuffer.readByte();
        if(b == Constants.LCB) {
            jsonNodes.addLast(new JsonNode<>(this, clazz, false));
        }else if(b == Constants.LSB) {
            jsonNodes.addLast(new JsonNode<>(this, clazz, true));
        }else {
            throw new JsonParseException(readBuffer);
        }
    }

    private static int estimateSize(ReadBuffer readBuffer) {
        long size = readBuffer.size();
        if(size < Constants.KB) {
            return SMALL_SIZE;
        }else if(size < Constants.MB) {
            return MIDDLE_SIZE;
        }else {
            return BIG_SIZE;
        }
    }

    /**
     *   Deserialize current readBuffer as a object value
     */
    public T deserializeAsObject() {
        JsonNode<?> current = jsonNodes.getLast();
        if(current.getState() != JsonReaderState.Object) {
            throw new JsonParseException("Target readBuffer is not an array");
        }
        for( ; ; ) {
            switch ()
        }
    }

    /**
     *   Deserialize current readBuffer as a list
     */
    public List<T> deserializeAsList() {
        JsonNode<?> current = jsonNodes.getLast();
        if(current.getState() != JsonReaderState.Array) {
            throw new JsonParseException("Target readBuffer is not an array");
        }
    }

    /**
     *   Initial decoding start, could be decoding an object or an array
     */
    private void decodeInitial(byte b, ReadBuffer readBuffer) {

    }

    void decodeObject(byte b, ReadBuffer readBuffer) {
        if(b == Constants.QUOTE) {
            state = State.KEY;
        }else {
            throw new JsonParseException(readBuffer);
        }
    }

    void decodeKey(byte b) {
        if(b == Constants.ESCAPE) {
            state = State.ESCAPE;
        }else if(b == Constants.QUOTE){
            key = new String(wb.rawArray(), Constants.ZERO, wb.writeIndex(), StandardCharsets.UTF_8);
            wb.reset();
            state = State.KEY_END;
        }else {
            wb.writeByte(b);
        }
    }

    void decodeKeyEnd(byte b, ReadBuffer readBuffer) {
        if(b == Constants.COLON) {
            state = State.COLON;
        }else {
            throw new JsonParseException(readBuffer);
        }
    }

    void decodeColon(byte b, ReadBuffer readBuffer) {
        switch (b) {
            case (byte) 't' -> decodeTrueValue(readBuffer);
            case (byte) 'f' -> decodeFalseValue(readBuffer);
            case (byte) 'n' -> decodeNullValue(readBuffer);
            case Constants.QUOTE -> state = State.STRING;
        }
    }

    /**
     *   If first byte is 't', then the value could be true
     */
    void decodeTrueValue(ReadBuffer readBuffer) {
        byte b1 = readBuffer.readByte();
        byte b2 = readBuffer.readByte();
        byte b3 = readBuffer.readByte();
        if(b1 == (byte) 'r' && b2 == (byte) 'u' && b3 == (byte) 'e') {
            assignBoolean(Boolean.TRUE, readBuffer);
        }
    }

    /**
     *   If first byte is 'f', then the value could be false
     */
    void decodeFalseValue(ReadBuffer readBuffer) {
        byte b1 = readBuffer.readByte();
        byte b2 = readBuffer.readByte();
        byte b3 = readBuffer.readByte();
        byte b4 = readBuffer.readByte();
        if(b1 == (byte) 'a' && b2 == (byte) 'l' && b3 == (byte) 's' && b4 == (byte) 'e') {
            assignBoolean(Boolean.FALSE, readBuffer);
        }
    }

    /**
     *   If first byte is 'n', then the value could be null
     */
    void decodeNullValue(ReadBuffer readBuffer) {
        byte b1 = readBuffer.readByte();
        byte b2 = readBuffer.readByte();
        byte b3 = readBuffer.readByte();
        if(b1 == (byte) 'u' && b2 == (byte) 'l' && b3 == (byte) 'l') {
            assignNull();
        }
    }

    /**
     *   After encountering first ' " ' in the value, read all the bytes into the buffer to construct a full string
     */
    void decodeStringValue(byte b, ReadBuffer readBuffer) {
        switch (b) {
            case Constants.QUOTE -> {
                assignString(new String(wb.rawArray(), Constants.ZERO, wb.writeIndex(), StandardCharsets.UTF_8), readBuffer);
                wb.reset();
                state = State.STRING_END;
            }
            case Constants.ESCAPE -> {
                state = State.ESCAPE;
            }
            default -> throw new JsonParseException(readBuffer);
        }
    }

    void decodeStringEndValue(byte b, ReadBuffer readBuffer) {
        switch (b) {
            case Constants.COMMA -> {
                state = State.COMMA;
            }
            case Constants.RCB -> {
                if(place == Place.IN_OBJECT) {

                }else {
                    throw new JsonParseException(readBuffer);
                }
            }
        }
    }

    void decodeEscape(byte b, ReadBuffer readBuffer) {
        switch (b) {
            case Constants.ESCAPE -> wb.writeByte(Constants.ESCAPE);
            case Constants.QUOTE -> wb.writeByte(Constants.QUOTE);
            case Constants.SLASH -> wb.writeByte(Constants.SLASH);
            case (byte) 'b' -> wb.writeByte(Constants.BS);
            case (byte) 'f' -> wb.writeByte(Constants.FF);
            case (byte) 'n' -> wb.writeByte(Constants.LF);
            case (byte) 'r' -> wb.writeByte(Constants.CR);
            case (byte) 't' -> wb.writeByte(Constants.HT);
            case (byte) 'u' -> decodeUnicode(readBuffer);
            default -> {
                if(b < Constants.SPACE || b == Constants.DEL) {
                    throw new JsonParseException(readBuffer);
                }
            }
        }
    }

    void assignBoolean(Boolean value, ReadBuffer readBuffer) {
        GtInfo gtInfo = jsonNodes.getLast();
        MetaInfo metaInfo = gtInfo.gt().metaInfo(key);
        if(metaInfo != null) {
            if(metaInfo.type() == Boolean.class) {
                metaInfo.setter().accept(gtInfo.obj(), value);
                key = Constants.EMPTY_STRING;
            }else {
                throw new JsonParseException(readBuffer);
            }
        }
    }

    void assignNull() {
        GtInfo gtInfo = jsonNodes.getLast();
        MetaInfo metaInfo = gtInfo.gt().metaInfo(key);
        if(metaInfo != null) {
            metaInfo.setter().accept(gtInfo.obj(), null);
            key = Constants.EMPTY_STRING;
        }
    }

    void assignString(String value, ReadBuffer readBuffer) {
        GtInfo gtInfo = jsonNodes.getLast();
        MetaInfo metaInfo = gtInfo.gt().metaInfo(key);
        if(metaInfo != null) {
            Class<?> type = metaInfo.type();
            metaInfo.setter().accept(gtInfo.obj(), type.isEnum() ? metaInfo.enumMap().get(value) : stringToObject(type, value, readBuffer));
            key = Constants.EMPTY_STRING;
        }
    }

    void assign() {

    }

    /**
     *   Convert json string to its corresponding java class type
     */
    private Object stringToObject(Class<?> type, String s, ReadBuffer readBuffer) {
        if(type == String.class) {
            return s;
        }else if(type == Byte.class) {
            return Byte.valueOf(s);
        }else if(type == Short.class) {
            return Short.valueOf(s);
        }else if(type == Integer.class) {
            return Integer.valueOf(s);
        }else if(type == Long.class) {
            return Long.valueOf(s);
        }else if(type == Float.class) {
            return Float.valueOf(s);
        }else if(type == Double.class) {
            return Double.valueOf(s);
        }else if(type == LocalDate.class) {
            return LocalDate.parse(s);
        }else if(type == LocalTime.class) {
            return LocalTime.parse(s);
        }else if(type == LocalDateTime.class) {
            return LocalDateTime.parse(s);
        }else {
            throw new JsonParseException(readBuffer);
        }
    }


    private void decodeArray(byte b, ReadBuffer readBuffer) {

    }

    private void decodeArrayEnd(byte b, ReadBuffer readBuffer) {

    }

    /**
     *   Convert unicode to utf-8 bytes
     */
    void decodeUnicode(ReadBuffer readBuffer) {
        int code = Constants.ZERO;
        for(int i = 0; i < 4; i++) {
            code = code << 4 + readHex(readBuffer);
        }
        if(code < 0x80) {
            wb.writeByte((byte) code);
        }else if(code < 0x800) {
            wb.writeByte((byte) (0xC0 | (code >> 6)));
            wb.writeByte((byte) (0x80 | (code & 0x3F)));
        }else {
            wb.writeByte((byte) (0xE0 | (code >> 12)));
            wb.writeByte((byte) (0x80 | ((code >> 6) & 0x3F)));
            wb.writeByte((byte) (0x80 | (code & 0x3F)));
        }
    }


    /**
     *   Determine whether or not should we skip a byte
     */
    private static boolean shouldSkip(byte b) {
        return b == Constants.SPACE || (b >= Constants.HT && b <= Constants.CR);
    }

    private static int readHex(ReadBuffer readBuffer) {
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
}
