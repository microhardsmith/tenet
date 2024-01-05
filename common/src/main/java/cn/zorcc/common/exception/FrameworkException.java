package cn.zorcc.common.exception;

import cn.zorcc.common.ExceptionType;

/**
 *  Framework level exception
 */
public final class FrameworkException extends RuntimeException {

    public FrameworkException(ExceptionType exceptionType, String message) {
        this(exceptionType, message, null, (Object) null);
    }

    public FrameworkException(ExceptionType exceptionType, String message, Throwable throwable) {
        this(exceptionType, message, throwable, (Object) null);
    }

    public FrameworkException(ExceptionType exceptionType, String message, Object... args) {
        this(exceptionType, message, null, args);
    }

    public FrameworkException(ExceptionType exceptionType, String message, Throwable throwable, Object... args) {
        super(STR."Type : \{exceptionType}, Msg : \{args == null ? message : String.format(message, args)}", throwable);
    }
}
