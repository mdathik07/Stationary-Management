package com.example.demo.exception;

/** Registration failed for a reason tied to a specific form field. */
public class RegistrationException extends RuntimeException {

    private final String field;

    public RegistrationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
