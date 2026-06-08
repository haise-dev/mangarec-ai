package com.mangarec.features.ratelimit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    private Ai ai = new Ai();
    private Otp otp = new Otp();

    @Getter
    @Setter
    public static class Ai {
        private int guestDailyLimit = 20;
        private int freeDailyLimit = 20;
        private int ipDailyLimit = 100;
        private int proBurstCapacity = 30;
        private int proRefillPerMinute = 10;
        private int proDailyLimit = 1000;
        private Concurrent concurrent = new Concurrent();
    }

    @Getter
    @Setter
    public static class Concurrent {
        private int guest = 1;
        private int free = 2;
        private int pro = 5;
    }

    @Getter
    @Setter
    public static class Otp {
        private int emailWindowLimit = 3;
        private int emailDailyLimit = 5;
        private int ipWindowLimit = 10;
        private int ipDailyLimit = 30;
    }
}
