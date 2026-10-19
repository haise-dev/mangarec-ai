package com.mangarec.features.ratelimit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mangarec.common.util.RequestHashUtils;
import com.mangarec.domain.user.model.SubscriptionStatus;
import com.mangarec.features.guest.GuestSessionConstants;
import com.mangarec.features.ratelimit.config.RateLimitProperties;
import com.mangarec.features.ratelimit.model.AiRateLimitSubjectType;
import com.mangarec.features.ratelimit.model.ConcurrencyPermit;
import com.mangarec.features.ratelimit.model.RateLimitResult;
import com.mangarec.features.ratelimit.service.RedisRateLimiter;
import com.mangarec.common.response.ApiResponse;
import com.mangarec.features.chat.controller.response.ChatResponse;
import com.mangarec.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AiRateLimitFilter extends OncePerRequestFilter {
    private static final Duration ONE_DAY = Duration.ofDays(1);
    private static final Duration TOKEN_BUCKET_TTL = Duration.ofHours(2);
    private static final Duration CONCURRENT_LEASE = Duration.ofMinutes(5);

    private final RedisRateLimiter redisRateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        return !("/api/chat".equals(path)
                || path.equals("/api/recommendation")
                || path.startsWith("/api/recommendation/")
                || path.equals("/api/ai")
                || path.startsWith("/api/ai/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        AiRateLimitSubject subject = resolveSubject(request);
        if (subject == null) {
            writeError(response, request, HttpServletResponse.SC_BAD_REQUEST,
                    "Bad Request", "guest_id is required. Call POST /api/guests first.", 0);
            return;
        }

        String api = apiKey(request);
        RateLimitResult rateLimitResult = checkRequestQuota(subject, request, api);
        if (!rateLimitResult.allowed()) {
            String path = request.getServletPath();
            if ("/api/chat".equals(path)) {
                if (subject.type() == AiRateLimitSubjectType.GUEST) {
                    writeSoftChatError(response, "Vui lòng đăng nhập để trải nghiệm thêm nhiều tính năng");
                    return;
                }
                if (subject.type() == AiRateLimitSubjectType.FREE) {
                    writeSoftChatError(response, "Bạn đã dùng hết lượt chat miễn phí hôm nay. Vui lòng nâng cấp gói Pro để tiếp tục.");
                    return;
                }
            }
            writeError(response, request, HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests", quotaMessage(subject.type()), rateLimitResult.retryAfterSeconds());
            return;
        }

        ConcurrencyPermit permit = redisRateLimiter.acquireConcurrent(
                concurrentKey(subject),
                concurrentLimit(subject.type()),
                CONCURRENT_LEASE
        );
        if (!permit.acquired()) {
            writeError(response, request, HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests", "Too many AI requests are running at the same time",
                    permit.retryAfterSeconds());
            return;
        }

        try {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining()));
            filterChain.doFilter(request, response);
        } finally {
            redisRateLimiter.releaseConcurrent(permit);
        }
    }

    private RateLimitResult checkRequestQuota(AiRateLimitSubject subject, HttpServletRequest request, String api) {
        RateLimitProperties.Ai ai = properties.getAi();

        if (subject.type() == AiRateLimitSubjectType.GUEST) {
            RateLimitResult ipCap = redisRateLimiter.checkSlidingWindow(
                    "rate_limit:guest_ip:" + ipHash(request) + ":" + api,
                    ai.getIpDailyLimit(),
                    ONE_DAY
            );
            if (!ipCap.allowed()) {
                return ipCap;
            }

            return redisRateLimiter.checkSlidingWindow(
                    "rate_limit:guest:" + subject.id() + ":" + api,
                    ai.getGuestDailyLimit(),
                    ONE_DAY
            );
        }

        if (subject.type() == AiRateLimitSubjectType.FREE) {
            return redisRateLimiter.checkSlidingWindow(
                    "rate_limit:user:" + subject.id() + ":" + api,
                    ai.getFreeDailyLimit(),
                    ONE_DAY
            );
        }

        RateLimitResult bucket = redisRateLimiter.checkTokenBucket(
                "rate_limit:pro:bucket:" + subject.id() + ":" + api,
                ai.getProBurstCapacity(),
                ai.getProRefillPerMinute(),
                TOKEN_BUCKET_TTL
        );
        if (!bucket.allowed()) {
            return bucket;
        }

        return redisRateLimiter.checkSlidingWindow(
                "rate_limit:pro:daily:" + subject.id() + ":" + api,
                ai.getProDailyLimit(),
                ONE_DAY
        );
    }

    private AiRateLimitSubject resolveSubject(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return new AiRateLimitSubject(resolveUserType(user), user.getId().toString());
        }

        String guestId = guestId(request);
        if (!StringUtils.hasText(guestId)) {
            return null;
        }

        return new AiRateLimitSubject(AiRateLimitSubjectType.GUEST, guestId);
    }

    private AiRateLimitSubjectType resolveUserType(AuthenticatedUser user) {
        boolean pro = user.getSubscriptionStatus() == SubscriptionStatus.PRO
                && (user.getSubscriptionExpiredAt() == null || user.getSubscriptionExpiredAt().isAfter(Instant.now()));
        return pro ? AiRateLimitSubjectType.PRO : AiRateLimitSubjectType.FREE;
    }

    private String guestId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (GuestSessionConstants.COOKIE_NAME.equals(cookie.getName())
                        && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue().trim();
                }
            }
        }

        String headerGuestId = request.getHeader(GuestSessionConstants.HEADER_NAME);
        return StringUtils.hasText(headerGuestId) ? headerGuestId.trim() : null;
    }

    private int concurrentLimit(AiRateLimitSubjectType type) {
        RateLimitProperties.Concurrent concurrent = properties.getAi().getConcurrent();
        return switch (type) {
            case GUEST -> concurrent.getGuest();
            case FREE -> concurrent.getFree();
            case PRO -> concurrent.getPro();
        };
    }

    private String concurrentKey(AiRateLimitSubject subject) {
        return switch (subject.type()) {
            case GUEST -> "concurrent:guest:" + subject.id();
            case FREE -> "concurrent:user:" + subject.id();
            case PRO -> "concurrent:pro:" + subject.id();
        };
    }

    private String quotaMessage(AiRateLimitSubjectType type) {
        return switch (type) {
            case GUEST -> "Guest AI quota exceeded";
            case FREE -> "Free user AI quota exceeded";
            case PRO -> "Pro user AI rate limit exceeded";
        };
    }

    private String apiKey(HttpServletRequest request) {
        String path = request.getServletPath().toLowerCase(Locale.ROOT);
        if (path.equals("/api/chat")) {
            return "chat";
        }
        if (path.equals("/api/recommendation") || path.startsWith("/api/recommendation/")) {
            return "recommendation";
        }
        return "ai";
    }

    private String ipHash(HttpServletRequest request) {
        return RequestHashUtils.sha256(RequestHashUtils.clientIp(request));
    }

    private void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            int status,
            String error,
            String message,
            long retryAfterSeconds
    ) throws IOException {
        if (retryAfterSeconds > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        }

        ErrorResponse body = new ErrorResponse();
        body.setTimestamp(new Date());
        body.setStatus(status);
        body.setPath(request.getRequestURI());
        body.setError(error);
        body.setMessage(message);

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private void writeSoftChatError(HttpServletResponse response, String message) throws IOException {
        ChatResponse chatResponse = new ChatResponse(null, message, java.util.Collections.emptyList(), Instant.now());
        ApiResponse<ChatResponse> apiResponse = ApiResponse.<ChatResponse>builder()
                .status(200)
                .message("Chat quota exceeded soft response")
                .data(chatResponse)
                .build();

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private record AiRateLimitSubject(AiRateLimitSubjectType type, String id) {
    }

    @Getter
    @Setter
    private static class ErrorResponse {
        private Date timestamp;
        private int status;
        private String path;
        private String error;
        private String message;
    }
}
