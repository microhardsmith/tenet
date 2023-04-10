package cn.zorcc.common.network.http;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 *   用于封装Http请求体
 */
@Data
public class HttpReq {
    /**
     *   Http访问方法
     */
    private HttpMethod method;
    /**
     *   访问Uri
     */
    private String uri;
    /**
     *   Http版本号
     */
    private String version;
    /**
     *   Http header信息
     */
    private Map<String, String> headers = new HashMap<>();
    /**
     *   Http 数据体
     */
    private byte[] data;
}
