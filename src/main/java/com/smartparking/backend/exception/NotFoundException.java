package com.smartparking.backend.exception;

/** Thrown when a record asked for by the user does not exist (answered with HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
