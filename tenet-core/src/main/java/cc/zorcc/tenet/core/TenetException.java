package cc.zorcc.tenet.core;

/**
 * The {@code TenetException} class represents a specific type of runtime exception
 * that occurs within the Tenet framework. This exception encapsulates details about
 * the nature of the error through the {@link ExceptionType} enum, which categorizes
 * the various types of exceptions that can arise in the framework.
 */
public final class TenetException extends RuntimeException {

    /**
     * Constructs a new {@code TenetException} with the specified detail message and exception type.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param exceptionType the type of the exception, represented by {@link ExceptionType}
     * @param message the detail message, which is saved for later retrieval by the {@link #getMessage()} method
     */
    public TenetException(ExceptionType exceptionType, String message) {
        super("Exception Type : %s, Msg : %s".formatted(exceptionType, message));
    }

    /**
     * Constructs a new {@code TenetException} with the specified detail message, exception type, and cause.
     * Note that the detail message associated with {@code cause} is not automatically incorporated
     * into this exception's detail message.
     *
     * @param exceptionType the type of the exception, represented by {@link ExceptionType}
     * @param message the detail message, which is saved for later retrieval by the {@link #getMessage()} method
     * @param throwable the cause, which is saved for later retrieval by the {@link #getCause()} method.
     *        (A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public TenetException(ExceptionType exceptionType, String message, Throwable throwable) {
        super("Exception Type : %s, Msg : %s".formatted(exceptionType, message), throwable);
    }
}
