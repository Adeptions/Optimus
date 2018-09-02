package com.adpetions.optimus.exceptions;

public class BadMatchPathException extends RuntimeException {
    /**
     * Constructs an {@code BadMatchPathException} with the specified detail message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     */
    public BadMatchPathException(String message) {
        super(message);
    }
}
