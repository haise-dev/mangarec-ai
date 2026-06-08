package com.mangarec.features.ratelimit.service;

import com.mangarec.common.util.RequestHashUtils;
import com.mangarec.exception.RateLimitExceededException;
import com.mangarec.features.ratelimit.config.RateLimitProperties;
import com.mangarec.features.ratelimit.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpRateLimitServiceTest {
    private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    private static final Duration ONE_DAY = Duration.ofDays(1);

    @Mock
    private RedisRateLimiter redisRateLimiter;

    private OtpRateLimitService service;

    @BeforeEach
    void setUp() {
        service = new OtpRateLimitService(redisRateLimiter, new RateLimitProperties());
    }

    @Test
    void forgotPasswordChecksEmailAndIpWindows() {
        MockHttpServletRequest request = request();
        String email = "Reader@Example.com";
        String emailHash = RequestHashUtils.sha256("reader@example.com");
        String ipHash = RequestHashUtils.sha256("198.51.100.20");

        when(redisRateLimiter.checkSlidingWindow(any(), anyInt(), any()))
                .thenReturn(new RateLimitResult(true, 1, 0));

        assertThatCode(() -> service.checkForgotPassword(email, request)).doesNotThrowAnyException();

        InOrder inOrder = inOrder(redisRateLimiter);
        inOrder.verify(redisRateLimiter)
                .checkSlidingWindow("rate_limit:otp:email:15m:" + emailHash, 3, FIFTEEN_MINUTES);
        inOrder.verify(redisRateLimiter)
                .checkSlidingWindow("rate_limit:otp:email:1d:" + emailHash, 5, ONE_DAY);
        inOrder.verify(redisRateLimiter)
                .checkSlidingWindow("rate_limit:otp:ip:15m:" + ipHash, 10, FIFTEEN_MINUTES);
        inOrder.verify(redisRateLimiter)
                .checkSlidingWindow("rate_limit:otp:ip:1d:" + ipHash, 30, ONE_DAY);
    }

    @Test
    void forgotPasswordThrowsWhenEmailWindowIsExceeded() {
        MockHttpServletRequest request = request();
        String emailHash = RequestHashUtils.sha256("reader@example.com");

        when(redisRateLimiter.checkSlidingWindow("rate_limit:otp:email:15m:" + emailHash, 3, FIFTEEN_MINUTES))
                .thenReturn(new RateLimitResult(false, 0, 900));

        assertThatThrownBy(() -> service.checkForgotPassword("reader@example.com", request))
                .isInstanceOfSatisfying(RateLimitExceededException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("Too many password reset requests for this email");
                    assertThat(exception.getRetryAfterSeconds()).isEqualTo(900);
                });

        verifyNoMoreInteractions(redisRateLimiter);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");
        return request;
    }
}
