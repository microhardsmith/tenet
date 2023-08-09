package cn.zorcc.gateway.config;

import cn.zorcc.common.config.CommonConfig;
import cn.zorcc.common.config.ToMintConfig;
import cn.zorcc.http.HttpConfig;

/**
 * 网关配置文件
 */
public class GatewayConfig {
    /**
     * 网关唯一id,不同网关之间配置不同项,用于生成traceId,取值范围为0 ~ 63
     */
    private Integer uniqueId;
    /**
     * 通用配置文件
     */
    private CommonConfig common = new CommonConfig();
    /**
     * http配置文件
     */
    private HttpConfig http = new HttpConfig();
    /**
     * gateway连接mint配置
     */
    private ToMintConfig toMint = new ToMintConfig();

    public GatewayConfig() {
    }

    public Integer getUniqueId() {
        return this.uniqueId;
    }

    public CommonConfig getCommon() {
        return this.common;
    }

    public HttpConfig getHttp() {
        return this.http;
    }

    public ToMintConfig getToMint() {
        return this.toMint;
    }

    public void setUniqueId(Integer uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setCommon(CommonConfig common) {
        this.common = common;
    }

    public void setHttp(HttpConfig http) {
        this.http = http;
    }

    public void setToMint(ToMintConfig toMint) {
        this.toMint = toMint;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof GatewayConfig)) return false;
        final GatewayConfig other = (GatewayConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$common = this.getCommon();
        final Object other$common = other.getCommon();
        if (this$common == null ? other$common != null : !this$common.equals(other$common)) return false;
        final Object this$http = this.getHttp();
        final Object other$http = other.getHttp();
        if (this$http == null ? other$http != null : !this$http.equals(other$http)) return false;
        final Object this$toMint = this.getToMint();
        final Object other$toMint = other.getToMint();
        if (this$toMint == null ? other$toMint != null : !this$toMint.equals(other$toMint)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof GatewayConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $common = this.getCommon();
        result = result * PRIME + ($common == null ? 43 : $common.hashCode());
        final Object $http = this.getHttp();
        result = result * PRIME + ($http == null ? 43 : $http.hashCode());
        final Object $toMint = this.getToMint();
        result = result * PRIME + ($toMint == null ? 43 : $toMint.hashCode());
        return result;
    }

    public String toString() {
        return "GatewayConfig(uniqueId=" + this.getUniqueId() + ", common=" + this.getCommon() + ", http=" + this.getHttp() + ", toMint=" + this.getToMint() + ")";
    }
}
