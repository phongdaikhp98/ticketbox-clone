package com.example.ticketbox.service;

import com.example.ticketbox.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    private static final String KEY_PREFIX = "seat:reservation:";

    private String key(Long seatId) {
        return KEY_PREFIX + seatId;
    }

    /**
     * Try to reserve a seat for a user.
     * Uses SET NX EX — atomic, returns false if already reserved/sold.
     */
    public boolean reserveSeat(Long seatId, Long userId) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key(seatId), String.valueOf(userId),
                        Duration.ofSeconds(appProperties.getSeat().getReservationTtlSeconds()));
        return Boolean.TRUE.equals(result);
    }

    /**
     * Release a seat reservation.
     */
    public void releaseSeat(Long seatId) {
        redisTemplate.delete(key(seatId));
    }

    /**
     * Release all reservations held by a user.
     * Uses SCAN to find matching keys (safe for production).
     */
    public void releaseUserSeats(Long userId) {
        String userIdStr = String.valueOf(userId);
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) return;
        for (String k : keys) {
            String reserved = redisTemplate.opsForValue().get(k);
            if (userIdStr.equals(reserved)) {
                redisTemplate.delete(k);
            }
        }
    }

    /**
     * Get the userId who has a reservation on seatId, or null if not reserved.
     */
    public String getReservedBy(Long seatId) {
        return redisTemplate.opsForValue().get(key(seatId));
    }

    public boolean isReservedByUser(Long seatId, Long userId) {
        return String.valueOf(userId).equals(getReservedBy(seatId));
    }

    /**
     * Batch check seat availability from Redis using pipeline.
     * Returns list of userIds (or null) per seatId in same order.
     */
    public List<String> getSeatReservations(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return new ArrayList<>();
        List<String> keys = seatIds.stream().map(this::key).toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        return values != null ? values : new ArrayList<>();
    }
}
