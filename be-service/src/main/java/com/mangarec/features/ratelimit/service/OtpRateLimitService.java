package com.mangarec.features.ratelimit.service;

import com.mangarec.common.util.RequestHashUtils;
import com.mangarec.exception.RateLimitExceededException;
import com.mangarec.features.ratelimit.config.RateLimitProperties;
import com.mangarec.features.ratelimit.model.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OtpRateLimitService {
    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration ONE_DAY = Duration.ofDays(1);

    private final RedisRateLimiter redisRateLimiter;
    private final RateLimitProperties properties;

    public void checkForgotPassword(String email, HttpServletRequest request) {
        String emailHash = RequestHashUtils.sha256(normalizeEmail(email));
        String ipHash = RequestHashUtils.sha256(RequestHashUtils.clientIp(request));
        RateLimitProperties.Otp otp = properties.getOtp();

        check("rate_limit:otp:email:15m:" + emailHash, otp.getEmailWindowLimit(), FIFTEEN_MINUTES,
                "Too many password reset requests for this email");
        check("rate_limit:otp:email:1d:" + emailHash, otp.getEmailDailyLimit(), ONE_DAY,
                "Daily password reset limit reached for this email");
        check("rate_limit:otp:ip:15m:" + ipHash, otp.getIpWindowLimit(), FIFTEEN_MINUTES,
                "Too many password reset requests from this IP");
        check("rate_limit:otp:ip:1d:" + ipHash, otp.getIpDailyLimit(), ONE_DAY,
                "Daily password reset limit reached from this IP");
    }

    private void check(String key, int limit, Duration window, String message) {
        RateLimitResult result = redisRateLimiter.checkSlidingWindow(key, limit, window);
        if (!result.allowed()) {
            throw new RateLimitExceededException(message, result.retryAfterSeconds());
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
