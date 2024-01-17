package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.exception.JsonParseException;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.structure.WriteBuffer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class JsonReaderCollectionNode extends JsonReaderNode {
    private final ReadBuffer readBuffer;
    private final List<Object> list = new ArrayList<>();
    private final WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer();
    private final Class<?> collectionType;
    private final Type elementType;

    public JsonReaderCollectionNode(ReadBuffer readBuffer, Class<?> collectionType, Type elementType) {
        this.readBuffer = readBuffer;
        this.collectionType = collectionType;
        this.elementType = elementType;
    }

    public List<Object> list() {
        return list;
    }

    @Override
    public JsonReaderNode tryDeserialize() {
        if(checkSeparator(readBuffer, Constants.RSB)) {
            return toPrev();
        }
        for ( ; ; ) {
            byte b = readNextByte(readBuffer);
            switch (b) {
                case Constants.LCB -> {
                    return newObjectOrRecordNode(readBuffer, elementType);
                }
                case Constants.LSB -> {
                    return newCollectionNode(readBuffer, elementType);
                }
                case Constants.QUOTE -> {
                    if(elementType instanceof Class<?> c && c == String.class) {
                        String strValue = readStringUntil(readBuffer, writeBuffer, Constants.QUOTE);
                        list.add(strValue);
                        if (checkSeparator(readBuffer, Constants.RSB)) {
                            return toPrev();
                        }
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                case (byte) 'n' -> {
                    readFollowingNullValue(readBuffer);
                    list.add(null);
                    if (checkSeparator(readBuffer, Constants.RSB)) {
                        return toPrev();
                    }
                }
                case (byte) 't' -> {
                    if(elementType instanceof Class<?> c && (c == boolean.class || c == Boolean.class)) {
                        readFollowingTrueValue(readBuffer);
                        list.add(Boolean.TRUE);
                        if (checkSeparator(readBuffer, Constants.RSB)) {
                            return toPrev();
                        }
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                case (byte) 'f' -> {
                    if(elementType instanceof Class<?> c && (c == boolean.class || c == Boolean.class)) {
                        readFollowingFalseValue(readBuffer);
                        list.add(Boolean.FALSE);
                        if (checkSeparator(readBuffer, Constants.RSB)) {
                            return toPrev();
                        }
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                default -> {
                    if(elementType instanceof Class<?> c && (c.isPrimitive() || Number.class.isAssignableFrom(c))) {
                        writeBuffer.writeByte(b);
                        byte sep = readUntilMatch(readBuffer, writeBuffer, Constants.COMMA, Constants.RSB);
                        list.add(convertJsonNumberValue(c, writeBuffer.toString()));
                        if(sep == Constants.RSB) {
                            return toPrev();
                        }else if(sep != Constants.COMMA) {
                            throw new JsonParseException(readBuffer);
                        }
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
            }
        }
    }

    @Override
    public void setJsonObject(Object value) {
        list.add(value);
    }

    @Override
    public Object getJsonObject() {
        return convertJsonCollectionValue(collectionType, list);
    }


}
