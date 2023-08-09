package cn.zorcc.http;

import cn.zorcc.common.Constants;

/**
 * 网关http配置文件
 */
public class HttpConfig {
    /**
     *  Http端口号
     */
    private int port = 8000;
    /**
     * Http client worker线程数,最低为1
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getHttpClientThreads() {
        return httpClientThreads;
    }

    public void setHttpClientThreads(int httpClientThreads) {
        this.httpClientThreads = httpClientThreads;
    }

    public int getHttpClientTimeout() {
        return httpClientTimeout;
    }

    public void setHttpClientTimeout(int httpClientTimeout) {
        this.httpClientTimeout = httpClientTimeout;
    }

    public int getHttpServerThreads() {
        return httpServerThreads;
    }

    public void setHttpServerThreads(int httpServerThreads) {
        this.httpServerThreads = httpServerThreads;
    }

    public boolean isEnableWebSocket() {
        return enableWebSocket;
    }

    public void setEnableWebSocket(boolean enableWebSocket) {
        this.enableWebSocket = enableWebSocket;
    }

    public boolean isUsingHttps() {
        return usingHttps;
    }

    public void setUsingHttps(boolean usingHttps) {
        this.usingHttps = usingHttps;
    }

    public String getHttpSslCertFile() {
        return httpSslCertFile;
    }

    public void setHttpSslCertFile(String httpSslCertFile) {
        this.httpSslCertFile = httpSslCertFile;
    }

    public String getHttpSslKeyFile() {
        return httpSslKeyFile;
    }

    public void setHttpSslKeyFile(String httpSslKeyFile) {
        this.httpSslKeyFile = httpSslKeyFile;
    }

    public String getHttpCaCertFile() {
        return httpCaCertFile;
    }

    public void setHttpCaCertFile(String httpCaCertFile) {
        this.httpCaCertFile = httpCaCertFile;
    }
}
