package cn.zorcc.common.postgre;

import cn.zorcc.common.structure.IntMap;

import java.lang.foreign.MemorySegment;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.util.stream.Stream;

public record PgDataType(
        String typeName,
        int oid,
        boolean binary,
        Class<?> javaType
) {
    /**
     *  256 is kind of an ideal size for current supported data-types without hash collision
     */
    private static final IntMap<PgDataType> pgDataTypeMap = new IntMap<>(256);

    static {
        Stream.of(
                new PgDataType("bool", 16, true, Boolean.class),
                new PgDataType("int2", 21, true, Short.class),
                new PgDataType("int4", 23, true, Integer.class),
                new PgDataType("int8", 20, true, Float.class),
                new PgDataType("float4", 700, true, Float.class),
                new PgDataType("float8", 701, true, Double.class),
                new PgDataType("numeric", 1700, true, BigDecimal.class),
                new PgDataType("money", 790, true, BigDecimal.class),
                new PgDataType("char", 18, true, String.class),
                new PgDataType("varchar", 1043, true, String.class),
                new PgDataType("text", 25, true, String.class),
                new PgDataType("date", 1082, true, LocalDate.class),
                new PgDataType("time", 1083, true, LocalTime.class),
                new PgDataType("timetz", 1266, true, OffsetTime.class),
                new PgDataType("timestamp", 1114, true, LocalDateTime.class),
                new PgDataType("timestamptz", 1184, true, OffsetDateTime.class),
                new PgDataType("uuid", 2950, true, UUID.class),
                new PgDataType("json", 114, true, MemorySegment.class),
                new PgDataType("jsonb", 3802, true, MemorySegment.class)
        ).forEach(pgDataType -> pgDataTypeMap.put(pgDataType.oid(), pgDataType));
    }

    public static PgDataType of(int oid) {
        return pgDataTypeMap.get(oid);
    }
}
