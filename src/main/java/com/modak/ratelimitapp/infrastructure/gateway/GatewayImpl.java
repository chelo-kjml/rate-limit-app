package com.modak.ratelimitapp.infrastructure.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayImpl implements Gateway {

    public void send(String userId, String message) {

        log.info("Sending message {} to user {}.", message, userId);

    }
}
