package com.modak.ratelimitapp.domain.models.http;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationResponse {

    String message;
}
