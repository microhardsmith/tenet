package cn.zorcc.common.http;

/**
 * Http request abstraction
 */
public class HttpRequest {
    /**
     * Http request method
     */
    private HttpMethod method;
    /**
     * Http request rri
     */
    private String uri;
    /**
     * Http version, default would be HTTP1.1
     */
    private String version;
    /**
     * Http headers
     */
    private HttpHeader httpHeader = new HttpHeader();
    /**
     * Http content data, normally would be json UTF-8 bytes or null
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HttpRequest)) return false;
        final HttpRequest other = (HttpRequest) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$method = this.getMethod();
        final Object other$method = other.getMethod();
        if (this$method == null ? other$method != null : !this$method.equals(other$method)) return false;
        final Object this$uri = this.getUri();
        final Object other$uri = other.getUri();
        if (this$uri == null ? other$uri != null : !this$uri.equals(other$uri)) return false;
        final Object this$version = this.getVersion();
        final Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final Object this$httpHeader = this.getHttpHeader();
        final Object other$httpHeader = other.getHttpHeader();
        if (this$httpHeader == null ? other$httpHeader != null : !this$httpHeader.equals(other$httpHeader))
            return false;
        if (!java.util.Arrays.equals(this.getData(), other.getData())) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HttpRequest;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $method = this.getMethod();
        result = result * PRIME + ($method == null ? 43 : $method.hashCode());
        final Object $uri = this.getUri();
        result = result * PRIME + ($uri == null ? 43 : $uri.hashCode());
        final Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final Object $httpHeader = this.getHttpHeader();
        result = result * PRIME + ($httpHeader == null ? 43 : $httpHeader.hashCode());
        result = result * PRIME + java.util.Arrays.hashCode(this.getData());
        return result;
    }

    public String toString() {
        return "HttpRequest(method=" + this.getMethod() + ", uri=" + this.getUri() + ", version=" + this.getVersion() + ", httpHeader=" + this.getHttpHeader() + ", data=" + java.util.Arrays.toString(this.getData()) + ")";
    }
}
