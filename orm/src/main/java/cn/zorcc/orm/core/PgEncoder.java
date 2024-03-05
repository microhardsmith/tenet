package cn.zorcc.orm.core;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;
import cn.zorcc.common.structure.WriteBuffer;
import cn.zorcc.orm.PgConfig;
import cn.zorcc.orm.frontend.*;

import java.nio.charset.StandardCharsets;

public class PgEncoder implements Encoder {

    @Override
    public void encode(WriteBuffer writeBuffer, Object o) {
        switch (o) {
            case PgStartUpMsg(PgConfig pgConfig) -> encodePgStartUpMsg(writeBuffer, pgConfig);
            case PgAuthSaslInitialResponseMsg(String mechanism, String clientFirstMsg) -> encodeSaslInitialResponseMsg(writeBuffer, mechanism, clientFirstMsg);
            case PgAuthSaslResponseMsg(String clientFinalMsg) -> encodeSaslResponseMsg(writeBuffer, clientFinalMsg);
            case PgQueryMsg pgQueryMsg -> encodeQueryMsg(writeBuffer, pgQueryMsg);
            case PgPasswordMsg pgPasswordMsg -> encodePasswordMsg(writeBuffer, pgPasswordMsg);
            case PgParseMsg pgParseMsg -> encodeParseMsg(writeBuffer, pgParseMsg);
            case PgDescribeMsg pgDescribeMsg -> encodeDescribeMsg(writeBuffer, pgDescribeMsg);
            case PgFlushMsg() -> encodePgFlushMsg(writeBuffer);
            case PgExecuteMsg pgExecuteMsg -> encodePgExecuteMsg(writeBuffer, pgExecuteMsg);
            case PgSyncMsg() -> encodePgSyncMsg(writeBuffer);
            case PgTerminateMsg() -> encodePgTerminateMsg(writeBuffer);
            default -> throw new FrameworkException(ExceptionType.SQL, Constants.UNREACHED);
        }
    }

    private void encodeDescribeMsg(WriteBuffer writeBuffer, PgDescribeMsg pgDescribeMsg) {
        writeBuffer.writeByte(PgConstants.DESCRIBE);
        long currentIndex = writeBuffer.writeIndex();
        writeBuffer.writeInt(Integer.MAX_VALUE);
        writeBuffer.writeByte(pgDescribeMsg.type());
        writeBuffer.writeStr(pgDescribeMsg.name());
        writeBuffer.setInt(currentIndex, (int) (writeBuffer.writeIndex() - currentIndex));
    }

    private void encodePgExecuteMsg(WriteBuffer writeBuffer, PgExecuteMsg pgExecuteMsg) {
        writeBuffer.writeByte(PgConstants.EXECUTE);
        long currentIndex = writeBuffer.writeIndex();
        writeBuffer.writeInt(Integer.MAX_VALUE);
        writeBuffer.writeStr(pgExecuteMsg.portal());
        writeBuffer.writeInt(pgExecuteMsg.maxRowsToReturn());
        writeBuffer.setInt(currentIndex, (int) (writeBuffer.writeIndex() - currentIndex));
    }

    private void encodePgFlushMsg(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(PgConstants.FLUSH);
        writeBuffer.writeInt(4);
    }

    private void encodeParseMsg(WriteBuffer writeBuffer, PgParseMsg pgParseMsg) {
        writeBuffer.writeByte(PgConstants.PARSE);
        long currentIndex = writeBuffer.writeIndex();
        writeBuffer.writeInt(Integer.MAX_VALUE);
        writeBuffer.writeStr(pgParseMsg.name());
        writeBuffer.writeStr(pgParseMsg.sql());
        short len = pgParseMsg.len();
        writeBuffer.writeShort(len);
        if(len > 0) {
            for (int oid : pgParseMsg.objectIds()) {
                writeBuffer.writeInt(oid);
            }
        }
        writeBuffer.setInt(currentIndex, (int) (writeBuffer.writeIndex() - currentIndex));
    }

