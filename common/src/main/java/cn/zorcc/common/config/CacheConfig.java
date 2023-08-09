package cn.zorcc.common.config;

import cn.zorcc.common.Constants;

public class CacheConfig {
    /**
     * 是否启用rocksdb磁盘缓存
     */
    private Boolean enableRocksdbCache = false;
    /**
     * 本地缓存数据文件存放位置,默认将文件存储在jar包所在目录下,文件夹名为local_db,如果文件夹不存在,则新建一个
     */
    private String dataDir = Constants.EMPTY_STRING;

    public CacheConfig() {
    }

    public Boolean getEnableRocksdbCache() {
        return this.enableRocksdbCache;
    }

    public String getDataDir() {
        return this.dataDir;
    }

    public void setEnableRocksdbCache(Boolean enableRocksdbCache) {
        this.enableRocksdbCache = enableRocksdbCache;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CacheConfig)) return false;
        final CacheConfig other = (CacheConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$enableRocksdbCache = this.getEnableRocksdbCache();
        final Object other$enableRocksdbCache = other.getEnableRocksdbCache();
        if (this$enableRocksdbCache == null ? other$enableRocksdbCache != null : !this$enableRocksdbCache.equals(other$enableRocksdbCache))
            return false;
        final Object this$dataDir = this.getDataDir();
        final Object other$dataDir = other.getDataDir();
        if (this$dataDir == null ? other$dataDir != null : !this$dataDir.equals(other$dataDir)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof CacheConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $enableRocksdbCache = this.getEnableRocksdbCache();
        result = result * PRIME + ($enableRocksdbCache == null ? 43 : $enableRocksdbCache.hashCode());
        final Object $dataDir = this.getDataDir();
        result = result * PRIME + ($dataDir == null ? 43 : $dataDir.hashCode());
        return result;
    }

    public String toString() {
        return "CacheConfig(enableRocksdbCache=" + this.getEnableRocksdbCache() + ", dataDir=" + this.getDataDir() + ")";
    }
}
