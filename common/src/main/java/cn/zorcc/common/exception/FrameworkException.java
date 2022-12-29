package cn.zorcc.common.exception;

import cn.zorcc.common.enums.ExceptionType;
import lombok.extern.slf4j.Slf4j;

/**
 * 框架层出现的异常，出现该类异常意味着在通用框架层面出现了问题，应用可能不能正常运行，开发者需要根据报错信息对应用进行调整
 */
@Slf4j
public class FrameworkException extends RuntimeException {

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
        super(args == null ? message : String.format(message, args), throwable);
        log.error("FrameworkException caught, Type : [{}], Message : [{}]", exceptionType.name(), super.getMessage());
    }
}
