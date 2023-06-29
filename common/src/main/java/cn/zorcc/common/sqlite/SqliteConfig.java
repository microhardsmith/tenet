package cn.zorcc.common.sqlite;

import lombok.Data;

@Data
public class SqliteConfig {
    /**
     *   The absolute path of the Sqlite database file
     */
    private String path;
    /**
     *   Whether or not using WAL mechanism, if using cluster-mode, WAL is encouraged to be enabled
     */
    private Boolean enableWAL;
}
