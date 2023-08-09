package cn.zorcc.orm.core;

import cn.zorcc.common.Constants;
import cn.zorcc.common.Pair;
import cn.zorcc.common.ReadBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Decoder;
import cn.zorcc.orm.backend.*;

import java.util.ArrayList;
import java.util.List;

public class PgDecoder implements Decoder {
    @Override
    public Object decode(ReadBuffer readBuffer) {
        long size = readBuffer.size();
        if(size < 5) {
            return null;
        }
        byte msgType = readBuffer.readByte();
        int msgLength = readBuffer.readInt();
        if(size < msgLength + 1) {
            readBuffer.setReadIndex(Constants.ZERO);
            return null;
        }
        switch (msgType) {
            case PgConstants.AUTH -> {
                return decodeAuthMsg(readBuffer, msgLength);
            }
            case PgConstants.PARAMETER_STATUE -> {
                return decodeParameterStatus(readBuffer,msgLength);
            }
            case PgConstants.BACKEND_KEY_DATA -> {
                return decodeBackendKeyData(readBuffer, msgLength);
            }
            case PgConstants.READY -> {
                return decodeReadyMsg(readBuffer, msgLength);
            }
            case PgConstants.PARSE_COMPLETE -> {
                return decodeParseCompleteMsg(msgLength);
            }
            case PgConstants.BIND_COMPLETE -> {
                return decodeBindCompleteMsg(msgLength);
            }
            case PgConstants.COMMAND_COMPLETE -> {
                return decodeCommandCompleteMsg(readBuffer, msgLength);
            }
            case PgConstants.ROW_DESCRIPTION -> {
                return decodeRowDescriptionMsg(readBuffer, msgLength);
            }
            case PgConstants.DATA_ROW -> {
                return decodeDataRowMsg(readBuffer, msgLength);
            }
            case PgConstants.NO_DATA -> {
                return decodeNoDataMsg(msgLength);
            }
            case PgConstants.EMPTY_QUERY_RESPONSE -> {
                return decodeEmptyQueryResponse(msgLength);
            }
            case PgConstants.NOTICE_RESPONSE -> {
                return decodeStatusResponseMsg(readBuffer, msgLength, false);
            }
            case PgConstants.ERROR_RESPONSE -> {
                return decodeStatusResponseMsg(readBuffer, msgLength, true);
            }
            case PgConstants.CLOSE_COMPLETE -> {
                return decodeCloseCompleteMsg(msgLength);
            }
            default -> throw new FrameworkException(ExceptionType.SQL, "Unrecognized postgresql message type");
        }
    }

    private Object decodeCloseCompleteMsg(int msgLength) {
        checkLength(msgLength, 4);
        return PgCloseCompleteMsg.INSTANCE;
    }

    private Object decodeEmptyQueryResponse(int msgLength) {
        checkLength(msgLength, 4);
        return PgEmptyQueryResponseMsg.INSTANCE;
    }


    private Object decodeStatusResponseMsg(ReadBuffer readBuffer, int msgLength, boolean isErr) {
        long startIndex = readBuffer.readIndex();
        List<Pair<Byte, String>> items = new ArrayList<>();
        for( ; ; ) {
            byte b = readBuffer.readByte();
            if(b == Constants.NUT) {
                break;
            }else {
                items.add(Pair.of(b, readBuffer.readCStr()));
            }
        }
        checkLength(msgLength, (int) (readBuffer.readIndex() - startIndex + 4));
        return new PgStatusResponseMsg(isErr, items);
    }

    private Object decodeNoDataMsg(int msgLength) {
        checkLength(msgLength, 4);
        return PgNoDataMsg.INSTANCE;
    }

    private Object decodeDataRowMsg(ReadBuffer readBuffer, int msgLength) {
        long startIndex = readBuffer.readIndex();
        short len = readBuffer.readShort();
        List<byte[]> data = new ArrayList<>(len);
        for(int i = 0; i < len; i++) {
            int size = readBuffer.readInt();
            byte[] bytes = readBuffer.readBytes(size);
            data.add(bytes);
        }
        checkLength(msgLength, (int) (readBuffer.readIndex() - startIndex + 4));
        return new PgDataRowMsg(len, data);
    }

    private Object decodeRowDescriptionMsg(ReadBuffer readBuffer, int msgLength) {
        long startIndex = readBuffer.readIndex();
        short len = readBuffer.readShort();
        PgRowDescription[] pgRowDescriptions = new PgRowDescription[len];
        for(int i = 0; i < len; i++) {
            String fieldName = readBuffer.readCStr();
            int tableOid = readBuffer.readInt();
            short attr = readBuffer.readShort();
            int fieldOid = readBuffer.readInt();
            short typeSize = readBuffer.readShort();
            int modifier = readBuffer.readInt();
            short format = readBuffer.readShort();
            pgRowDescriptions[i] = new PgRowDescription(fieldName, tableOid, attr, fieldOid, typeSize, modifier, format);
        }
        checkLength(msgLength, (int) (readBuffer.readIndex() - startIndex + 4));
        return new PgRowDescriptionMsg(len, pgRowDescriptions);
    }

