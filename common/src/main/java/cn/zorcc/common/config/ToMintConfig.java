package cn.zorcc.common.config;

import cn.zorcc.common.pojo.Peer;

import java.util.List;

/**
 * 微服务或网关连接mint的配置
 */
public class ToMintConfig {
    /**
     * 重试连接Mint间隔,单位毫秒
     */
    private Integer retryInterval = 5000;
    /**
     * 主动拉取信息间隔,单位毫秒,mint在发生变动时会主动推送消息,该值可尽量大,设置为负数可禁用该功能
     */
    private Integer fetchInterval = 30000;
    /**
     * mint服务器地址
     */
    private List<Peer> mintList;

    public ToMintConfig() {
    }

    public Integer getRetryInterval() {
        return this.retryInterval;
    }

    public Integer getFetchInterval() {
        return this.fetchInterval;
    }

    public List<Peer> getMintList() {
        return this.mintList;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public void setFetchInterval(Integer fetchInterval) {
        this.fetchInterval = fetchInterval;
    }

    public void setMintList(List<Peer> mintList) {
        this.mintList = mintList;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ToMintConfig)) return false;
        final ToMintConfig other = (ToMintConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$retryInterval = this.getRetryInterval();
        final Object other$retryInterval = other.getRetryInterval();
        if (this$retryInterval == null ? other$retryInterval != null : !this$retryInterval.equals(other$retryInterval))
            return false;
        final Object this$fetchInterval = this.getFetchInterval();
        final Object other$fetchInterval = other.getFetchInterval();
        if (this$fetchInterval == null ? other$fetchInterval != null : !this$fetchInterval.equals(other$fetchInterval))
            return false;
        final Object this$mintList = this.getMintList();
        final Object other$mintList = other.getMintList();
        if (this$mintList == null ? other$mintList != null : !this$mintList.equals(other$mintList)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ToMintConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $retryInterval = this.getRetryInterval();
        result = result * PRIME + ($retryInterval == null ? 43 : $retryInterval.hashCode());
        final Object $fetchInterval = this.getFetchInterval();
        result = result * PRIME + ($fetchInterval == null ? 43 : $fetchInterval.hashCode());
        final Object $mintList = this.getMintList();
        result = result * PRIME + ($mintList == null ? 43 : $mintList.hashCode());
        return result;
    }

    public String toString() {
        return "ToMintConfig(retryInterval=" + this.getRetryInterval() + ", fetchInterval=" + this.getFetchInterval() + ", mintList=" + this.getMintList() + ")";
    }
}
