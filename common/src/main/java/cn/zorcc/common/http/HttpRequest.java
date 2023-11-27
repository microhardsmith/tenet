package cn.zorcc.common.http;

import java.lang.foreign.MemorySegment;

/**
 *  Http request abstraction
 */
public final class HttpRequest {
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
    private MemorySegment data;

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public HttpHeader getHttpHeader() {
        return httpHeader;
    }

    public void setHttpHeader(HttpHeader httpHeader) {
        this.httpHeader = httpHeader;
    }

    public MemorySegment getData() {
        return data;
    }

    public void setData(MemorySegment data) {
        this.data = data;
    }
}
