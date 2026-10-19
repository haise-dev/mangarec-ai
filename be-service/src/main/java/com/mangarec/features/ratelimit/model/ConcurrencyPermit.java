package com.mangarec.features.ratelimit.model;

public record ConcurrencyPermit(
        boolean acquired,
        String key,
        String requestId,
        long remaining,
        long retryAfterSeconds
) {
}
