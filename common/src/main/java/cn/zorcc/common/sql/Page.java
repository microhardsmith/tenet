package cn.zorcc.common.sql;

import java.util.List;

public record Page<T>(
    Long pageNo,
    Long pageSize,
    Long totalPage,
    Long total,
    List<T> data
) {

}
