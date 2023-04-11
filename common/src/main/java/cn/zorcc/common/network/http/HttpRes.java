package cn.zorcc.common.network.http;

import cn.zorcc.common.Constants;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpRes {
    /**
     *   Http version, default would be HTTP1.1
     */
    private String version = Constants.DEFAULT_HTTP_VERSION;
    /**
     *   Http status code, default would be 200
     */
    private HttpStatus status = HttpStatus.OK;
    /**
     *   Http headers
     */
    private Map<String, String> headers = new HashMap<>();
    /**
     *   Http content data, normally would be json UTF-8 bytes
     */
    private byte[] data;
}
