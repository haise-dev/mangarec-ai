package com.mangarec.features.ratelimit.filter;

import com.mangarec.common.util.RequestHashUtils;
import com.mangarec.domain.user.entity.UserEntity;
import com.mangarec.domain.user.model.SubscriptionStatus;
import com.mangarec.features.guest.GuestSessionConstants;
import com.mangarec.features.ratelimit.config.RateLimitProperties;
import com.mangarec.features.ratelimit.model.ConcurrencyPermit;
import com.mangarec.features.ratelimit.model.RateLimitResult;
import com.mangarec.features.ratelimit.service.RedisRateLimiter;
import com.mangarec.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRateLimitFilterTest {
    private static final Duration ONE_DAY = Duration.ofDays(1);
    private static final Duration TOKEN_BUCKET_TTL = Duration.ofHours(2);
    private static final Duration CONCURRENT_LEASE = Duration.ofMinutes(5);

    @Mock
    private RedisRateLimiter redisRateLimiter;

    @Mock
    private FilterChain filterChain;

    private AiRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AiRateLimitFilter(redisRateLimiter, new RateLimitProperties());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chatRequestWithoutGuestIdOrJwtReturnsBadRequest() throws Exception {
        MockHttpServletRequest request = chatRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(response.getContentAsString()).contains("guest_id is required");
        verifyNoInteractions(redisRateLimiter);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void guestChatRequestStopsWhenGuestQuotaIsExceeded() throws Exception {
        String guestId = "guest-123";
        MockHttpServletRequest request = chatRequest();
        request.setCookies(new Cookie(GuestSessionConstants.COOKIE_NAME, guestId));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisRateLimiter.checkSlidingWindow("rate_limit:guest_ip:" + ipHash(request) + ":chat", 100, ONE_DAY))
                .thenReturn(new RateLimitResult(true, 99, 0));
        when(redisRateLimiter.checkSlidingWindow("rate_limit:guest:" + guestId + ":chat", 20, ONE_DAY))
                .thenReturn(new RateLimitResult(false, 0, 600));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("600");
        assertThat(response.getContentAsString()).contains("Guest AI quota exceeded");
        verify(redisRateLimiter, never()).acquireConcurrent(any(), any(Integer.class), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void guestChatRequestRunsAndReleasesConcurrentPermitWhenAllowed() throws Exception {
        String guestId = "guest-456";
        MockHttpServletRequest request = chatRequest();
        request.setCookies(new Cookie(GuestSessionConstants.COOKIE_NAME, guestId));
        MockHttpServletResponse response = new MockHttpServletResponse();
        ConcurrencyPermit permit = new ConcurrencyPermit(true, "concurrent:guest:" + guestId, "request-1", 0, 0);

        when(redisRateLimiter.checkSlidingWindow("rate_limit:guest_ip:" + ipHash(request) + ":chat", 100, ONE_DAY))
                .thenReturn(new RateLimitResult(true, 99, 0));
        when(redisRateLimiter.checkSlidingWindow("rate_limit:guest:" + guestId + ":chat", 20, ONE_DAY))
                .thenReturn(new RateLimitResult(true, 12, 0));
        when(redisRateLimiter.acquireConcurrent("concurrent:guest:" + guestId, 1, CONCURRENT_LEASE))
                .thenReturn(permit);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (servletRequest, servletResponse) -> {
            chainCalled.set(true);
            ((HttpServletResponse) servletResponse).setStatus(202);
        };

        filter.doFilter(request, response, chain);

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("12");
        verify(redisRateLimiter).releaseConcurrent(permit);
    }

    @Test
    void authenticatedProChatRequestUsesTokenBucketAndDailySafetyCap() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthenticatedProUser(userId);
        MockHttpServletRequest request = chatRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ConcurrencyPermit permit = new ConcurrencyPermit(true, "concurrent:pro:" + userId, "request-2", 4, 0);

        when(redisRateLimiter.checkTokenBucket("rate_limit:pro:bucket:" + userId + ":chat", 30, 10, TOKEN_BUCKET_TTL))
                .thenReturn(new RateLimitResult(true, 29, 0));
        when(redisRateLimiter.checkSlidingWindow("rate_limit:pro:daily:" + userId + ":chat", 1000, ONE_DAY))
                .thenReturn(new RateLimitResult(true, 999, 0));
        when(redisRateLimiter.acquireConcurrent("concurrent:pro:" + userId, 5, CONCURRENT_LEASE))
                .thenReturn(permit);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (servletRequest, servletResponse) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("999");
        verify(redisRateLimiter).releaseConcurrent(permit);
    }

    private MockHttpServletRequest chatRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/chat");
        request.setServletPath("/api/chat");
        request.setRemoteAddr("203.0.113.10");
        return request;
    }

    private String ipHash(MockHttpServletRequest request) {
        return RequestHashUtils.sha256(RequestHashUtils.clientIp(request));
    }

    private void setAuthenticatedProUser(UUID userId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail("pro@example.com");
        user.setSubscriptionStatus(SubscriptionStatus.PRO);

        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
