package cn.zorcc.common.json;

import cn.zorcc.common.*;
import cn.zorcc.common.exception.JsonParseException;

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
        if(checkSeparator(readBuffer, Constants.RCB)) {
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
                    return newObjectOrRecordNode(readBuffer, metaInfo.fieldClass(), metaInfo.genericType());
                }
                case Constants.LSB -> {
                    return newCollectionNode(readBuffer, metaInfo.fieldClass(), metaInfo.genericType());
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
                        metaInfo.setter().accept(target, convertJsonNumberValue(fieldClass, writeBuffer.toString()));
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
