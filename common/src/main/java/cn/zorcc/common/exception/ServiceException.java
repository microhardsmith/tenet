package cn.zorcc.common.exception;

import cn.zorcc.common.Constants;

/**
 * 服务端因为外部操作导致的异常,需要用户来进行捕获和处理
 * 用户应该继承ServiceException来定义自己的code返回给前端,框架默认使用的正确code为200,错误code为500
 */
public class ServiceException extends RuntimeException {
    private final String code;
    private final String msg;

    public ServiceException(String msg) {
        super(msg);
        this.msg = msg;
        this.code = Constants.ERR;
    }

    public ServiceException(String code, String msg) {
        super("code : " + code + " rpcMsg : " + msg);
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
