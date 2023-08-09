package cn.zorcc.common.config;

import cn.zorcc.common.Constants;
import cn.zorcc.common.pojo.Peer;

/**
 * 节点通用配置
 */

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
}
