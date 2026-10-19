package com.mangarec.features.ratelimit.service;

import com.mangarec.features.ratelimit.model.ConcurrencyPermit;
import com.mangarec.features.ratelimit.model.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisRateLimiter {
    private static final DefaultRedisScript<List> SLIDING_WINDOW_SCRIPT = script("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]
            local ttl = tonumber(ARGV[5])

            redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window)
            local count = redis.call('ZCARD', key)

            if count >= limit then
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                local retry = ttl
                if oldest[2] then
                    retry = math.ceil((tonumber(oldest[2]) + window - now) / 1000)
                end
                return {0, 0, retry}
            end

            redis.call('ZADD', key, now, member)
            redis.call('EXPIRE', key, ttl)
            return {1, limit - count - 1, 0}
            """);

    private static final DefaultRedisScript<List> TOKEN_BUCKET_SCRIPT = script("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local capacity = tonumber(ARGV[2])
            local refill_per_ms = tonumber(ARGV[3])
            local cost = tonumber(ARGV[4])
            local ttl = tonumber(ARGV[5])

            local bucket = redis.call('HMGET', key, 'tokens', 'updatedAt')
            local tokens = tonumber(bucket[1])
            local updated_at = tonumber(bucket[2])

            if tokens == nil then
                tokens = capacity
                updated_at = now
            end
            if updated_at == nil then
                updated_at = now
            end

            local elapsed = math.max(0, now - updated_at)
            tokens = math.min(capacity, tokens + (elapsed * refill_per_ms))

            if tokens < cost then
                local retry = math.ceil(((cost - tokens) / refill_per_ms) / 1000)
                redis.call('HSET', key, 'tokens', tokens, 'updatedAt', now)
                redis.call('EXPIRE', key, ttl)
                return {0, math.floor(tokens), retry}
            end

            tokens = tokens - cost
            redis.call('HSET', key, 'tokens', tokens, 'updatedAt', now)
            redis.call('EXPIRE', key, ttl)
            return {1, math.floor(tokens), 0}
            """);

    private static final DefaultRedisScript<List> CONCURRENT_SCRIPT = script("""
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local lease = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local request_id = ARGV[4]
            local ttl = tonumber(ARGV[5])

            redis.call('ZREMRANGEBYSCORE', key, '-inf', now)
            local count = redis.call('ZCARD', key)

            if count >= limit then
                local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
                local retry = 1
                if oldest[2] then
                    retry = math.ceil((tonumber(oldest[2]) - now) / 1000)
                end
                return {0, 0, retry}
            end

            redis.call('ZADD', key, now + lease, request_id)
            redis.call('EXPIRE', key, ttl)
            return {1, limit - count - 1, 0}
            """);

    private final StringRedisTemplate redisTemplate;

    public RateLimitResult checkSlidingWindow(String key, int limit, Duration window) {
        long now = Instant.now().toEpochMilli();
        long windowMillis = window.toMillis();
        String member = now + ":" + UUID.randomUUID();
        List<?> result = redisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                List.of(key),
                String.valueOf(now),
                String.valueOf(windowMillis),
                String.valueOf(limit),
                member,
                String.valueOf(expireSeconds(window))
        );

        return toRateLimitResult(result);
    }

    public RateLimitResult checkTokenBucket(
            String key,
            int capacity,
            int refillPerMinute,
            Duration ttl
    ) {
        long now = Instant.now().toEpochMilli();
        double refillPerMs = Math.max(1, refillPerMinute) / 60_000D;
        List<?> result = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(key),
                String.valueOf(now),
                String.valueOf(capacity),
                String.valueOf(refillPerMs),
                "1",
                String.valueOf(expireSeconds(ttl))
        );

        return toRateLimitResult(result);
    }

    public ConcurrencyPermit acquireConcurrent(String key, int limit, Duration lease) {
        String requestId = UUID.randomUUID().toString();
        long now = Instant.now().toEpochMilli();
        List<?> result = redisTemplate.execute(
                CONCURRENT_SCRIPT,
                List.of(key),
                String.valueOf(now),
                String.valueOf(lease.toMillis()),
                String.valueOf(limit),
                requestId,
                String.valueOf(expireSeconds(lease.plusSeconds(30)))
        );

        RateLimitResult rateLimitResult = toRateLimitResult(result);
        return new ConcurrencyPermit(
                rateLimitResult.allowed(),
                key,
                requestId,
                rateLimitResult.remaining(),
                rateLimitResult.retryAfterSeconds()
        );
    }

    public void releaseConcurrent(ConcurrencyPermit permit) {
        if (permit == null || !permit.acquired()) {
            return;
        }
        redisTemplate.opsForZSet().remove(permit.key(), permit.requestId());
    }

    private RateLimitResult toRateLimitResult(List<?> result) {
        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Redis rate limit script returned no result");
        }
        return new RateLimitResult(
                toLong(result.get(0)) == 1,
                Math.max(0, toLong(result.get(1))),
                Math.max(0, toLong(result.get(2)))
        );
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private long expireSeconds(Duration duration) {
        return Math.max(1, duration.plusSeconds(60).toSeconds());
    }

    private static DefaultRedisScript<List> script(String scriptText) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(scriptText);
        script.setResultType(List.class);
        return script;
    }
}
