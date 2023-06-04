package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;
import cn.zorcc.orm.PgConfig;

import java.nio.charset.StandardCharsets;

public class PgEncoder implements Encoder {

    @SuppressWarnings("unused")
    @Override
    public void encode(WriteBuffer writeBuffer, Object o) {
        switch (o) {
            case PgStartUpMsg pgStartUpMsg -> encodePgStartUpMsg(writeBuffer, pgStartUpMsg);
            case PgAuthSaslInitialResponseMsg pgAuthSaslInitialResponseMsg -> encodeSaslInitialResponseMsg(writeBuffer, pgAuthSaslInitialResponseMsg);
            case PgAuthSaslResponseMsg pgAuthSaslResponseMsg -> encodeSaslResponseMsg(writeBuffer, pgAuthSaslResponseMsg);
            case PgSyncMsg pgSyncMsg -> encodePgSyncMsg(writeBuffer);
            case PgTerminateMsg pgTerminateMsg -> encodePgTerminateMsg(writeBuffer);
            default -> throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void encodePgTerminateMsg(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(PgConstants.TERMINATE);
        writeBuffer.writeInt(4);
    }

    private void encodePgSyncMsg(WriteBuffer writeBuffer) {
        writeBuffer.writeByte(PgConstants.SYNC);
        writeBuffer.writeInt(4);
    }

    private void encodePgStartUpMsg(WriteBuffer buffer, PgStartUpMsg pgStartUpMsg) {
        PgConfig pgConfig = pgStartUpMsg.pgConfig();
        buffer.writeInt(0);
        buffer.writeInt(3 << 16);
        buffer.writeCStr(PgConstants.USER);
        buffer.writeCStr(pgConfig.getUsername());
        buffer.writeCStr(PgConstants.DATABASE);
        buffer.writeCStr(pgConfig.getDatabaseName());
        buffer.writeCStr(PgConstants.CLIENT_ENCODING);
        buffer.writeCStr(PgConstants.UTF_8);
        buffer.writeCStr(PgConstants.APPLICATION_NAME);
        buffer.writeCStr(PgConstants.DEFAULT_APPLICATION_NAME);
        buffer.writeCStr(PgConstants.DATE_STYLE);
        buffer.writeCStr(PgConstants.ISO);
        String currentSchema = pgConfig.getCurrentSchema();
        if (currentSchema != null && !currentSchema.isBlank()) {
            buffer.writeCStr(PgConstants.SEARCH_PATH);
            buffer.writeCStr(currentSchema);
        }
        buffer.writeCStr(PgConstants.FLOAT_PRECISION);
        buffer.writeCStr(PgConstants.DEFAULT_FLOAT_PRECISION);
        buffer.writeByte(Constants.NUT);
        buffer.setInt(0L, (int) buffer.writeIndex());
    }

    private void encodeSaslInitialResponseMsg(WriteBuffer writeBuffer, PgAuthSaslInitialResponseMsg saslInitialMsg) {
        String mechanism = saslInitialMsg.mechanism();
        String clientFirstMsg = saslInitialMsg.clientFirstMsg();
        writeBuffer.writeByte(PgConstants.SASL_RESPONSE);
        writeBuffer.writeInt(0);
        writeBuffer.writeCStr(mechanism);
        if(clientFirstMsg == null || clientFirstMsg.isEmpty()) {
            writeBuffer.writeInt(-1);
        }else {
            byte[] bytes = clientFirstMsg.getBytes(StandardCharsets.UTF_8);
            writeBuffer.writeInt(bytes.length);
            writeBuffer.writeBytes(bytes);
        }
        writeBuffer.setInt(1L, (int) writeBuffer.writeIndex() - 1);
    }

    private void encodeSaslResponseMsg(WriteBuffer writeBuffer, PgAuthSaslResponseMsg saslResponseMsg) {
        int len = saslResponseMsg.len();
        byte[] bytes = saslResponseMsg.bytes();
        writeBuffer.writeByte(PgConstants.SASL_RESPONSE);
        writeBuffer.writeInt(4 + len);
        writeBuffer.writeBytes(bytes);
    }
}
