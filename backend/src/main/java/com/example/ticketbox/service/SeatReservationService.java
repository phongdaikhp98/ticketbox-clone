package com.example.ticketbox.service;

import com.example.ticketbox.config.AppProperties;
import com.example.ticketbox.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
        // [SECURITY] Limit reservations per user to prevent seat-squatting (M1)
        int maxReservations = appProperties.getSeat().getMaxReservationsPerUser();
        if (getReservationCountByUser(userId) >= maxReservations) {
            throw new BadRequestException(
                    "Bạn đang giữ tối đa " + maxReservations + " ghế. Vui lòng hủy bớt trước khi chọn thêm.");
        }
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
     * [SECURITY] Uses non-blocking SCAN iterator — replaces KEYS * which blocks Redis (H2).
     */
    public void releaseUserSeats(Long userId) {
        String userIdStr = String.valueOf(userId);
        List<String> userKeys = scanKeysOwnedByUser(userIdStr);
        if (!userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }
    }

    /**
     * Count how many seats the user currently holds in Redis.
     * Uses non-blocking SCAN (H2/M1).
     */
    public int getReservationCountByUser(Long userId) {
        return scanKeysOwnedByUser(String.valueOf(userId)).size();
    }

    /**
     * SCAN all seat:reservation:* keys and return those whose value matches userId.
     * Non-blocking O(N) with small COUNT batches — safe for production.
     */
    private List<String> scanKeysOwnedByUser(String userIdStr) {
        // Step 1: collect all matching keys via SCAN (non-blocking)
        List<String> allKeys = new ArrayList<>();
        redisTemplate.execute((RedisCallback<Void>) conn -> {
            try (var cursor = conn.scan(
                    ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(100).build())) {
                cursor.forEachRemaining(k -> allKeys.add(new String(k, StandardCharsets.UTF_8)));
            }
            return null;
        });
        if (allKeys.isEmpty()) return allKeys;

        // Step 2: batch GET values and filter by userId
        List<String> values = redisTemplate.opsForValue().multiGet(allKeys);
        List<String> ownedKeys = new ArrayList<>();
        if (values != null) {
            for (int i = 0; i < allKeys.size(); i++) {
                if (userIdStr.equals(values.get(i))) {
                    ownedKeys.add(allKeys.get(i));
                }
            }
        }
        return ownedKeys;
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
