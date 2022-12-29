package cn.zorcc.common.config;

import cn.zorcc.common.Constants;
import cn.zorcc.common.pojo.Peer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 集群以及本地的网络配置
 */
@Data
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
     *  raft阻塞队列长度 TODO 限制写操作的流速避免阻塞队列爆掉
     */
    private int queueSize =  1024;
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
     * raft临时文件存放位置,默认存放到/temp/lithiasis文件夹下
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
}
