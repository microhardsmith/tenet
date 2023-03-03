package cn.zorcc.common.net;

import cn.zorcc.common.Constants;
import lombok.Data;

/**
 *  Configuration of net
 */
@Data
public class NetConfig {
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
     *  服务器ip地址
     */
    private String ip = "127.0.0.1";
    /**
     *  服务器端口号
     */
    private Integer port = 10705;
    /**
     *  等待接受全连接socket队列长度
     */
    private Integer backlog = 128;
    /**
     *  单次epoll获取事件数组长度
     */
    private Integer maxEvents = 16;
    /**
     *  单次分配内存块大小
     */
    private Integer segmentSize = 4 * Constants.KB;
}
