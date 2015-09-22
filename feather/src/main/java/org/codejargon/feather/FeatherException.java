package org.codejargon.feather;

public class FeatherException extends RuntimeException {
    public FeatherException(String message) {
        super(message);
    }

    public FeatherException(String message, Throwable cause) {
        super(message, cause);
    }
}