    private Object decodeCommandCompleteMsg(ReadBuffer readBuffer, int msgLength) {
        long startIndex = readBuffer.readIndex();
        String tag = readBuffer.readCStr();
        checkLength(msgLength, (int) (readBuffer.readIndex() - startIndex + 4));
        return new PgCommandCompleteMsg(tag);
    }

    private Object decodeBindCompleteMsg(int msgLength) {
        checkLength(msgLength, 4);
        return PgBindCompleteMsg.INSTANCE;
    }

    private Object decodeParseCompleteMsg(int msgLength) {
        checkLength(msgLength, 4);
        return PgParseCompleteMsg.INSTANCE;
    }

    private Object decodeAuthMsg(ReadBuffer readBuffer, int msgLength) {
        int authType = readBuffer.readInt();
        switch (authType) {
            case PgConstants.AUTH_OK -> {
                checkLength(msgLength, 8);
                return PgAuthOkMsg.INSTANCE;
            }
            case PgConstants.AUTH_CLEAR_TEXT_PASSWORD -> {
                checkLength(msgLength, 8);
                return PgAuthClearPwdMsg.INSTANCE;
            }
            case PgConstants.AUTH_MD5_PASSWORD -> {
                checkLength(msgLength, 12);
                return new PgAuthMd5Msg(readBuffer.readBytes(4L));
            }
            case PgConstants.AUTH_SASL_PASSWORD -> {
                return decodeSaslPwdMsg(readBuffer, msgLength);
            }
            case PgConstants.AUTH_SASL_CONTINUE -> {
                return decodeSaslContinueMsg(readBuffer, msgLength);
            }
            case PgConstants.AUTH_SASL_FINAL -> {
                return decodeSaslFinalMsg(readBuffer, msgLength);
            }
            default -> throw new FrameworkException(ExceptionType.SQL, "Unrecognized auth type");
        }
    }

    private Object decodeSaslPwdMsg(ReadBuffer readBuffer, int msgLength) {
        long startIndex = readBuffer.readIndex();
        List<String> mechanisms = new ArrayList<>();
        for( ; ;) {
            String mechanism = readBuffer.readCStr();
            if(mechanism == null) {
                break;
            }
            mechanisms.add(mechanism);
        }
        checkLength(msgLength, (int) (readBuffer.readIndex() - startIndex + 8));
        return new PgAuthSaslPwdMsg(mechanisms);
    }

    private Object decodeSaslContinueMsg(ReadBuffer readBuffer, int msgLength) {
        long startIndex = readBuffer.readIndex();
        String serverFirstMsg = readBuffer.readCStr();
        checkLength(msgLength, (int) (readBuffer.readIndex() - startIndex + 8));
        return new PgAuthSaslContinueMsg(serverFirstMsg);
    }

    private Object decodeSaslFinalMsg(ReadBuffer readBuffer, int msgLength) {
        long startIndex = readBuffer.readIndex();
        String serverFinalMsg = readBuffer.readCStr();
        checkLength(msgLength, (int) (readBuffer.readIndex() - startIndex + 8));
        return new PgAuthSaslFinalMsg(serverFinalMsg);
    }

    private Object decodeParameterStatus(ReadBuffer readBuffer, int msgLength) {
        long startIndex = readBuffer.readIndex();
        String key = readBuffer.readCStr();
        String value = readBuffer.readCStr();
        checkLength(msgLength, (int) (readBuffer.readIndex() - startIndex + 4));
        return new PgParameterStatusMsg(key, value);
    }

    private Object decodeBackendKeyData(ReadBuffer readBuffer, int msgLength) {
        checkLength(msgLength, 12);
        int processId = readBuffer.readInt();
        int secretKey = readBuffer.readInt();
        return new PgBackendKeyDataMsg(processId, secretKey);
    }

    private Object decodeReadyMsg(ReadBuffer readBuffer, int msgLength) {
        checkLength(msgLength, 5);
        PgStatus pgStatus = switch (readBuffer.readByte()) {
            case PgConstants.TRANSACTION_IDLE -> PgStatus.IDLE;
            case PgConstants.TRANSACTION_ON -> PgStatus.TRANSACTION_ON;
            case PgConstants.TRANSACTION_FAIL -> PgStatus.TRANSACTION_FAIL;
            default -> throw new FrameworkException(ExceptionType.SQL, "Unrecognized postgresql transaction status indicator");
        };
        return new PgReadyMsg(pgStatus);
    }

    private void checkLength(int actual, int expected) {
        if(actual != expected) {
            throw new FrameworkException(ExceptionType.SQL, "Msg length corrupted");
        }
    }
}
