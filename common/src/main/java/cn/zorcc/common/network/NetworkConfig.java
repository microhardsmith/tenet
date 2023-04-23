package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import lombok.Data;

/**
 *  Configuration of net
 */
@Data
public class NetworkConfig {
    /**
     *   是否开启TLS加密,默认不开启
     */
    private Boolean enableSsl = false;
    /**
     *   服务端证书公钥路径
     */
    private String publicKeyFile = Constants.EMPTY_STRING;
    /**
     *   服务端证书私钥路径
     */
    private String privateKeyFile = Constants.EMPTY_STRING;
    /**
     *  worker线程数量,默认为4,可以根据CPU核数进行适当调整
     *  Net的性能并不完全依赖于worker数量的提升,实际读写任务都会在虚拟线程中进行
     */
    private Integer workerCount = 4;
    /**
     *  是否可复用端口,默认为true
     */
    private Boolean reuseAddr = Boolean.TRUE;
    /**
     *  是否周期性发送保活报文以维持连接,推荐关闭,应在应用层手动实现心跳
     */
    private Boolean keepAlive = Boolean.FALSE;
    /**
     *  是否关闭Nagle算法,推荐关闭,这样每个小包都能得到及时发送
     */
    private Boolean tcpNoDelay = Boolean.TRUE;
    /**
     *  服务器ip地址, note: 目前只支持ipv4版本协议栈,使用localhost访问时可能会定向到ipv6,尽量使用0.0.0.0或127.0.0.1访问
     */
    private String ip = "0.0.0.0";
    /**
     *  服务器端口号
     */
    private Integer port = 8001;
    /**
     *  等待接受全连接socket队列长度
     */
    private Integer backlog = 64;
    /**
     *  单次epoll获取事件数组长度
     */
    private Integer maxEvents = 64;
    /**
     *  单次分配读内存块大小,每次轮询到事件后都会读取至多该值的内存数据
     */
    private Integer readBufferSize = 16 * Constants.KB;
    /**
     *  单次分配写内存块大小,该值只代表WriteBuffer初始大小,在后续写过程中会逐渐扩容
     */
    private Integer writeBufferSize = 4 * Constants.KB;
    /**
     *  socket map初始大小,后续会自动扩容
     */
    private Integer mapSize = 1024;
}
