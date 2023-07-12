package cn.zorcc.common.sql;

import java.util.List;

/**
 *   Page record from database
 */
public record Page<T>(
    long pageNo,
    long pageSize,
    long totalPage,
    long total,
    List<T> data
) {

}
