package com.mangarec.features.ratelimit.model;

public record RateLimitResult(
        boolean allowed,
        long remaining,
        long retryAfterSeconds
) {
}
