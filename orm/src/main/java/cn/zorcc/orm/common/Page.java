package cn.zorcc.orm.common;

import java.util.List;

public record Page<T>(
    Long pageNo,
    Long pageSize,
    Long totalPage,
    Long total,
    List<T> data
) {

}
