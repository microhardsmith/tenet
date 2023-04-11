package cn.zorcc.common.network.http;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 *   Http request abstraction
 */
@Data
public class HttpReq {
    /**
     *   Http request method
     */
    private HttpMethod method;
    /**
     *   Http request rri
     */
    private String uri;
    /**
     *   Http version, default would be HTTP1.1
     */
    private String version;
    /**
     *   Http headers
     */
    private Map<String, String> headers = new HashMap<>();
    /**
     *   Http content data, normally would be json UTF-8 bytes
     */
    private byte[] data;
}
