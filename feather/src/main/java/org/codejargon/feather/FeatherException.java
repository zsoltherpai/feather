package org.codejargon.feather;

public class FeatherException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -1606897572808444593L;

	FeatherException(String message) {
        super(message);
    }

    FeatherException(String message, Throwable cause) {
        super(message, cause);
    }
}
