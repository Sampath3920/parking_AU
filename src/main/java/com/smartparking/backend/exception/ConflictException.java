package com.smartparking.backend.exception;

/** Thrown when a request clashes with the current data (for example: slot already booked). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
