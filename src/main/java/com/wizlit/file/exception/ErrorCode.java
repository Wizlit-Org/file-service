package com.wizlit.file.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Centralized catalog for all error codes and their messages.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
        
    // File errors
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST,
            "Invalid file format"),

    // Generic errors
    EMPTY(HttpStatus.BAD_REQUEST,
            "No data to be returned"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,
            "Invalid Token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,
            "Token expired"),
    INACCESSIBLE_USER(HttpStatus.FORBIDDEN,
            "You are not allowed to access this resource"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "User not found with email: %s"),
    INTERNAL_SERVER(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. - %s"),
    UNKNOWN(HttpStatus.INTERNAL_SERVER_ERROR,
            "An unspecified error occurred.");


    private final HttpStatus status; // A clear and reusable error message
    private final String message; // A clear and reusable error message

    /**
     * Returns a formatted error message if the message contains placeholders.
     *
     * @param args Placeholder replacements for the error message
     * @return A formatted error message
     */
    public String getFormattedMessage(Object... args) {
        return String.format(message, args);
    }
}