package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.network.Encoder;
import cn.zorcc.orm.PgConfig;

import java.nio.charset.StandardCharsets;

public class PgEncoder implements Encoder {
    private final PgConfig pgConfig;

    public PgEncoder(PgConfig pgConfig) {
        this.pgConfig = pgConfig;
    }
    @Override
    public void encode(WriteBuffer writeBuffer, Object o) {
        switch (o) {
            case PgStartUpMsg ignored -> encodePgStartUpMsg(writeBuffer);
            case PgAuthSaslInitialMsg saslInitialMsg -> encodeSaslInitialMsg(writeBuffer, saslInitialMsg);
            default -> throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void encodePgStartUpMsg(WriteBuffer buffer) {
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

    private void encodeSaslInitialMsg(WriteBuffer writeBuffer, PgAuthSaslInitialMsg saslInitialMsg) {
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
}
