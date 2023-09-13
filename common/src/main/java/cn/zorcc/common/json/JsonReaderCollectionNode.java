package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.exception.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        checkFirstByte(readBuffer, Constants.RSB);
        for ( ; ; ) {
            byte b = readNextByte(readBuffer);
            switch (b) {
                case Constants.LCB -> {
                    if(elementType instanceof Class<?> c) {
                        return toNext(new JsonReaderObjectNode(readBuffer, c));
                    }else if(elementType instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?> c && Map.class.isAssignableFrom(c)) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if(actualTypeArguments[Constants.ZERO] instanceof Class<?> keyClass && keyClass == String.class) {
                            return toNext(new JsonReaderMapNode(readBuffer, c, actualTypeArguments[Constants.ONE]));
                        }else {
                            throw new JsonParseException(Constants.JSON_KEY_TYPE_ERR);
                        }
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                case Constants.LSB -> {
                    if(elementType instanceof Class<?> c && c.isArray()) {
                        return toNext(new JsonReaderCollectionNode(readBuffer, c, c.componentType()));
                    }else if(elementType instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c && Collection.class.isAssignableFrom(c)) {
                        return toNext(new JsonReaderCollectionNode(readBuffer, c, pt.getActualTypeArguments()[Constants.ZERO]));
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
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
