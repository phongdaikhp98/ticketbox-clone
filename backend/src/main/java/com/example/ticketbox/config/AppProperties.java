package com.example.ticketbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Central configuration properties for application-level tunables.
 * All values can be overridden in application.yml or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private RateLimit rateLimit = new RateLimit();
    private Upload upload = new Upload();
    private Auth auth = new Auth();
    private Seat seat = new Seat();
    private Reminder reminder = new Reminder();
    private Order order = new Order();
    private Refund refund = new Refund();
    private Dashboard dashboard = new Dashboard();

    @Getter
    @Setter
    public static class RateLimit {
        /** Max requests per minute for authenticated users (identified by email). */
        private int authenticatedLimit = 300;
        /** Max requests per minute for anonymous users (identified by IP). */
        private int anonymousLimit = 60;
        /** Max requests per minute per IP for sensitive endpoints (login, register, etc.). */
        private int sensitiveLimit = 10;
        /** Sliding window duration in minutes. */
        private int windowMinutes = 1;
        /** Endpoints subject to the stricter sensitive limit. */
        private List<String> sensitivePaths = List.of(
                "/v1/auth/login",
                "/v1/auth/register",
                "/v1/auth/forgot-password",
                "/v1/auth/reset-password"
        );
        /**
         * [SECURITY] Chỉ bật khi server nằm sau một reverse-proxy tin cậy (nginx, ALB...).
         * Khi false, getClientIp() bỏ qua X-Forwarded-For để tránh IP spoofing.
         */
        private boolean trustedProxyEnabled = false;
    }

    @Getter
    @Setter
    public static class Upload {
        /** Maximum allowed image upload size in megabytes. */
        private int maxFileSizeMb = 5;
    }

    @Getter
    @Setter
    public static class Auth {
        /** How long (minutes) a password-reset token stays valid in Redis. */
        private long passwordResetTtlMinutes = 15;
        /** How long (minutes) an email-verification token stays valid in Redis. */
        private long emailVerifyTtlMinutes = 60;
    }

    @Getter
    @Setter
    public static class Seat {
        /** How long (seconds) a seat reservation is held in Redis before expiring. */
        private long reservationTtlSeconds = 600;
    }

    @Getter
    @Setter
    public static class Reminder {
        /** Cron expression controlling when the event-reminder job runs. */
        private String cron = "0 0 */23 * * *";
        /** Lower bound (hours before event) of the reminder window. */
        private long windowStartHours = 23;
        /** Upper bound (hours before event) of the reminder window. */
        private long windowEndHours = 25;
    }

    @Getter
    @Setter
    public static class Order {
        /** Orders can only be cancelled when ALL events are at least this many hours away. */
        private long cancellationDeadlineHours = 24;
    }

    @Getter
    @Setter
    public static class Refund {
        /** Refunds can only be requested when ALL events are at least this many hours away. */
        private long deadlineHours = 24;
    }

    @Getter
    @Setter
    public static class Dashboard {
        /** Number of recent orders shown in the overview panel. */
        private int recentOrdersSize = 10;
        /** Number of top events shown in the overview panel. */
        private int topEventsSize = 5;
    }
}
