package com.modak.ratelimitapp.infrastructure.gateway;

public interface Gateway {

    void send(String userId, String message);
}
