package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import lombok.Data;

/**
 *  Configuration of net
 */
@Data
public class NetworkConfig {
    /**
     *  read buffer maximum size for a read operation
     */
    private long readBufferSize = 64 * Constants.KB;
    /**
     *  write buffer initial size for a write operation
     */
    private long writeBufferSize = 64 * Constants.KB;
    /**
     *  socket map initial size, will automatically expand, could be changed according to specific circumstances
     */
    private int mapSize = Constants.KB;
    /**
     *   服务端是否开启TLS加密,默认不开启
     */
    private int enableSsl = Constants.ZERO;
    /**
     *   服务端证书公钥路径,必须为绝对路径
     */
    private String publicKeyFile = Constants.EMPTY_STRING;
    /**
     *   服务端证书私钥路径,必须为绝对路径
     */
    private String privateKeyFile = Constants.EMPTY_STRING;
    /**
     *  是否可复用端口,默认为true,该选项只会影响服务端socket在bind上的行为,对客户端socket设置不会产生影响
     */
    private int reuseAddr = Constants.ONE;
    /**
     *  是否周期性发送保活报文以维持连接,推荐关闭,应在应用层手动实现心跳
     */
    private int keepAlive = Constants.ZERO;
    /**
     *  是否关闭Nagle算法,推荐关闭,这样每个小包都能得到及时发送
     */
    private int tcpNoDelay = Constants.ONE;
    /**
     *  优雅停机超时时间,单位豪秒
     */
    private long shutdownTimeout = 10000L;
    /**
     *  默认建立连接超时时间,单位毫秒
     */
    private long defaultConnectionTimeout = 5000L;
}
