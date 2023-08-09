package com.github.viktor235.gameretriever.exception;

public class AuthException extends AppException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
