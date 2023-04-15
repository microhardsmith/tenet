package cn.zorcc.orm.pg;

import cn.zorcc.common.Constants;
import cn.zorcc.common.WriteBuffer;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

/**
 * postgresql工具类
 * pg中字符串数据使用的是c风格,使用\0作为结束符
 */
@Slf4j
public class PgUtil {
    private static final byte[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    /**
     * 时间戳基准时间
     */
    private static final LocalDateTime baseLocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    /**
     * 日期基准时间
     */
    private static final LocalDate baseLocalDate = LocalDate.of(2000, 1, 1);
    /**
     * timetz文本格式
     */
    private static final DateTimeFormatter timetzFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_TIME)
            .appendOffset("+HH:mm", "00:00")
            .toFormatter();
    /**
     * timestamp文本格式
     */
    private static final DateTimeFormatter timestampFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter();
    /**
     * timestamptz文本格式
     */
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


    /**
     * 从Bytebuf中读取c风格的字符串,如果已经读取到byteBuf的末尾,则返回null
     */
    public static String readString(ByteBuf byteBuf) {
        int readerIndex = byteBuf.readerIndex();
        byte b = byteBuf.readByte();
        if (b == Constants.END_BYTE) {
            return null;
        } else {
            while (b != Constants.END_BYTE) {
                b = byteBuf.readByte();
            }
            return byteBuf.getCharSequence(readerIndex, byteBuf.readerIndex() - readerIndex - 1, StandardCharsets.UTF_8).toString();
        }
    }

    /**
     *  从Bytebuf中读取指定长度的byte组合成String
     */
    public static String readString(ByteBuf byteBuf, int len) {
        return byteBuf.readCharSequence(len, StandardCharsets.UTF_8).toString();
    }

