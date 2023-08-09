package cn.zorcc.common.config;

import cn.zorcc.common.Constants;
import cn.zorcc.common.pojo.Peer;

import java.util.ArrayList;
import java.util.List;

/**
 * 集群以及本地的网络配置
 */
public class ClusterConfig {
    /**
     * 是否使用集群部署
     */
    private boolean enabled = false;
    /**
     * 集群中其他节点信息
     */
    private List<Peer> peers = new ArrayList<>();
    /**
     * raft阻塞队列长度 TODO 限制写操作的流速避免阻塞队列爆掉
     */
    private int queueSize = 1024;
    /**
     * 是否作为配置变更加入集群,即集群中的其他节点是否事先了解此信息
     */
    private boolean membershipChange = false;
    /**
     * 集群心跳间隔,单位毫秒
     */
    private int heartBeatTimeout = 100;
    /**
     * 接受心跳超时间隔,单位毫秒,必须比heartBeatTimeout大
     */
    private int heartBeatReceiveTimeout = 125;
    /**
     * 集群选举超时时间,单位毫秒,实际等待超时时间为 electionTimeout << 1 ~ electionTimeout之间的随机值
     */
    private int electionTimeout = 500;
    /**
     * 允许接受心跳超时事件的数量,follower会在心跳超时次数达到该值时,随机等待 electionTimeout << 1 ~ electionTimeout时间后发起预选举
     */
    private int allowHeartBeatTimeoutCount = 3;
    /**
     * 消息发送的超时时间,单位毫秒,默认发送消息25ms后仍未收到回复则认为消息丢失
     */
    private long msgTimeout = 25L;
    /**
     * 读写最大超时重试次数
     */
    private int maxRetryTimes = 2;
    /**
     * 超时时重试等待时间,单位毫秒
     */
    private long retryWaitTime = 200;
    /**
     * raft集群元文件存放的位置,默认存放到当前jar包运行目录下
     */
    private String metaDir = Constants.EMPTY_STRING;
    /**
     * raft集群数据文件存放的位置,默认存放到当前jar包运行目录下
     */
    private String dataDir = Constants.EMPTY_STRING;
    /**
     * 分布式缓存备份文件存放位置,默认将文件存储在jar包所在目录下,文件夹名为cluster_backup,如果文件夹不存在,则新建一个
     */
    private String backupDir = Constants.EMPTY_STRING;
    /**
     * raft临时文件存放位置,默认存放到临时文件夹下
     */
    private String tempDir = Constants.EMPTY_STRING;
    /**
     * 日志压缩启用时机,在写入超过该次数后将日志压缩,默认值为10000
     * 如果所有节点的matchIndex未均达到该值,则不允许进行压缩
     */
    private int compressionCount = 10000;
    /**
     * 内存中加载的最多的日志数量,默认为4w条
     */
    private int maxInMemoryLogSize = 40000;
    /**
     * 集群重试连接间隔,节点在无法连接其他节点时会每隔该间隔再次发起连接,单位毫秒
     */
    private int retryInterval = 5000;

