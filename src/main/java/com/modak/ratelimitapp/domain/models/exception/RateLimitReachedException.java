package com.modak.ratelimitapp.domain.models.exception;

public class RateLimitReachedException extends RuntimeException {

    public RateLimitReachedException(String message) {
        super(message);
    }
}
