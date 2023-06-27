package cn.zorcc.common.storage;

import lombok.Data;

@Data
public class SqliteConfig {
    /**
     *   The absolute path of the Sqlite database file
     */
    private String path;
}
