package cn.zorcc.orm.pg;

import lombok.Getter;
import lombok.Setter;

/**
 * postgresql服务器返回的列描述字段
 */
@Getter
@Setter
public class PgRowDescription {
    /**
     * 列名称
     */
    private String name;
    /**
     * 列索引,如果存在多个列则索引从1开始逐渐递增,否则为0
     */
    private Integer index;
    /**
     * 列类型id
     */
    private Integer oid;
    /**
     * 列数据类型,false表示为Text类型,true表示为Binary类型
     * SimpleQuery基本上返回的总是Text类型的数据,ExtendedQuery返回数据类型会在Bind中覆盖
     */
    private Boolean format;
    /**
     * 映射对象相关信息
     */
    // private PoMetaInfo poMetaInfo;
    /**
     * pg数据类型
     */
    private PgDataType dataType;
}
