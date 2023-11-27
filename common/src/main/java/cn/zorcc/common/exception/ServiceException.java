package cn.zorcc.common.exception;

import cn.zorcc.common.Constants;

/**
 *  TODO refactor
 */
public final class ServiceException extends RuntimeException {
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
