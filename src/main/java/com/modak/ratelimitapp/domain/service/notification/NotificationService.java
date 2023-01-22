package com.modak.ratelimitapp.domain.service.notification;

public interface NotificationService {

    void send(String type, String userId, String message) throws Exception;
}
