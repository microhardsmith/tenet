package cn.zorcc.common.json;

import cn.zorcc.common.*;
import cn.zorcc.common.exception.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public final class JsonReaderObjectNode extends JsonReaderNode {
    private final ReadBuffer readBuffer;
    private final Meta<?> meta;
    private final Object target;
    private final WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer();
    private MetaInfo metaInfo;

    public JsonReaderObjectNode(ReadBuffer readBuffer, Class<?> type) {
        this.readBuffer = readBuffer;
        this.meta = Meta.of(type);
        this.target = meta.constructor().get();
    }

    @Override
    public JsonReaderNode tryDeserialize() {
        if(checkFirstByte(readBuffer, Constants.RCB)) {
            return toPrev();
        }
        for( ; ; ) {
            readExpected(readBuffer, Constants.QUOTE);
            String fieldName = readStringUntil(readBuffer, writeBuffer, Constants.QUOTE);
            metaInfo = meta.metaInfo(fieldName);
            readExpected(readBuffer, Constants.COLON);
            byte b = readNextByte(readBuffer);
            switch (b) {
                case Constants.LCB -> {
                    Class<?> fieldClass = metaInfo.fieldClass();
                    if(Map.class.isAssignableFrom(fieldClass) && metaInfo.genericType() instanceof ParameterizedType pt) {
                        Type[] actualTypeArguments = pt.getActualTypeArguments();
                        if(actualTypeArguments[Constants.ZERO] instanceof Class<?> keyClass && keyClass == String.class) {
                            return toNext(new JsonReaderMapNode(readBuffer, fieldClass, actualTypeArguments[Constants.ONE]));
                        }else {
                            throw new JsonParseException(Constants.JSON_KEY_TYPE_ERR);
                        }
                    }else {
                        return toNext(new JsonReaderObjectNode(readBuffer, fieldClass));
                    }
                }
                case Constants.LSB -> {
                    Class<?> fieldClass = metaInfo.fieldClass();
                    Type genericType = metaInfo.genericType();
                    if(fieldClass.isArray()) {
                        return toNext(new JsonReaderCollectionNode(readBuffer, fieldClass, fieldClass.componentType()));
                    }else if(Collection.class.isAssignableFrom(fieldClass) && genericType instanceof ParameterizedType pt ) {
                        return toNext(new JsonReaderCollectionNode(readBuffer, fieldClass, pt.getActualTypeArguments()[Constants.ZERO]));
                    }else {
                        throw new JsonParseException(Constants.JSON_VALUE_TYPE_ERR);
                    }
                }
                case Constants.QUOTE -> {
                    String strValue = readStringUntil(readBuffer, writeBuffer, Constants.QUOTE);
                    metaInfo.setter().accept(target, convertJsonStringValue(metaInfo.fieldClass(), strValue));
                    if (checkSeparator(readBuffer, Constants.RCB)) {
                        return toPrev();
                    }
                }
                case (byte) 'n' -> {
                   readFollowingNullValue(readBuffer);
                    metaInfo.setter().accept(target, null);
                    if (checkSeparator(readBuffer, Constants.RCB)) {
                        return toPrev();
                    }
                }
                case (byte) 't' -> {
                    readFollowingTrueValue(readBuffer);
                    Class<?> fieldClass = metaInfo.fieldClass();
                    if(fieldClass == boolean.class || fieldClass == Boolean.class) {
                        metaInfo.setter().accept(target, Boolean.TRUE);
                        if (checkSeparator(readBuffer, Constants.RCB)) {
                            return toPrev();
                        }
                    }else {
                        throw new JsonParseException("Unsupported bool type");
                    }
                }
                case (byte) 'f' -> {
                    readFollowingFalseValue(readBuffer);
                    Class<?> fieldClass = metaInfo.fieldClass();
                    if(fieldClass == boolean.class || fieldClass == Boolean.class) {
                        metaInfo.setter().accept(target, Boolean.FALSE);
                        if (checkSeparator(readBuffer, Constants.RCB)) {
                            return toPrev();
                        }
                    }else {
                        throw new JsonParseException("Unsupported bool type");
                    }
                }
                default -> {
                    Class<?> fieldClass = metaInfo.fieldClass();
                    if(fieldClass.isPrimitive() || Number.class.isAssignableFrom(fieldClass)) {
                        writeBuffer.writeByte(b);
                        byte sep = readUntilMatch(readBuffer, writeBuffer, Constants.COMMA, Constants.RCB);
                        metaInfo.setter().accept(target, convertJsonNumberValue(metaInfo.fieldClass(), writeBuffer.toString()));
                        if(sep == Constants.RCB) {
                            return toPrev();
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
        metaInfo.invokeSetter(target, value);
    }

    @Override
    public Object getJsonObject() {
        return target;
    }

}
