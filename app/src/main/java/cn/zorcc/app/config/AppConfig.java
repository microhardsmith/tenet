package cn.zorcc.app.config;

import cn.zorcc.common.config.CacheConfig;
import cn.zorcc.common.config.ClusterConfig;
import cn.zorcc.common.config.CommonConfig;
import cn.zorcc.common.config.ToMintConfig;
import cn.zorcc.orm.PgConfig;

/**
 * 微服务配置文件加载类
 */
public class AppConfig {
    /**
     * 通用配置文件
     */
    private CommonConfig common = new CommonConfig();
    /**
     * 数据库配置
     */
    private PgConfig database = new PgConfig();
    /**
     * app连接mint配置
     */
    private ToMintConfig mint = new ToMintConfig();
    /**
     * app缓存配置
     */
    private CacheConfig cache = new CacheConfig();
    /**
     * 集群配置
     */
    private ClusterConfig cluster = new ClusterConfig();

    public AppConfig() {
    }

    public CommonConfig getCommon() {
        return this.common;
    }

    public PgConfig getDatabase() {
        return this.database;
    }

    public ToMintConfig getMint() {
        return this.mint;
    }

    public CacheConfig getCache() {
        return this.cache;
    }

    public ClusterConfig getCluster() {
        return this.cluster;
    }

    public void setCommon(CommonConfig common) {
        this.common = common;
    }

    public void setDatabase(PgConfig database) {
        this.database = database;
    }

    public void setMint(ToMintConfig mint) {
        this.mint = mint;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    public void setCluster(ClusterConfig cluster) {
        this.cluster = cluster;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof AppConfig)) return false;
        final AppConfig other = (AppConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$common = this.getCommon();
        final Object other$common = other.getCommon();
        if (this$common == null ? other$common != null : !this$common.equals(other$common)) return false;
        final Object this$database = this.getDatabase();
        final Object other$database = other.getDatabase();
        if (this$database == null ? other$database != null : !this$database.equals(other$database)) return false;
        final Object this$mint = this.getMint();
        final Object other$mint = other.getMint();
        if (this$mint == null ? other$mint != null : !this$mint.equals(other$mint)) return false;
        final Object this$cache = this.getCache();
        final Object other$cache = other.getCache();
        if (this$cache == null ? other$cache != null : !this$cache.equals(other$cache)) return false;
        final Object this$cluster = this.getCluster();
        final Object other$cluster = other.getCluster();
        if (this$cluster == null ? other$cluster != null : !this$cluster.equals(other$cluster)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof AppConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $common = this.getCommon();
        result = result * PRIME + ($common == null ? 43 : $common.hashCode());
        final Object $database = this.getDatabase();
        result = result * PRIME + ($database == null ? 43 : $database.hashCode());
        final Object $mint = this.getMint();
        result = result * PRIME + ($mint == null ? 43 : $mint.hashCode());
        final Object $cache = this.getCache();
        result = result * PRIME + ($cache == null ? 43 : $cache.hashCode());
        final Object $cluster = this.getCluster();
        result = result * PRIME + ($cluster == null ? 43 : $cluster.hashCode());
        return result;
    }

    public String toString() {
        return "AppConfig(common=" + this.getCommon() + ", database=" + this.getDatabase() + ", mint=" + this.getMint() + ", cache=" + this.getCache() + ", cluster=" + this.getCluster() + ")";
    }
}
