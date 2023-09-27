package cn.zorcc.common.http;

import cn.zorcc.common.Constants;

import java.lang.foreign.MemorySegment;

/**
 *   Http response abstraction
 */
public class HttpResponse {
    /**
     *   Http version, default would be HTTP1.1
     */
    private String version = Constants.DEFAULT_HTTP_VERSION;
    /**
     *   Http status code, default would be 200
     */
    private HttpStatus status = HttpStatus.OK;
    /**
     *   Http headers, must exist
     */
    private HttpHeader headers = new HttpHeader();
    /**
     *   Http content data, normally would be json UTF-8 bytes, could be null for chunked data
     */
    private MemorySegment data;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public HttpHeader getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeader headers) {
        this.headers = headers;
    }

    public MemorySegment getData() {
        return data;
    }

    public void setData(MemorySegment data) {
        this.data = data;
    }
}
