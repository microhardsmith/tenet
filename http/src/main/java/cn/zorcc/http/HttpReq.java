package cn.zorcc.http;

import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * Http请求解析
 * @param method Http请求方法
 * @param headers Http请求头
 * @param path Http请求路径
 * @param params Http请求参数,格式 ?a=1&b=2
 * @param body Http Body请求体字节数组
 */
public record HttpReq (
        HttpMethod method,
        Map<String, String> headers,
        String path,
        Map<String, String> params,
        byte[] body
){
    /**
     * 将FullHttpRequest解析为HttpReq
     * 典型的Http请求Uri为http://www.xxx.com/test?name=someone&x=true#stuff #之后的字符会被忽略
     */
    public static HttpReq from(FullHttpRequest fullHttpRequest) {
        HttpMethod method = fullHttpRequest.method();
        Map<String, String> headers = new HashMap<>();
        fullHttpRequest.headers().entries().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        Map<String, String> params = new HashMap<>();
        String uri = fullHttpRequest.uri();
        char[] chars = uri.toCharArray();
        String path = uri;
        for(int i = 0; i < chars.length; i++) {
            if(chars[i] == '?') {
                path = new String(chars, 0, i);
                String key = null;
                for(int j = ++i; j < chars.length; j++) {
                    char d = chars[j];
                    if(d == '=') {
                        key = new String(chars, i, j - i);
                        if(key.isBlank()) {
                            throw new FrameworkException(ExceptionType.HTTP, "Illegal http uri: " + uri);
                        }
                        i = j + 1;
                    }else if(d == '&' || d == '#') {
                        String value = new String(chars, i, j - i);
                        if(key == null || value.isBlank()) {
                            throw new FrameworkException(ExceptionType.HTTP, "Illegal http uri: " + uri);
                        }else {
                            params.put(key, value);
                            key = null;
                            i = j + 1;
                        }
                    }
                }
                if(key != null) {
                    String value = new String(chars, i, chars.length - i);
                    if(value.isBlank()) {
                        throw new FrameworkException(ExceptionType.HTTP, "Illegal http uri: " + uri);
                    }
                    params.put(key, value);
                }
                break;
            }else if(chars[i] == '#') {
                path = new String(chars, 0, i);
            }
        }
        ByteBuf content = fullHttpRequest.content();
        return new HttpReq(method, headers, path, params, ByteBufUtil.getBytes(content));
    }
}
