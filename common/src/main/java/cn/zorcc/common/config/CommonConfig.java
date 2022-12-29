package cn.zorcc.common.config;

import cn.zorcc.common.Constants;
import cn.zorcc.common.pojo.Peer;
import lombok.Getter;
import lombok.Setter;

/**
 * 节点通用配置
 */
@Getter
@Setter
public class CommonConfig {
    /**
     * rpc worker线程数,默认4个（该值不建议设置的过大,实际耗时任务会在业务线程池中处理）
     */
    private Integer rpcWorkerThreads = 4;
    /**
     * rpc调用默认超时时间,单位毫秒(在gateway中表现为http超时时间)
     */
    private Integer rpcTtl = 30000;
    /**
     * rpc认证超时时间,单位毫秒
     */
    private Integer rpcAuthTtl = 1000;
    /**
     *  心跳超时检测时长,单位毫秒
     */
    private Integer heartBeatInterval = 60000;
    /**
     * rpc高水位线,默认设置为16MB
     */
    private Integer highWaterMark = 16 * Constants.MB;
    /**
     * rpc低水位线,默认设置为8MB
     */
    private Integer lowWaterMark = 8 * Constants.MB;
    /**
     * 当channel不可写时,限制channel每秒传输的Bytes量实现背压
     */
    private Integer writeLimit = Constants.MB;
    /**
     * 三次握手的请求的队列的最大长度
     */
    private Integer backLogSize = 128;
    /**
     * 当前节点信息,无论是否采用集群部署,都应该配置该值
     */
    private Peer self;
    /**
     * 时间轮槽位,更大的时间轮会使每个槽位的任务链表的长度更短,提高执行效率,但也会消耗更多内存
     */
    private Integer timeWheelSlots = 1024;
    /**
     * 时间轮tick大小,单位毫秒,默认精度为25ms
     * 如果启用了集群,时间轮精度需要满足raft算法的需求,单机情况下可适当放大该值
     */
    private Long timeWheelTick = 25L;
    /**
     * 时间轮界限值,超过该值的任务会被添加至等待队列,单位毫秒
     */
    private Long timerWheelBoundary = 10000L;
}
