package cc.zorcc.core;

/**
 *   Framework Exception represents a common exception in Tenet framework itself
 */
public final class FrameworkException extends RuntimeException {
    public FrameworkException(ExceptionType exceptionType, String message) {
        super("Exception Type : %s, Msg : %s".formatted(exceptionType, message));
    }

    public FrameworkException(ExceptionType exceptionType, String message, Throwable throwable) {
        super("Exception Type : %s, Msg : %s".formatted(exceptionType, message), throwable);
    }
}
