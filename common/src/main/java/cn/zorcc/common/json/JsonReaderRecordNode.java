package cn.zorcc.common.json;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Record;
import cn.zorcc.common.RecordInfo;
import cn.zorcc.common.exception.JsonParseException;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.structure.WriteBuffer;

public final class JsonReaderRecordNode extends JsonReaderNode {
    private final ReadBuffer readBuffer;
    private final Record<?> record;
    private final Object[] args;
    private final WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer();
    private RecordInfo recordInfo;

    public JsonReaderRecordNode(ReadBuffer readBuffer, Class<?> type) {
        this.readBuffer = readBuffer;
        this.record = Record.of(type);
        this.args = record.createElementArray();
    }

    @Override
    protected JsonReaderNode tryDeserialize() {
        if(checkSeparator(readBuffer, Constants.RCB)) {
            return toPrev();
        }
        for( ; ; ) {
            readExpected(readBuffer, Constants.QUOTE);
            String fieldName = readStringUntil(readBuffer, writeBuffer, Constants.QUOTE);
            recordInfo = record.recordInfo(fieldName);
            readExpected(readBuffer, Constants.COLON);
            byte b = readNextByte(readBuffer);
            switch (b) {
                case Constants.LCB -> {
                    return newObjectOrRecordNode(readBuffer, recordInfo.fieldClass(), recordInfo.genericType());
                }
                case Constants.LSB -> {
                    return newCollectionNode(readBuffer, recordInfo.fieldClass(), recordInfo.genericType());
                }
                case Constants.QUOTE -> {
                    String strValue = readStringUntil(readBuffer, writeBuffer, Constants.QUOTE);
                    record.assign(args, recordInfo, convertJsonStringValue(recordInfo.fieldClass(), strValue));
                    if (checkSeparator(readBuffer, Constants.RCB)) {
                        return toPrev();
                    }
                }
                case (byte) 'n' -> {
                    readFollowingNullValue(readBuffer);
                    // for record, null doesn't need to be assigned
                    if (checkSeparator(readBuffer, Constants.RCB)) {
                        return toPrev();
                    }
                }
                case (byte) 't' -> {
                    readFollowingTrueValue(readBuffer);
                    Class<?> fieldClass = recordInfo.fieldClass();
                    if(fieldClass == boolean.class || fieldClass == Boolean.class) {
                        record.assign(args, recordInfo, Boolean.TRUE);
                        if (checkSeparator(readBuffer, Constants.RCB)) {
                            return toPrev();
                        }
                    }else {
                        throw new JsonParseException("Unsupported bool type");
                    }
                }
                case (byte) 'f' -> {
                    readFollowingFalseValue(readBuffer);
                    Class<?> fieldClass = recordInfo.fieldClass();
                    if(fieldClass == boolean.class || fieldClass == Boolean.class) {
                        record.assign(args, recordInfo, Boolean.FALSE);
                        if (checkSeparator(readBuffer, Constants.RCB)) {
                            return toPrev();
                        }
                    }else {
                        throw new JsonParseException("Unsupported bool type");
                    }
                }
                default -> {
                    Class<?> fieldClass = recordInfo.fieldClass();
                    if(fieldClass.isPrimitive() || Number.class.isAssignableFrom(fieldClass)) {
                        writeBuffer.writeByte(b);
                        byte sep = readUntilMatch(readBuffer, writeBuffer, Constants.COMMA, Constants.RCB);
                        record.assign(args, recordInfo, convertJsonNumberValue(fieldClass, writeBuffer.toString()));
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
    protected void setJsonObject(Object value) {
        record.assign(args, recordInfo, value);
    }

    @Override
    protected Object getJsonObject() {
        return record.construct(args);
    }
}
