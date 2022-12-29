package cn.zorcc.http;

import cn.zorcc.common.Constants;
import lombok.Getter;
import lombok.Setter;

/**
 * 网关http配置文件
 */
@Getter
@Setter
public class HttpConfig {
    /**
     *  Http端口号
     */
    private int port = 8000;
    /**
     * Http client worker线程数，最低为1
     */
    private int httpClientThreads = 1;
    /**
     * Http client 连接超时时间,单位毫秒
     */
    private int httpClientTimeout = 3000;
    /**
     *  Http server worker线程数,如果小于等于0则会使用boss线程
     */
    private int httpServerThreads = 4;
    /**
     *  是否启用WebSocket
     */
    private boolean enableWebSocket = false;
    /**
     * 是否开启https
     */
    private boolean usingHttps = false;
    /**
     * ssl证书文件地址, .crt或.pem格式
     */
    private String httpSslCertFile = Constants.EMPTY_STRING;
    /**
     * ssl证书key文件地址, .key格式
     */
    private String httpSslKeyFile = Constants.EMPTY_STRING;
    /**
     * ssl ca证书文件地址, .crt或.pem格式
     */
    private String httpCaCertFile = Constants.EMPTY_STRING;
}
