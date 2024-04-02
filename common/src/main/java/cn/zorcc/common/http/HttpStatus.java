package cn.zorcc.common.http;

import java.nio.charset.StandardCharsets;

/**
 *   Http status codes
 */
public record HttpStatus(
        String code,
        String description,
        byte[] content
) {
    public HttpStatus(String code, String description) {
        this(code, description, (STR."\{code} \{description}").getBytes(StandardCharsets.UTF_8));
    }
    public static final HttpStatus OK = new HttpStatus("200", "OK");
    public static final HttpStatus CREATED = new HttpStatus("201", "Created");
    public static final HttpStatus ACCEPTED = new HttpStatus("202", "Accepted");
    public static final HttpStatus NON_AUTHORITATIVE_INFORMATION = new HttpStatus("203", "Non-Authoritative Information");
    public static final HttpStatus NO_CONTENT = new HttpStatus("204", "No Content");
    public static final HttpStatus RESET_CONTENT = new HttpStatus("205", "Reset Content");
    public static final HttpStatus PARTIAL_CONTENT = new HttpStatus("206", "Partial Content");
    public static final HttpStatus MULTIPLE_CHOICES = new HttpStatus("300", "Multiple Choices");
    public static final HttpStatus MOVED_PERMANENTLY = new HttpStatus("301", "Moved Permanently");
    public static final HttpStatus FOUND = new HttpStatus("302", "Found");
    public static final HttpStatus SEE_OTHER = new HttpStatus("303", "See Other");
    public static final HttpStatus NOT_MODIFIED = new HttpStatus("304", "Not Modified");
    public static final HttpStatus USE_PROXY = new HttpStatus("305", "Use Proxy");
    public static final HttpStatus TEMPORARY_REDIRECT = new HttpStatus("307", "Temporary Redirect");
    public static final HttpStatus BAD_REQUEST = new HttpStatus("400", "Bad Request");
    public static final HttpStatus UNAUTHORIZED = new HttpStatus("401", "Unauthorized");
    public static final HttpStatus FORBIDDEN = new HttpStatus("403", "Forbidden");
    public static final HttpStatus NOT_FOUND = new HttpStatus("404", "Not Found");
    public static final HttpStatus METHOD_NOT_ALLOWED = new HttpStatus("405", "Method Not Allowed");
    public static final HttpStatus NOT_ACCEPTABLE = new HttpStatus("406", "Not Acceptable");
    public static final HttpStatus PROXY_AUTHENTICATION_REQUIRED = new HttpStatus("407", "Proxy Authentication Required");
    public static final HttpStatus REQUEST_TIMEOUT = new HttpStatus("408", "Request Timeout");
    public static final HttpStatus CONFLICT = new HttpStatus("409", "Conflict");
    public static final HttpStatus GONE = new HttpStatus("410", "Gone");
    public static final HttpStatus LENGTH_REQUIRED = new HttpStatus("411", "Length Required");
    public static final HttpStatus PRECONDITION_FAILED = new HttpStatus("412", "Precondition Failed");
    public static final HttpStatus REQUEST_ENTITY_TOO_LARGE = new HttpStatus("413", "Request Entity Too Large");
    public static final HttpStatus REQUEST_URI_TOO_LONG = new HttpStatus("414", "Request-URI Too Long");
    public static final HttpStatus UNSUPPORTED_MEDIA_TYPE = new HttpStatus("415", "Unsupported Media Type");
    public static final HttpStatus REQUESTED_RANGE_NOT_SATISFIABLE = new HttpStatus("416", "Requested Range Not Satisfiable");
    public static final HttpStatus EXPECTATION_FAILED = new HttpStatus("417", "Expectation Failed");
    public static final HttpStatus INTERNAL_SERVER_ERR = new HttpStatus("500", "Internal Server Error");
    public static final HttpStatus NOT_IMPLEMENTED = new HttpStatus("501", "Not Implemented");
    public static final HttpStatus BAD_GATEWAY = new HttpStatus("502", "Bad Gateway");
    public static final HttpStatus SERVICE_UNAVAILABLE = new HttpStatus("503", "Service Unavailable");
    public static final HttpStatus GATEWAY_TIMEOUT = new HttpStatus("504", "Gateway Timeout");
    public static final HttpStatus HTTP_VERSION_NOT_SUPPORTED = new HttpStatus("505", "HTTP Version Not Supported");

}
