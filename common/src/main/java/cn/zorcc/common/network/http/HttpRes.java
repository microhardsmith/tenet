package cn.zorcc.common.network.http;

import cn.zorcc.common.Constants;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpRes {
    /**
     *   Http版本号
     */
    private String version = Constants.DEFAULT_HTTP_VERSION;
    /**
     *   Http状态码
     */
    private HttpStatus status = HttpStatus.OK;
    /**
     *   Http header信息
     */
    private Map<String, String> headers = new HashMap<>();
    /**
     *   返回体数据
     */
    private byte[] data;
}
