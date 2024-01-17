package cn.zorcc.common.http;

import cn.zorcc.common.Constants;
import cn.zorcc.common.structure.WriteBuffer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 *   Http header abstraction backed by a hashmap
 */
public final class HttpHeader {
    public static final String K_CONTENT_TYPE = "Content-Type";
    public static final String V_JSON_TYPE = "application/json";
    public static final String V_BINARY_TYPE = "application/octet-stream";
    public static final String K_CONTENT_LENGTH = "Content-Length";
    public static final String K_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String K_CONTENT_ENCODING = "Content-Encoding";
    public static final String V_GZIP = "gzip";
    public static final String V_DEFLATE = "deflate";
    public static final String K_CONNECTION = "Connection";
    public static final String K_KEEP_ALIVE = "Keep-Alive";
    public static final String V_KEEP_ALIVE = "keep-alive";
    public static final String V_TIMEOUT = "timeout=";
    public static final String K_DATE = "Date";
    public static final String K_TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String V_CHUNKED = "chunked";
    public static final String K_AUTHORIZATION = "Authorization";
    private final Map<String, String> headers = new HashMap<>();

    public String get(String key) {
        return headers.get(key);
    }

    public void put(String key, String value) {
        headers.put(key, value);
    }

    public void encode(WriteBuffer writeBuffer) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            writeBuffer.writeBytes(entry.getKey().getBytes(StandardCharsets.UTF_8));
            writeBuffer.writeBytes(Constants.HTTP_PAIR_SEP);
            writeBuffer.writeBytes(entry.getValue().getBytes(StandardCharsets.UTF_8));
            writeBuffer.writeBytes(Constants.HTTP_LINE_SEP);
        }
    }
}
