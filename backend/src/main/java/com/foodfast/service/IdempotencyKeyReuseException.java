package com.foodfast.service;

public class IdempotencyKeyReuseException extends IllegalStateException {
    public IdempotencyKeyReuseException(String message) {
        super(message);
    }
}
