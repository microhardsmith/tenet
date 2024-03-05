package cn.zorcc.common.postgre;

import cn.zorcc.common.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.lang.foreign.MemorySegment;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record PgDataType(
        String typeName,
        int oid,
        boolean binary,
        Class<?> javaType
) {
    private static final int BOOL_OID = 16;
    private static final int INT2_OID = 21;
    private static final int INT4_OID = 23;
    private static final int INT8_OID = 20;
    private static final int FLOAT4_OID = 700;
    private static final int FLOAT8_OID = 701;
    private static final int NUMERIC_OID = 1700;
    private static final int MONEY_OID = 790;
    private static final int CHAR_OID = 18;
    private static final int VARCHAR_OID = 1043;
    private static final int TEXT_OID = 25;
    private static final int DATE_OID = 1082;
    private static final int TIME_OID = 1083;
    private static final int TIMETZ_OID = 1266;
    private static final int TIMESTAMP_OID = 1114;
    private static final int TIMESTAMPTZ_OID = 1184;
    private static final int UUID_OID = 2950;
    private static final int JSON_OID = 114;
    private static final int JSONB_OID = 3802;
    private static final PgDataType BOOL = new PgDataType("bool", BOOL_OID, true, Boolean.class);
    private static final PgDataType INT2 = new PgDataType("int2", INT2_OID, true, Short.class);
    private static final PgDataType INT4 = new PgDataType("int4", INT4_OID, true, Integer.class);
    private static final PgDataType INT8 = new PgDataType("int8", INT8_OID, true, Long.class);
    private static final PgDataType FLOAT4 = new PgDataType("float4", FLOAT4_OID, true, Float.class);
    private static final PgDataType FLOAT8 = new PgDataType("float8", FLOAT8_OID, true, Double.class);
    private static final PgDataType NUMERIC = new PgDataType("numeric", NUMERIC_OID, true, BigDecimal.class);
    private static final PgDataType MONEY = new PgDataType("money", MONEY_OID, true, BigDecimal.class);
    private static final PgDataType CHAR = new PgDataType("char", CHAR_OID, true, String.class);
    private static final PgDataType VARCHAR = new PgDataType("varchar", VARCHAR_OID, true, String.class);
    private static final PgDataType TEXT = new PgDataType("text", TEXT_OID, true, String.class);
    private static final PgDataType DATE = new PgDataType("date", DATE_OID, true, LocalDate.class);
    private static final PgDataType TIME = new PgDataType("time", TIME_OID, true, LocalTime.class);
    private static final PgDataType TIMETZ = new PgDataType("timetz", TIMETZ_OID, true, OffsetTime.class);
    private static final PgDataType TIMESTAMP = new PgDataType("timestamp", TIMESTAMP_OID, true, LocalDateTime.class);
    private static final PgDataType TIMESTAMPTZ = new PgDataType("timestamptz", TIMESTAMPTZ_OID, true, OffsetDateTime.class);
    private static final PgDataType UUID = new PgDataType("uuid", UUID_OID, true, UUID.class);
    private static final PgDataType JSON = new PgDataType("json", JSON_OID, true, MemorySegment.class);
    private static final PgDataType JSONB = new PgDataType("jsonb", JSONB_OID, true, MemorySegment.class);

    public static PgDataType of(int oid) {
        return switch (oid) {
            case BOOL_OID -> BOOL;
            case INT2_OID -> INT2;
            case INT4_OID -> INT4;
            case INT8_OID -> INT8;
            case FLOAT4_OID -> FLOAT4;
            case FLOAT8_OID -> FLOAT8;
            case NUMERIC_OID -> NUMERIC;
            case MONEY_OID -> MONEY;
            case CHAR_OID -> CHAR;
            case VARCHAR_OID -> VARCHAR;
            case TEXT_OID -> TEXT;
            case DATE_OID -> DATE;
            case TIME_OID -> TIME;
            case TIMETZ_OID -> TIMETZ;
            case TIMESTAMP_OID -> TIMESTAMP;
            case TIMESTAMPTZ_OID -> TIMESTAMPTZ;
            case UUID_OID -> UUID;
            case JSON_OID -> JSON;
            case JSONB_OID -> JSONB;
            default -> throw new FrameworkException(ExceptionType.POSTGRESQL, "Unsupported data type");
        };
    }
}
