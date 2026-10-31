package com.banking.utility.exception;

/**
 * Base runtime exception for banking domain errors.
 * <p>
 * Applications can extend this class to model business exceptions and map them to
 * appropriate HTTP status codes in their own exception handlers.
 */
public class BaseException extends RuntimeException {

    /**
     * Constructs a new base banking exception with {@code null} as its detail message.
     */
    public BaseException() {
        super();
    }

    /**
     * Constructs a new base banking exception with the specified detail message.
     *
     * @param message the detail message describing the error
     */
    public BaseException(final String message) {
        super(message);
    }

    /**
     * Constructs a new base banking exception with the specified detail message and cause.
     *
     * @param message the detail message describing the error
     * @param cause   the underlying cause of the exception
     */
    public BaseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new base banking exception with the specified cause.
     *
     * @param cause the underlying cause of the exception
     */
    public BaseException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new base banking exception with the specified detail message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or disabled.
     *
     * @param message            the detail message describing the error
     * @param cause              the underlying cause of the exception
     * @param enableSuppression  whether suppression is enabled or disabled
     * @param writableStackTrace whether the stack trace should be writable
     */
    protected BaseException(final String message, final Throwable cause, final boolean enableSuppression,
                            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
