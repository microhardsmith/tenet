package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import lombok.Data;

/**
 *  Configuration of net
 */
@Data
public class NetworkConfig {
    /**
     *  是否启用Server,如果不启用Server则为纯客户端应用
     *  Note: 无论如何Dispatcher都会被启用，在客户端应用下Dispatcher只负责监听connect成功，在服务端应用下还负责监听accept连接
     */
    private Boolean launchServer = Boolean.TRUE;
    /**
     *  worker线程数量，默认为4
     */
    private Integer workerCount = 4;
    /**
     *  是否可复用端口,默认为true
     */
    private Boolean reuseAddr = Boolean.TRUE;
    /**
     *  是否周期性发送保活报文以维持连接
     */
    private Boolean keepAlive = Boolean.FALSE;
    /**
     *  是否关闭Nagle算法
     */
    private Boolean tcpNoDelay = Boolean.TRUE;
    /**
     *  服务器ip地址, note: 目前只支持ipv4版本协议栈，使用localhost访问时可能会定向到ipv6，尽量使用0.0.0.0或127.0.0.1访问
     */
    private String ip = "0.0.0.0";
    /**
     *  服务器端口号
     */
    private Integer port = 8001;
    /**
     *  等待接受全连接socket队列长度
     */
    private Integer backlog = 128;
    /**
     *  单次epoll获取事件数组长度
     */
    private Integer maxEvents = 256;
    /**
     *  单次分配读内存块大小,每次轮询到事件后都会读取至多该值的内存数据
     */
    private Integer readBufferSize = 16 * Constants.KB;
    /**
     *  单次分配写内存块大小,该值只代表WriteBuffer初始大小,在后续写过程中会逐渐扩容
     */
    private Integer writeBufferSize = 4 * Constants.KB;
    /**
     *  socket map 初始大小
     */
    private Integer mapSize = 1024;
    /**
     *  worker buf queue size
     */
    private Integer queueSize = 256;
}
