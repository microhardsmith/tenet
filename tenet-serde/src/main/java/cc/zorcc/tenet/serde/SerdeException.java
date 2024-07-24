package cc.zorcc.tenet.serde;

/**
 * SerdeException is a custom exception used to indicate errors that occur during
 * serialization and deserialization processes
 */
public final class SerdeException extends RuntimeException {

    /**
     * Constructs a new SerdeException with the specified detail message.
     * The cause is not initialized and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message, saved for later retrieval by the {@link #getMessage()} method.
     */
    public SerdeException(String message) {
        super(message);
    }

    /**
     * Constructs a new SerdeException with the specified detail message and cause.
     *
     * @param message the detail message, saved for later retrieval by the {@link #getMessage()} method.
     * @param cause the cause, saved for later retrieval by the {@link #getCause()} method.
     *              A null value is permitted and indicates that the cause is nonexistent or unknown.
     */
    public SerdeException(String message, Throwable cause) {
        super(message, cause);
    }

}
