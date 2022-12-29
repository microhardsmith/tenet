package cn.zorcc.common.config;

import cn.zorcc.common.Constants;
import lombok.Data;

@Data
public class CacheConfig {
    /**
     * 是否启用rocksdb磁盘缓存
     */
    private Boolean enableRocksdbCache = false;
    /**
     * 本地缓存数据文件存放位置,默认将文件存储在jar包所在目录下,文件夹名为local_db,如果文件夹不存在,则新建一个
     */
    private String dataDir = Constants.EMPTY_STRING;
}
