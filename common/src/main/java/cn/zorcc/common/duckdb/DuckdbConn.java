package cn.zorcc.common.duckdb;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.Map;

/**
 *   DuckDB connection implementation
 */
public final class DuckdbConn {

    public DuckdbConn(DuckdbConfig config) {
        String path = config.getPath();
        if(path == null || path.isBlank()) {
            throw new FrameworkException(ExceptionType.DUCKDB, "Empty path detected");
        }
        Map<String, String> options = config.getOptions();
        if(options == null || options.isEmpty()) {

        }else {

        }
    }
}
