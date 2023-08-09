package cn.zorcc.mint;

import cn.zorcc.common.config.CacheConfig;
import cn.zorcc.common.config.ClusterConfig;
import cn.zorcc.common.config.CommonConfig;

public class MintConfig {
    /**
     * jwt秘钥
     */
    private String secret = "blVm4EPe55kpAcZz";
    /**
     * 默认登录的用户名
     */
    private String rootUserName = "root";
    /**
     * 默认登录密码
     */
    private String rootPassword = "blVm4EPe55kpAcZz";
    /**
     * 通用配置文件
     */
    private CommonConfig common = new CommonConfig();
    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();
    /**
     * 集群配置
     */
    private ClusterConfig cluster = new ClusterConfig();

    public MintConfig() {
    }

    public String getSecret() {
        return this.secret;
    }

    public String getRootUserName() {
        return this.rootUserName;
    }

    public String getRootPassword() {
        return this.rootPassword;
    }

    public CommonConfig getCommon() {
        return this.common;
    }

    public CacheConfig getCache() {
        return this.cache;
    }

    public ClusterConfig getCluster() {
        return this.cluster;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setRootUserName(String rootUserName) {
        this.rootUserName = rootUserName;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    public void setCommon(CommonConfig common) {
        this.common = common;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    public void setCluster(ClusterConfig cluster) {
        this.cluster = cluster;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof MintConfig)) return false;
        final MintConfig other = (MintConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$secret = this.getSecret();
        final Object other$secret = other.getSecret();
        if (this$secret == null ? other$secret != null : !this$secret.equals(other$secret)) return false;
        final Object this$rootUserName = this.getRootUserName();
        final Object other$rootUserName = other.getRootUserName();
        if (this$rootUserName == null ? other$rootUserName != null : !this$rootUserName.equals(other$rootUserName))
            return false;
        final Object this$rootPassword = this.getRootPassword();
        final Object other$rootPassword = other.getRootPassword();
        if (this$rootPassword == null ? other$rootPassword != null : !this$rootPassword.equals(other$rootPassword))
            return false;
        final Object this$common = this.getCommon();
        final Object other$common = other.getCommon();
        if (this$common == null ? other$common != null : !this$common.equals(other$common)) return false;
        final Object this$cache = this.getCache();
        final Object other$cache = other.getCache();
        if (this$cache == null ? other$cache != null : !this$cache.equals(other$cache)) return false;
        final Object this$cluster = this.getCluster();
        final Object other$cluster = other.getCluster();
        if (this$cluster == null ? other$cluster != null : !this$cluster.equals(other$cluster)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof MintConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $secret = this.getSecret();
        result = result * PRIME + ($secret == null ? 43 : $secret.hashCode());
        final Object $rootUserName = this.getRootUserName();
        result = result * PRIME + ($rootUserName == null ? 43 : $rootUserName.hashCode());
        final Object $rootPassword = this.getRootPassword();
        result = result * PRIME + ($rootPassword == null ? 43 : $rootPassword.hashCode());
        final Object $common = this.getCommon();
        result = result * PRIME + ($common == null ? 43 : $common.hashCode());
        final Object $cache = this.getCache();
        result = result * PRIME + ($cache == null ? 43 : $cache.hashCode());
        final Object $cluster = this.getCluster();
        result = result * PRIME + ($cluster == null ? 43 : $cluster.hashCode());
        return result;
    }

    public String toString() {
        return "MintConfig(secret=" + this.getSecret() + ", rootUserName=" + this.getRootUserName() + ", rootPassword=" + this.getRootPassword() + ", common=" + this.getCommon() + ", cache=" + this.getCache() + ", cluster=" + this.getCluster() + ")";
    }
}
