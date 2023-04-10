package cn.zorcc.common.network.http;

/**
 *   Http状态码及其描述，只列举了部分常用的返回状态
 *   在前后端分离的架构中，只返回json数据，应该尽可能避免返回除OK以外的其他状态
 */
public enum HttpStatus {
    OK("200", "OK"),
    BAD_REQUEST("400", "Bad Request"),
    UNAUTHORIZED("401", "Unauthorized"),
    FORBIDDEN("403", "Forbidden"),
    NOT_FOUND("404", "Not Found"),
    METHOD_NOT_ALLOWED("405", "Method Not Allowed"),
    NOT_ACCEPTABLE("406", "Not Found"),
    INTERNAL_SERVER_ERR("500", "Internal Server Error");

    private final String code;
    private final String description;

    HttpStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }
}
