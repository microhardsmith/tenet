package cn.zorcc.common.database;

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
