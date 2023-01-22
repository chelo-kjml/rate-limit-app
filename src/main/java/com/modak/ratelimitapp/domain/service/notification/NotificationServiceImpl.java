package com.modak.ratelimitapp.domain.service.notification;


import com.modak.ratelimitapp.infrastructure.gateway.Gateway;
import com.modak.ratelimitapp.infrastructure.rateLimiter.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final Gateway gateway;
    private final RateLimiter rateLimiter;

    @Override
    public void send(String type, String userId, String message) throws Exception{

        try {
            rateLimiter.checkRateLimitAndExecuteOperation(type, userId,
                    () -> gateway.send(userId, message));
        } catch (Exception ex) {
            log.error("Unable to send message of type {} for user {}. Error: {}.",
                    type, userId, ex.getMessage());
            throw ex;
        }


    }
}
