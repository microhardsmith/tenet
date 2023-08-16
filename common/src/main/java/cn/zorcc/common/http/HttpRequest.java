package cn.zorcc.common.http;

/**
 *  Http request abstraction
 */
public class HttpRequest {
    /**
     *  Http request method
     */
    private HttpMethod method;
    /**
     *  Http request rri
     */
    private String uri;
    /**
     *  Http version, default would be HTTP1.1
     */
    private String version;
    /**
     *  Http headers
     */
    private HttpHeader httpHeader = new HttpHeader();
    /**
     *  Http content data, normally would be json UTF-8 bytes or null
     */
    private byte[] data;

    public HttpRequest() {
    }

    public HttpMethod getMethod() {
        return this.method;
    }

    public String getUri() {
        return this.uri;
    }

    public String getVersion() {
        return this.version;
    }

    public HttpHeader getHttpHeader() {
        return this.httpHeader;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setHttpHeader(HttpHeader httpHeader) {
        this.httpHeader = httpHeader;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
