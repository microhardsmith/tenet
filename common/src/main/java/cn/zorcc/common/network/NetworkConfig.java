package cn.zorcc.common.network;

import cn.zorcc.common.Constants;
import lombok.Data;

/**
 *  Configuration of net
 */
@Data
public class NetworkConfig {
    /**
     *   服务端是否开启TLS加密,默认不开启
     */
    private Boolean enableSsl = Boolean.FALSE;
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
     *  优雅停机超时时间,单位秒
     */
    private Long shutdownTimeout = 10L;
}