    private void encodePasswordMsg(WriteBuffer writeBuffer, PgPasswordMsg pgPasswordMsg) {
        String password = pgPasswordMsg.password();
        writeBuffer.writeByte(PgConstants.PASSWORD);
        long currentIndex = writeBuffer.writeIndex();
        writeBuffer.writeInt(Integer.MAX_VALUE);
        writeBuffer.writeStr(password);
        writeBuffer.setInt(currentIndex, (int) (writeBuffer.writeIndex() - currentIndex));
    }

    private void encodeQueryMsg(WriteBuffer writeBuffer, PgQueryMsg pgQueryMsg) {
        String sql = pgQueryMsg.sql();
        writeBuffer.writeByte(PgConstants.QUERY);
        long currentIndex = writeBuffer.writeIndex();
        writeBuffer.writeInt(Integer.MAX_VALUE);
        writeBuffer.writeStr(sql);
        writeBuffer.setInt(currentIndex, (int) (writeBuffer.writeIndex() - currentIndex));
    }

    private void encodePgTerminateMsg(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(PgConstants.TERMINATE);
        writeBuffer.writeInt(4);
    }

    private void encodePgSyncMsg(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(PgConstants.SYNC);
        writeBuffer.writeInt(4);
    }

    private void encodePgStartUpMsg(WriteBuffer buffer, PgConfig pgConfig) {
        long currentIndex = buffer.writeIndex();
        buffer.writeInt(Integer.MAX_VALUE);
        buffer.writeInt(3 << 16);
        buffer.writeStr(PgConstants.USER);
        buffer.writeStr(pgConfig.getUsername());
        buffer.writeStr(PgConstants.DATABASE);
        buffer.writeStr(pgConfig.getDatabaseName());
        buffer.writeStr(PgConstants.CLIENT_ENCODING);
        buffer.writeStr(PgConstants.UTF_8);
        buffer.writeStr(PgConstants.APPLICATION_NAME);
        buffer.writeStr(PgConstants.DEFAULT_APPLICATION_NAME);
        buffer.writeStr(PgConstants.DATE_STYLE);
        buffer.writeStr(PgConstants.ISO);
        String currentSchema = pgConfig.getCurrentSchema();
        if (currentSchema != null && !currentSchema.isBlank()) {
            buffer.writeStr(PgConstants.SEARCH_PATH);
            buffer.writeStr(currentSchema);
        }
        buffer.writeStr(PgConstants.FLOAT_PRECISION);
        buffer.writeStr(PgConstants.DEFAULT_FLOAT_PRECISION);
        buffer.writeByte(Constants.NUT);
        buffer.setInt(currentIndex, (int) (buffer.writeIndex() - currentIndex));
    }

    private void encodeSaslInitialResponseMsg(WriteBuffer writeBuffer, String mechanism, String clientFirstMsg) {
        writeBuffer.writeByte(PgConstants.SASL_RESPONSE);
        long currentIndex = writeBuffer.writeIndex();
        writeBuffer.writeInt(Integer.MAX_VALUE);
        writeBuffer.writeStr(mechanism);
        if(clientFirstMsg == null || clientFirstMsg.isEmpty()) {
            writeBuffer.writeInt(-1);
        }else {
            byte[] bytes = clientFirstMsg.getBytes(StandardCharsets.UTF_8);
            writeBuffer.writeInt(bytes.length);
            writeBuffer.writeBytes(bytes);
        }
        writeBuffer.setInt(currentIndex, (int) (writeBuffer.writeIndex() - currentIndex));
    }

    private void encodeSaslResponseMsg(WriteBuffer writeBuffer, String clientFinalMsg) {
        byte[] bytes = clientFinalMsg.getBytes(StandardCharsets.UTF_8);
        writeBuffer.writeByte(PgConstants.SASL_RESPONSE);
        writeBuffer.writeInt(4 + bytes.length);
        writeBuffer.writeBytes(bytes);
    }
}
