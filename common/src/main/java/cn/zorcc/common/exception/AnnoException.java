package cn.zorcc.common.exception;


/**
 *   A dedicated exception to describe what's wrong during the compilation phase
 */
public final class AnnoException extends RuntimeException {
    public AnnoException(final String message) {
        super(message);
    }
}
