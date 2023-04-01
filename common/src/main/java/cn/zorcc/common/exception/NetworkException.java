package cn.zorcc.common.exception;

public class NetworkException extends RuntimeException {
    public static final String CONNECTION_TIME_OUT = "Acquire connection timeout";
    public NetworkException(String msg) {
        super(msg);
    }
}
