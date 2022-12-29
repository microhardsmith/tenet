package cn.zorcc.orm.pg;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

/**
 * pg数据类型对应java中具体类实现
 * 驱动默认支持部分常用数据类型,如果需要使用特殊数据类型则请自行添加处理类
 * 如果数据类型支持binary且binary形式更高效,驱动将会尽可能使用binary模式传输数据,pg在执行SimpleQuery时返回时会默认选择文本类型
 * pg默认对数组的支持是不区分一维数组和多维数组的,在实际使用的情形下很少会使用到数组类型,所以没有实现数组相关的类型匹配
 */
public enum PgDataType {
    BOOL(16, true, Boolean.class),


    INT2(21, true, Short.class),


    INT4(23, true, Integer.class),


    INT8(20, true, Long.class),


    FLOAT4(700, true, Float.class),


    FLOAT8(701, true, Boolean.class),

    /**
     *  NUMERIC类型并不是不能使用二进制进行传输,只是二进制传输的方式转化复杂且数据长度显著高于文本形式,故使用Text方式更为高效
     *  NUMERIC在pg中与DECIMAL等同
     */
    NUMERIC(1700, false, BigDecimal.class),

    /**
     *  货币类型,默认会保留两位小数
     */
    MONEY(790, true, BigDecimal.class),

    /**
     *  固定长度字符串,填充空格
     */
    CHAR(18, true, String.class),

    /**
     *  变长字符串
     */
    VARCHAR(1043, true, String.class),

    /**
     *  不限制长度字符串,等同于未指定长度的VARCHAR
     *  NOTE:三种字符串在pg的实现中并没有明显的差距,尽可能使用text类型,除非需要显式限制字符串长度
     */
    TEXT(25, true, String.class),

    /**
     *  日期,精确至天,占用4字节
     */
    DATE(1082, true, LocalDate.class),

    /**
     *  不带有时区的当日内时间,占用8字节
     */
    TIME(1083, true, LocalTime.class),

    /**
     *  带有时区的当日内时间,占用12字节
     */
    TIMETZ(1266, true, OffsetTime.class),

    /**
     *  不带有时区的时间戳,占用8字节
     */
    TIMESTAMP(1114, true, LocalDateTime.class),

    /**
     *  带有时区的时间戳,占用8字节 NOTE:尽量使用timestamp with out timezone来完成任务
     */
    TIMESTAMPTZ(1184, true, OffsetDateTime.class),

    /**
     *  UUID类型
     */
    UUID(2950, true, UUID.class),

    /**
     *  json类型
     */
    JSON(114, true, String.class),

    /**
     *  以二进制形式存储的json类型,处理时不需要解析,同时支持索引,写入稍慢但检索更快
     */
    JSONB(3802, true, String.class),

    ;
    /**
     * pg数据类型id
     */
    final int oid;
    /**
     * 是否使用二进制传输（所有数据类型均可使用二进制传输,但考虑到解析的复杂度NUMERIC仍使用文本形式传输）
     */
    final boolean usingBinary;
    /**
     * 数据库类型对应Java类型
     */
    final Class<?> javaType;


    PgDataType(int oid, boolean usingBinary, Class<?> javaType) {
        this.oid = oid;
        this.usingBinary = usingBinary;
        this.javaType = javaType;
    }

    public int getOid() {
        return oid;
    }

    public boolean isUsingBinary() {
        return usingBinary;
    }

    public Class<?> getJavaType() {
        return javaType;
    }
}