    public ClusterConfig() {
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public List<Peer> getPeers() {
        return this.peers;
    }

    public int getQueueSize() {
        return this.queueSize;
    }

    public boolean isMembershipChange() {
        return this.membershipChange;
    }

    public int getHeartBeatTimeout() {
        return this.heartBeatTimeout;
    }

    public int getHeartBeatReceiveTimeout() {
        return this.heartBeatReceiveTimeout;
    }

    public int getElectionTimeout() {
        return this.electionTimeout;
    }

    public int getAllowHeartBeatTimeoutCount() {
        return this.allowHeartBeatTimeoutCount;
    }

    public long getMsgTimeout() {
        return this.msgTimeout;
    }

    public int getMaxRetryTimes() {
        return this.maxRetryTimes;
    }

    public long getRetryWaitTime() {
        return this.retryWaitTime;
    }

    public String getMetaDir() {
        return this.metaDir;
    }

    public String getDataDir() {
        return this.dataDir;
    }

    public String getBackupDir() {
        return this.backupDir;
    }

    public String getTempDir() {
        return this.tempDir;
    }

    public int getCompressionCount() {
        return this.compressionCount;
    }

    public int getMaxInMemoryLogSize() {
        return this.maxInMemoryLogSize;
    }

    public int getRetryInterval() {
        return this.retryInterval;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPeers(List<Peer> peers) {
        this.peers = peers;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setMembershipChange(boolean membershipChange) {
        this.membershipChange = membershipChange;
    }

    public void setHeartBeatTimeout(int heartBeatTimeout) {
        this.heartBeatTimeout = heartBeatTimeout;
    }

    public void setHeartBeatReceiveTimeout(int heartBeatReceiveTimeout) {
        this.heartBeatReceiveTimeout = heartBeatReceiveTimeout;
    }

    public void setElectionTimeout(int electionTimeout) {
        this.electionTimeout = electionTimeout;
    }

    public void setAllowHeartBeatTimeoutCount(int allowHeartBeatTimeoutCount) {
        this.allowHeartBeatTimeoutCount = allowHeartBeatTimeoutCount;
    }

    public void setMsgTimeout(long msgTimeout) {
        this.msgTimeout = msgTimeout;
    }

    public void setMaxRetryTimes(int maxRetryTimes) {
        this.maxRetryTimes = maxRetryTimes;
    }

    public void setRetryWaitTime(long retryWaitTime) {
        this.retryWaitTime = retryWaitTime;
    }

    public void setMetaDir(String metaDir) {
        this.metaDir = metaDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public void setCompressionCount(int compressionCount) {
        this.compressionCount = compressionCount;
    }

    public void setMaxInMemoryLogSize(int maxInMemoryLogSize) {
        this.maxInMemoryLogSize = maxInMemoryLogSize;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ClusterConfig)) return false;
        final ClusterConfig other = (ClusterConfig) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isEnabled() != other.isEnabled()) return false;
        final Object this$peers = this.getPeers();
        final Object other$peers = other.getPeers();
        if (this$peers == null ? other$peers != null : !this$peers.equals(other$peers)) return false;
        if (this.getQueueSize() != other.getQueueSize()) return false;
        if (this.isMembershipChange() != other.isMembershipChange()) return false;
        if (this.getHeartBeatTimeout() != other.getHeartBeatTimeout()) return false;
        if (this.getHeartBeatReceiveTimeout() != other.getHeartBeatReceiveTimeout()) return false;
        if (this.getElectionTimeout() != other.getElectionTimeout()) return false;
        if (this.getAllowHeartBeatTimeoutCount() != other.getAllowHeartBeatTimeoutCount()) return false;
        if (this.getMsgTimeout() != other.getMsgTimeout()) return false;
        if (this.getMaxRetryTimes() != other.getMaxRetryTimes()) return false;
        if (this.getRetryWaitTime() != other.getRetryWaitTime()) return false;
        final Object this$metaDir = this.getMetaDir();
        final Object other$metaDir = other.getMetaDir();
        if (this$metaDir == null ? other$metaDir != null : !this$metaDir.equals(other$metaDir)) return false;
        final Object this$dataDir = this.getDataDir();
        final Object other$dataDir = other.getDataDir();
        if (this$dataDir == null ? other$dataDir != null : !this$dataDir.equals(other$dataDir)) return false;
        final Object this$backupDir = this.getBackupDir();
        final Object other$backupDir = other.getBackupDir();
        if (this$backupDir == null ? other$backupDir != null : !this$backupDir.equals(other$backupDir)) return false;
        final Object this$tempDir = this.getTempDir();
        final Object other$tempDir = other.getTempDir();
        if (this$tempDir == null ? other$tempDir != null : !this$tempDir.equals(other$tempDir)) return false;
        if (this.getCompressionCount() != other.getCompressionCount()) return false;
        if (this.getMaxInMemoryLogSize() != other.getMaxInMemoryLogSize()) return false;
        if (this.getRetryInterval() != other.getRetryInterval()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ClusterConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isEnabled() ? 79 : 97);
        final Object $peers = this.getPeers();
        result = result * PRIME + ($peers == null ? 43 : $peers.hashCode());
        result = result * PRIME + this.getQueueSize();
        result = result * PRIME + (this.isMembershipChange() ? 79 : 97);
        result = result * PRIME + this.getHeartBeatTimeout();
        result = result * PRIME + this.getHeartBeatReceiveTimeout();
        result = result * PRIME + this.getElectionTimeout();
        result = result * PRIME + this.getAllowHeartBeatTimeoutCount();
        final long $msgTimeout = this.getMsgTimeout();
        result = result * PRIME + (int) ($msgTimeout >>> 32 ^ $msgTimeout);
        result = result * PRIME + this.getMaxRetryTimes();
        final long $retryWaitTime = this.getRetryWaitTime();
        result = result * PRIME + (int) ($retryWaitTime >>> 32 ^ $retryWaitTime);
        final Object $metaDir = this.getMetaDir();
        result = result * PRIME + ($metaDir == null ? 43 : $metaDir.hashCode());
        final Object $dataDir = this.getDataDir();
        result = result * PRIME + ($dataDir == null ? 43 : $dataDir.hashCode());
        final Object $backupDir = this.getBackupDir();
        result = result * PRIME + ($backupDir == null ? 43 : $backupDir.hashCode());
        final Object $tempDir = this.getTempDir();
        result = result * PRIME + ($tempDir == null ? 43 : $tempDir.hashCode());
        result = result * PRIME + this.getCompressionCount();
        result = result * PRIME + this.getMaxInMemoryLogSize();
        result = result * PRIME + this.getRetryInterval();
        return result;
    }

    public String toString() {
        return "ClusterConfig(enabled=" + this.isEnabled() + ", peers=" + this.getPeers() + ", queueSize=" + this.getQueueSize() + ", membershipChange=" + this.isMembershipChange() + ", heartBeatTimeout=" + this.getHeartBeatTimeout() + ", heartBeatReceiveTimeout=" + this.getHeartBeatReceiveTimeout() + ", electionTimeout=" + this.getElectionTimeout() + ", allowHeartBeatTimeoutCount=" + this.getAllowHeartBeatTimeoutCount() + ", msgTimeout=" + this.getMsgTimeout() + ", maxRetryTimes=" + this.getMaxRetryTimes() + ", retryWaitTime=" + this.getRetryWaitTime() + ", metaDir=" + this.getMetaDir() + ", dataDir=" + this.getDataDir() + ", backupDir=" + this.getBackupDir() + ", tempDir=" + this.getTempDir() + ", compressionCount=" + this.getCompressionCount() + ", maxInMemoryLogSize=" + this.getMaxInMemoryLogSize() + ", retryInterval=" + this.getRetryInterval() + ")";
    }
}
