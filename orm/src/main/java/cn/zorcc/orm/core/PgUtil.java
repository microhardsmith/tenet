package cn.zorcc.orm.core;


import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

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
     *   Converting human-readable hexadecimal data into UTF-8 string
     */
    public static String toHexString(byte[] data) {
        byte[] hex = new byte[data.length << 1];
        int offset = 0;
        for (byte b : data) {
            int v = b & 0xFF;
            hex[offset++] = hexArray[v >> 4];
            hex[offset++] = hexArray[v & 0xF];
        }
        return new String(hex, StandardCharsets.UTF_8);
    }

}
