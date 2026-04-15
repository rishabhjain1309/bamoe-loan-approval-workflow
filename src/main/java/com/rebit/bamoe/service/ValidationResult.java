package com.rebit.bamoe.service;

public class ValidationResult {
    private java.util.Map<String, String> errors = new java.util.HashMap<>();

    public void addError(String field, String message) {
        errors.put(field, message);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public java.util.Map<String, String> getErrors() {
        return errors;
    }
}