    /**
     * 向Bytebuf写入c风格的字符串,以'\0'结尾,返回写入数据的字节长度
     */
    public static void writeString(ByteBuf byteBuf, String s) {
        if (s != null && !s.isEmpty()) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            for (byte b : bytes) {
                byteBuf.writeByte(b);
            }
        }
        byteBuf.writeByte(Constants.END_BYTE);
    }

    /**
     * 构建简单查询
     */
    public static ByteBuf createQueryByteBuf(String sql) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(PgConstants.QUERY);
        int writerIndex = buffer.writerIndex();
        buffer.writeInt(0);
        PgUtil.writeString(buffer, sql);
        buffer.setInt(writerIndex, buffer.writerIndex() - writerIndex);
        return buffer;
    }

    /**
     * 发送消息至postgresql,如果失败则终止连接
     */
    public static void sendByteBuf(ByteBuf byteBuf, Channel channel, Runnable runnable) {
        channel.writeAndFlush(byteBuf).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                terminate(channel);
                throw new FrameworkException(ExceptionType.ORM, Constants.DB_CONNECTION_ERR, future.cause());
            } else if (runnable != null) {
                runnable.run();
            }
        });
    }

    /**
     * 向postgresql发送终止消息
     */
    public static void terminate(Channel channel) {
        ByteBuf byteBuf = Unpooled.buffer(5);
        byteBuf.writeByte(PgConstants.TERMINATE);
        byteBuf.writeInt(4);
        channel.writeAndFlush(byteBuf).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 处理postgresql错误消息,打印具体日志信息
     */
    public static void handleErrorResponse(ByteBuf byteBuf, int length) {
        int readerIndex = byteBuf.readerIndex();
        byte errType = byteBuf.readByte();
        while (errType != Constants.END_BYTE && byteBuf.readerIndex() - readerIndex <= length) {
            String errTypeMsg = errTypeMap.get(errType);
            String errMsg = readString(byteBuf);
            log.error("Postgresql Error {} : {}", errTypeMsg, errMsg);
            errType = byteBuf.readByte();
        }
    }

    /**
     * 将byte数组转化为16进制表示,从data输出到hex中,hex起始索引为offset
     */
    public static void toHex(byte[] data, byte[] hex, int offset) {
        if (offset + (data.length << 1) > hex.length) {
            throw new FrameworkException(ExceptionType.ORM, "Not enough space for hex");
        }
        for (byte datum : data) {
            int v = datum & 0xFF;
            hex[offset++] = hexArray[v >> 4];
            hex[offset++] = hexArray[v & 0xF];
        }
    }

    /**
     * 在ExtendedQuery中根据PgDataType中指定的数据类型写入对象
     */
    public static void writeObject(Object arg, PgDataType dataType, ByteBuf byteBuf) {
        switch (dataType) {
            case BOOL -> byteBuf.writeBoolean((Boolean) arg);
            case INT2 -> byteBuf.writeShort((Short) arg);
            case INT4 -> byteBuf.writeInt((Integer) arg);
            case INT8 -> byteBuf.writeLong((Long) arg);
            case CHAR, VARCHAR, TEXT -> writeString(byteBuf, (String) arg);
            case FLOAT4 -> byteBuf.writeFloat((Float) arg);
            case FLOAT8 -> byteBuf.writeDouble((Double) arg);
            case NUMERIC -> {
                BigDecimal b = (BigDecimal) arg;
                writeString(byteBuf, b.toPlainString());
            }
            case MONEY -> {
                BigDecimal b = (BigDecimal) arg;
                byteBuf.writeLong(b.multiply(Constants.HUNDRED).longValue()); // Money类型实际记录的是乘以100后舍弃尾数的值
            }
            case DATE -> {
                LocalDate localDate = (LocalDate) arg;
                byteBuf.writeInt((int) -localDate.until(baseLocalDate, ChronoUnit.DAYS));
            }
            case TIME -> {
                LocalTime localTime = (LocalTime) arg;
                byteBuf.writeLong(localTime.getLong(ChronoField.MICRO_OF_DAY));
            }
            case TIMETZ -> {
                OffsetTime offsetTime = (OffsetTime) arg;
                byteBuf.writeLong(offsetTime.toLocalTime().getLong(ChronoField.MICRO_OF_DAY));
                byteBuf.writeInt(-offsetTime.getOffset().getTotalSeconds());
            }
            case TIMESTAMP -> {
                LocalDateTime localDateTime = (LocalDateTime) arg;
                byteBuf.writeLong(-localDateTime.until(baseLocalDateTime, ChronoUnit.MICROS));
            }
            case TIMESTAMPTZ -> {
                OffsetDateTime offsetDateTime = (OffsetDateTime) arg;
                if (offsetDateTime.getOffset() != ZoneOffset.UTC) {
                    byteBuf.writeLong(-offsetDateTime.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime().until(baseLocalDateTime, ChronoUnit.MICROS));
                } else {
                    byteBuf.writeLong(-offsetDateTime.toLocalDateTime().until(baseLocalDateTime, ChronoUnit.MICROS));
                }
            }
            case UUID -> {
                UUID uuid = (UUID) arg;
                byteBuf.writeLong(uuid.getMostSignificantBits());
                byteBuf.writeLong(uuid.getLeastSignificantBits());
            }
            case JSON -> {
                String s = arg == null ? "null" : (String) arg;
                writeString(byteBuf, s);
            }
            case JSONB -> {
                String s = arg == null ? "null" : (String) arg;
                byteBuf.writeByte(1); // 写入版本号
                writeString(byteBuf, s);
            }
        }
    }

    /**
     * 扩展查询从DataRow中读取数据,扩展查询的数据格式为自定义类型
     * 扩展查询时要求postgresql返回数据类型的格式由Bind时进行指定,总是与PgDataType中指定的格式一致
     */
    public static Object readDataFromExtendedQuery(PgDataType dataType, ByteBuf byteBuf, int len) {
        switch (dataType) {
            case BOOL -> {
                if(len != 1) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return byteBuf.readBoolean();
            }
            case INT2 -> {
                if(len != 2) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return byteBuf.readShort();
            }
            case INT4 -> {
                if(len != 4) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return byteBuf.readInt();
            }
            case INT8 -> {
                if(len != 8) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return byteBuf.readLong();
            }
            case CHAR, VARCHAR, TEXT, JSON, JSONB -> {
                return readString(byteBuf, len);
            }
            case FLOAT4 -> {
                if(len != 4) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return byteBuf.readFloat();
            }
            case FLOAT8 -> {
                if(len != 8) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return byteBuf.readDouble();
            }
            case NUMERIC -> {
                return new BigDecimal(readString(byteBuf, len));
            }
            case MONEY -> {
                if(len != 8) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return BigDecimal.valueOf(byteBuf.readLong()).divide(Constants.HUNDRED, 2, RoundingMode.UNNECESSARY);
            }
            case DATE -> {
                if(len != 4) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                int i = byteBuf.readInt();
                return baseLocalDate.plus(i, ChronoUnit.DAYS);
            }
            case TIME -> {
                if(len != 8) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return LocalTime.ofNanoOfDay(byteBuf.readLong() * 1000);
            }
            case TIMETZ -> {
                if(len != 12) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                long day = byteBuf.readLong();
                int seconds = byteBuf.readInt();
                return OffsetTime.of(LocalTime.ofNanoOfDay(day * 1000), ZoneOffset.ofTotalSeconds(seconds));
            }
            case TIMESTAMP -> {
                if(len != 8) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return baseLocalDateTime.plus(byteBuf.readLong(), ChronoUnit.MICROS);
            }
            case TIMESTAMPTZ -> {
                if(len != 8) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                LocalDateTime localDateTime = baseLocalDateTime.plus(byteBuf.readLong(), ChronoUnit.MICROS);
                return OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
            }
            case UUID -> {
                if(len != 16) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                long mostSignificantBytes = byteBuf.readLong();
                long leastSignificantBytes = byteBuf.readLong();
                return new UUID(mostSignificantBytes, leastSignificantBytes);
            }
            default -> throw new FrameworkException(ExceptionType.ORM, Constants.UNSUPPORTED);
        }
    }

    /**
     * 简单查询从DataRow中读取数据,简单查询均为Text类型的数据（指定Fetch Binary除外,该情况会导致解析发生错误）
     */
    public static Object readDataFromSimpleQuery(PgDataType dataType, ByteBuf byteBuf, int len) {
        switch (dataType) {
            case BOOL -> {
                if(len != 1) {
                    throw new FrameworkException(ExceptionType.ORM, PgConstants.ERR_MSG);
                }
                return byteBuf.readByte() == PgConstants.BOOL_TRUE ? Boolean.TRUE : Boolean.FALSE;
            }
            case INT2 -> {
                return Short.valueOf(readString(byteBuf, len));
            }
            case INT4 -> {
                return Integer.valueOf(readString(byteBuf, len));
            }
            case INT8 -> {
                return Long.valueOf(readString(byteBuf, len));
            }
            case CHAR, VARCHAR, TEXT, JSON, JSONB -> {
                return readString(byteBuf, len);
            }
            case FLOAT4 -> {
                return Float.valueOf(readString(byteBuf, len));
            }
            case FLOAT8 -> {
                return Double.valueOf(readString(byteBuf, len));
            }
            case NUMERIC -> {
                return new BigDecimal(readString(byteBuf, len));
            }
            case MONEY -> {
                // 忽视文本中出现的货币符号
                byteBuf.skipBytes(1);
                return new BigDecimal(readString(byteBuf, len - 1));
            }
            case DATE -> {
                return LocalDate.parse(readString(byteBuf, len));
            }
            case TIME -> {
                return LocalTime.parse(readString(byteBuf, len));
            }
            case TIMETZ -> {
                return OffsetTime.parse(readString(byteBuf, len), timetzFormatter);
            }
            case TIMESTAMP -> {
                return LocalDateTime.parse(readString(byteBuf, len), timestampFormatter);
            }
            case TIMESTAMPTZ -> {
                return OffsetDateTime.parse(readString(byteBuf, len), timestamptzFormatter);
            }
            case UUID -> {
                return UUID.fromString(readString(byteBuf, len));
            }
            default -> throw new FrameworkException(ExceptionType.ORM, Constants.UNSUPPORTED);
        }
    }

    /**
     * 解析commandComplete消息中返回的数据条数
     */
    public static int resolveCommandCompleteMsg(String commandCompleteMsg) {
        int index = 0;
        char[] chars = commandCompleteMsg.toCharArray();
        int len = chars.length;
        for (int i = 0; i < len; i++) {
            if (chars[i] == ' ') {
                index = i + 1;
            }
        }
        return Integer.parseInt(commandCompleteMsg.substring(index));
    }

    /**
     *   encode require ssl msg
     */
    public static void encodeSslMsg(WriteBuffer writeBuffer) {
        writeBuffer.writeInt(8);
        writeBuffer.writeInt(PgConstants.SSL_CODE);
    }

}
