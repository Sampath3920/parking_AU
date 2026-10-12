package com.smartparking.backend.exception;

/** Thrown at login when an administrator has deactivated the account (answered with HTTP 403). */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(String message) {
        super(message);
    }
}
