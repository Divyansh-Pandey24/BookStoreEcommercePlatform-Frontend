package com.booknest.wallet.exception;

// Simple custom exception for resource not found (404)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
