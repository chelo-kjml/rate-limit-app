package com.modak.ratelimitapp.domain.models.exception;

public class NoSuchRateLimitTypeException extends RuntimeException {

    public NoSuchRateLimitTypeException(String message) {
        super(message);
    }
}
