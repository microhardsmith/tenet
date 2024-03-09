package cn.zorcc.common.postgre;

import cn.zorcc.common.Constants;
import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import cn.zorcc.common.log.Logger;
import cn.zorcc.common.network.Channel;
import cn.zorcc.common.network.Handler;
import cn.zorcc.common.network.TaggedResult;
import cn.zorcc.common.structure.ReadBuffer;
import cn.zorcc.common.structure.WriteBuffer;
import com.ongres.scram.client.ScramClient;
import com.ongres.scram.client.ScramSession;
import com.ongres.scram.common.exception.ScramInvalidServerSignatureException;
import com.ongres.scram.common.exception.ScramParseException;
import com.ongres.scram.common.exception.ScramServerErrorException;
import com.ongres.scram.common.stringprep.StringPreparations;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public final class PgHandler implements Handler {
    private static final Logger log = new Logger(PgHandler.class);
    private static final int STARTUP = 1;
    private static final int WAITING_CLEAR_TEXT_PASSWORD = 2;
    private static final int WAITING_MD5_PASSWORD = 3;
    private static final int WAITING_SASL_CONTINUE = 4;
    private static final int WAITING_SASL_COMPLETE = 5;
    private static final int WAITING_SASL_AUTH_OK = 6;
    private static final int AUTH_OK = 10;
    private static final int CONFIGURING_SCHEMA = 11;
    private static final int CONFIGURING_ENCODING = 13;
    private static final int READY_FOR_QUERY = 15;
    private final PgConfig pgConfig;
    private int state = STARTUP;
    private PgStatus status = PgStatus.UNSET;
    private ScramSession scramSession;
    private ScramSession.ClientFinalProcessor clientFinalProcessor;
    private static final byte[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final LocalDateTime baseLocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    private static final LocalDate baseLocalDate = LocalDate.of(2000, 1, 1);
    private static final DateTimeFormatter timetzFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_TIME)
            .appendOffset("+HH:mm", "00:00")
            .toFormatter();
    private static final DateTimeFormatter timestampFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter();
    private static final DateTimeFormatter timestamptzFormatter = new DateTimeFormatterBuilder()
            .append(timestampFormatter)
            .appendOffset("+HH:mm", "00:00")
            .toFormatter();

    private static final Map<Byte, String> errTypeMap = Map.ofEntries(
            Map.entry((byte) 'S', "Severity"),
            Map.entry((byte) 'V', "Severity"),
            Map.entry((byte) 'C', "Code"),
            Map.entry((byte) 'M', "Message"),
            Map.entry((byte) 'D', "Detail"),
            Map.entry((byte) 'H', "Hint"),
            Map.entry((byte) 'P', "Position"),
            Map.entry((byte) 'p', "Internal Position"),
            Map.entry((byte) 'q', "Internal Query"),
            Map.entry((byte) 'W', "Where"),
            Map.entry((byte) 's', "Schema name"),
            Map.entry((byte) 't', "Table name"),
            Map.entry((byte) 'c', "Column name"),
            Map.entry((byte) 'd', "Data type name"),
            Map.entry((byte) 'n', "Constraint name"),
            Map.entry((byte) 'F', "File"),
            Map.entry((byte) 'L', "Line"),
            Map.entry((byte) 'R', "Routine"));

    private static byte[] toHex(byte[] data) {
        byte[] hex = new byte[data.length << 1];
        int offset = 0;
        for (byte b : data) {
            int v = b & 0xFF;
            hex[offset++] = hexArray[v >> 4];
            hex[offset++] = hexArray[v & 0xF];
        }
        return hex;
    }

    public PgHandler(PgConfig pgConfig) {
        this.pgConfig = pgConfig;
    }

    @Override
    public void onFailed(Channel channel) {
        log.debug(STR."Failed to establish postgresql connection with loc : \{channel.loc()}");
    }

    @Override
    public void onConnected(Channel channel) {
        log.debug(STR."Postgresql connection established, target loc : \{channel.loc()}");
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            writeBuffer.writeInt(Integer.MIN_VALUE);
            writeBuffer.writeInt(3 << 16);
            writeBuffer.writeStr("user");
            writeBuffer.writeStr(pgConfig.getUserName());
            writeBuffer.writeStr("database");
            writeBuffer.writeStr(pgConfig.getDatabaseName());
            channel.sendMsg(new PgMsg(Constants.NUT, writeBuffer.asSegment()));
        }
    }

    @Override
    public TaggedResult onRecv(Channel channel, Object data) {
        if(data instanceof PgMsg pgMsg) {
            return onMsg(pgMsg, channel);
        }else {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private TaggedResult onMsg(PgMsg pgMsg, Channel channel) {
        byte type = pgMsg.type();
        MemorySegment data = pgMsg.data();
        switch (state) {
            case STARTUP -> handleStartUpMsg(type, data, channel);
            case WAITING_CLEAR_TEXT_PASSWORD, WAITING_MD5_PASSWORD, WAITING_SASL_AUTH_OK -> handleAuthenticationResult(type, data);
            case WAITING_SASL_CONTINUE -> handleSaslContinue(type, data, channel);
            case WAITING_SASL_COMPLETE -> handleSaslComplete(type, data);
            case AUTH_OK -> handleMsgAfterAuth(type, data, channel);
            case CONFIGURING_SCHEMA -> handleConfiguringSchema(type, channel);
            case CONFIGURING_ENCODING -> handleConfiguringEncoding(type, data);
        }
        return null;
    }

    private void handleStartUpMsg(byte type, MemorySegment data, Channel channel) {
        if(type == Constants.PG_AUTH) {
            ReadBuffer readBuffer = new ReadBuffer(data);
            int authType = readBuffer.readInt();
            switch (authType) {
                case Constants.PG_AUTH_MD5_PASSWORD -> handleMd5Msg(readBuffer, channel);
                case Constants.PG_AUTH_CLEAR_TEXT_PASSWORD -> handleClearTextPasswordMsg(channel);
                case Constants.PG_AUTH_SASL_PASSWORD -> handleSaslPasswordMsg(readBuffer, channel);
                default -> throw new FrameworkException(ExceptionType.POSTGRESQL, STR."Unsupported postgresql authentication method, protocol code : \{authType}");
            }
        }else {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void handleSaslPasswordMsg(ReadBuffer readBuffer, Channel channel) {
        state = WAITING_SASL_CONTINUE;
        List<String> mechanisms = readBuffer.readMultipleStr();
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            ScramClient scramClient = ScramClient.channelBinding(ScramClient.ChannelBinding.NO)
                    .stringPreparation(StringPreparations.SASL_PREPARATION)
                    .selectMechanismBasedOnServerAdvertised(mechanisms.toArray(new String[0])).setup();
            String mechanism = scramClient.getScramMechanism().getName();
            log.debug(STR."Postgresql using scram mechanism : \{mechanism}");
            scramSession = scramClient.scramSession("*");
            byte[] clientFirstMessage = scramSession.clientFirstMessage().getBytes(StandardCharsets.UTF_8);
            writeBuffer.writeStr(mechanism);
            if(clientFirstMessage.length > 0) {
                writeBuffer.writeInt(clientFirstMessage.length);
                writeBuffer.writeBytes(clientFirstMessage);
            }else {
                writeBuffer.writeInt(-1);
            }
            channel.sendMsg(new PgMsg(Constants.PG_PASSWORD, writeBuffer.asSegment()));
        }
    }

    private void handleSaslContinue(byte type, MemorySegment data, Channel channel) {
        state = WAITING_SASL_COMPLETE;
        if(type != Constants.PG_AUTH) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
        ReadBuffer readBuffer = new ReadBuffer(data);
        int code = readBuffer.readInt();
        if(code != Constants.PG_AUTH_SASL_CONTINUE) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
        byte[] authenticationData = readBuffer.readBytes(readBuffer.available());
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            ScramSession.ServerFirstProcessor serverFirstProcessor = scramSession.receiveServerFirstMessage(new String(authenticationData, StandardCharsets.UTF_8));
            clientFinalProcessor = serverFirstProcessor.clientFinalProcessor(pgConfig.getPassword());
            String clientFinalMessage = clientFinalProcessor.clientFinalMessage();
            writeBuffer.writeStr(clientFinalMessage);
            channel.sendMsg(new PgMsg(Constants.PG_PASSWORD, writeBuffer.asSegment()));
        }catch (ScramParseException e) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, "SASL authentication failed in continue phrase", e);
        }
    }

    private void handleSaslComplete(byte type, MemorySegment data) {
        state = WAITING_SASL_AUTH_OK;
        if(type != Constants.PG_AUTH) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
        ReadBuffer readBuffer = new ReadBuffer(data);
        int code = readBuffer.readInt();
        if(code != Constants.PG_AUTH_SASL_FINAL) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
        byte[] authenticationData = readBuffer.readBytes(readBuffer.available());
        try{
            clientFinalProcessor.receiveServerFinalMessage(new String(authenticationData, StandardCharsets.UTF_8));
        }catch (ScramParseException | ScramServerErrorException | ScramInvalidServerSignatureException e) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, "SASL authentication failed in final phrase", e);
        }
    }

    private void handleMd5Msg(ReadBuffer readBuffer, Channel channel) {
        state = WAITING_MD5_PASSWORD;
        byte[] salt = readBuffer.readBytes(4);
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            MessageDigest digest = MessageDigest.getInstance(Constants.MD_5);
            digest.update(pgConfig.getPassword().getBytes(StandardCharsets.UTF_8));
            digest.update(pgConfig.getUserName().getBytes(StandardCharsets.UTF_8));
            byte[] firstDigestResult = digest.digest();
            digest.reset();
            digest.update(toHex(firstDigestResult));
            digest.update(salt);
            byte[] secondDigestResult = digest.digest();
            writeBuffer.writeByte((byte) 'm');
            writeBuffer.writeByte((byte) 'd');
            writeBuffer.writeByte((byte) '5');
            writeBuffer.writeBytes(toHex(secondDigestResult));
            channel.sendMsg(new PgMsg(Constants.PG_PASSWORD, writeBuffer.asSegment()));
        }catch (NoSuchAlgorithmException e) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void handleClearTextPasswordMsg(Channel channel) {
        state = WAITING_CLEAR_TEXT_PASSWORD;
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            writeBuffer.writeStr(pgConfig.getPassword());
            channel.sendMsg(new PgMsg(Constants.PG_PASSWORD, writeBuffer.asSegment()));
        }
    }

    private void handleAuthenticationResult(byte type, MemorySegment data) {
        ReadBuffer readBuffer = new ReadBuffer(data);
        if(type == Constants.PG_AUTH) {
            int authCode = readBuffer.readInt();
            if(authCode == Constants.PG_AUTH_OK) {
                state = AUTH_OK;
            }else {
                throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
            }
        }else if(type == Constants.PG_ERROR) {
            handleErrorResponse(readBuffer);
        }else {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void handleMsgAfterAuth(byte type, MemorySegment data, Channel channel) {
        ReadBuffer readBuffer = new ReadBuffer(data);
        if(type == Constants.PG_PARAMETER_STATUS) {
            String key = readBuffer.readStr();
            String value = readBuffer.readStr();
            log.debug(STR."Postgresql parameter status received, key : \{key}, value : \{value}");
        }else if(type == Constants.PG_BACKEND_KEY_DATA) {
            int processId = readBuffer.readInt();
            int secretKey = readBuffer.readInt();
            log.debug(STR."Postgresql backend key data received, processId : \{processId}, secretKey : \{secretKey}");
        }else if(type == Constants.PG_READY) {
            if (readBuffer.readByte() == Constants.PG_TRANSACTION_IDLE) {
                status = PgStatus.IDLE;
            }else {
                throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
            }
            String schema = pgConfig.getCurrentSchema();
            if(schema == null || schema.isBlank() || schema.equals("public")) {
                sendConfigureEncodingMsg(channel);
            }else {
                sendConfigureSchemaMsg(channel, schema);
            }
        }
    }

    private void sendConfigureEncodingMsg(Channel channel) {
        state = CONFIGURING_ENCODING;
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            writeBuffer.writeStr("SET client_encoding TO 'utf-8' ");
            channel.sendMsg(new PgMsg(Constants.PG_SIMPLE_QUERY, writeBuffer.asSegment()));
        }
    }

    private void sendConfigureSchemaMsg(Channel channel, String schema) {
        state = CONFIGURING_SCHEMA;
        try(WriteBuffer writeBuffer = WriteBuffer.newHeapWriteBuffer()) {
            writeBuffer.writeStr(STR."SET search_path TO \{schema} ");
            channel.sendMsg(new PgMsg(Constants.PG_SIMPLE_QUERY, writeBuffer.asSegment()));
        }
    }

    private void handleErrorResponse(ReadBuffer readBuffer) {
        Map<String, String> errMap = new HashMap<>();
        for( ; ; ) {
            byte errCode = readBuffer.readByte();
            if(errCode != Constants.NUT) {
                String errStr = readBuffer.readStr();
                errMap.put(errTypeMap.get(errCode), errStr);
            }else {
                break ;
            }
        }
        if(state < READY_FOR_QUERY) {
            throw new FrameworkException(ExceptionType.POSTGRESQL, STR."Authentification failed, Error response : \{errMap}");
        }else {
            log.error(STR."Error response : \{errMap}");
        }
    }

    private void handleConfiguringEncoding(byte type, MemorySegment data) {
        if(type == Constants.PG_COMMAND_COMPLETION) {
            log.debug("Configuring client-encoding completed");
        }else if(type == Constants.PG_PARAMETER_STATUS) {
            ReadBuffer readBuffer = new ReadBuffer(data);
            String key = readBuffer.readStr();
            String value = readBuffer.readStr();
            log.debug(STR."Parameter status updated, key : \{key}, value : \{value}");
        }else if(type == Constants.PG_READY) {
            state = READY_FOR_QUERY;
            // TODO add to manager
        }else {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    private void handleConfiguringSchema(byte type, Channel channel) {
        if(type == Constants.PG_COMMAND_COMPLETION) {
            log.debug("Configuring schema completed}");
        }else if(type == Constants.PG_READY) {
            sendConfigureEncodingMsg(channel);
        }else {
            throw new FrameworkException(ExceptionType.POSTGRESQL, Constants.UNREACHED);
        }
    }

    @Override
    public void onShutdown(Channel channel) {

    }

    @Override
    public void onRemoved(Channel channel) {

    }

}
