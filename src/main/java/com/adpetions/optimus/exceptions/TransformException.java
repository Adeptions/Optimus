package com.adpetions.optimus.exceptions;

import java.io.IOException;

public class TransformException extends IOException {
    /**
     * Constructs an {@code TransformException} with {@code null}
     * as its error detail message.
     */
    public TransformException() {
        super();
    }

    /**
     * Constructs an {@code TransformException} with the specified detail message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     */
    public TransformException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code TransformException} with the specified detail message
     * and cause.
     *
     * <p>Note that the detail message associated with {@code cause} is
     * <i>not</i> automatically incorporated into this exception's detail
     * message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     *
     * @param cause
     *        The cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A null value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     */
    public TransformException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an {@code TransformException} with the specified cause and a
     * detail message of {@code (cause==null ? null : cause.toString())}
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause
     *        The cause (which is saved for later retrieval by the
     *        {@link #getCause()} method).  (A null value is permitted,
     *        and indicates that the cause is nonexistent or unknown.)
     */
    public TransformException(Throwable cause) {
        super(cause);
    }
}

