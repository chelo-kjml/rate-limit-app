package com.modak.ratelimitapp.infrastructure.rateLimiter;

public interface RateLimiter {

    void checkRateLimitAndExecuteOperation(String type, String user, Runnable runnable) throws Exception;

}
