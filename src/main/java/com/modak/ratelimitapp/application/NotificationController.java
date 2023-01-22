package com.modak.ratelimitapp.application;

import com.modak.ratelimitapp.domain.models.http.NotificationRequest;
import com.modak.ratelimitapp.domain.models.http.NotificationResponse;
import com.modak.ratelimitapp.domain.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
@RequiredArgsConstructor

@RequestMapping(value = "/notification")
public class NotificationController {

    private final NotificationService notificationService;

    //For manual testing
    @PostMapping(path = "/send", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public NotificationResponse send(@RequestBody NotificationRequest notificationRequest) throws Exception {

        notificationService.send(notificationRequest.getRateLimitType(),
                notificationRequest.getUserId(),
                notificationRequest.getMessage());

        return NotificationResponse.builder()
                .message("Notification sent successfully.")
                .build();
    }
}
