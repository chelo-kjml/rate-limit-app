package com.modak.ratelimitapp.domain.models;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RateLimitDefinition {

    Long id;
    String name;
    int windowInSeconds;
    int maxThreshold;

}
