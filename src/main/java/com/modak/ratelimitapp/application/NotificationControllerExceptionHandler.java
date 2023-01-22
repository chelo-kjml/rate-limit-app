package com.modak.ratelimitapp.application;

import com.modak.ratelimitapp.domain.models.exception.NoSuchRateLimitTypeException;
import com.modak.ratelimitapp.domain.models.exception.RateLimitReachedException;
import com.modak.ratelimitapp.domain.models.http.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

import java.util.Objects;

@Slf4j
@RestControllerAdvice(assignableTypes = {
        NotificationController.class
})
public class NotificationControllerExceptionHandler {

    @ResponseStatus(
            value = HttpStatus.INTERNAL_SERVER_ERROR,
            reason = "unhandled exception")
    @ExceptionHandler
    public NotificationResponse handleException(Exception e) {
        return NotificationResponse.builder()
                .message("Internal Server Error. Please contact support.")
                .build();
    }

    @ResponseStatus(
            value = HttpStatus.TOO_MANY_REQUESTS,
            reason = "rate limit reached")
    @ExceptionHandler(RateLimitReachedException.class)
    public NotificationResponse handleException(RateLimitReachedException e) {
        return NotificationResponse.builder()
                .message(e.getMessage())
                .build();
    }

    @ResponseStatus(
            value = HttpStatus.BAD_REQUEST,
            reason = "rate limit reached")
    @ExceptionHandler(NoSuchRateLimitTypeException.class)
    public NotificationResponse handleException(NoSuchRateLimitTypeException e) {
        return NotificationResponse.builder()
                .message(e.getMessage())
                .build();
    }
}
