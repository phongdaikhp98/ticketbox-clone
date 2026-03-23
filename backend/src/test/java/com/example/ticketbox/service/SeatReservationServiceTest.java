package com.example.ticketbox.service;

import com.example.ticketbox.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatReservationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private AppProperties appProperties = new AppProperties();

    @InjectMocks
    private SeatReservationService seatReservationService;

    private static final Long SEAT_ID = 42L;
    private static final Long USER_ID = 7L;
    private static final String KEY = "seat:reservation:42";

    /** Helper: stub opsForValue() to return the mock ValueOperations. */
    private void stubOpsForValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void reserveSeat_seatAvailable_returnsTrue() {
        stubOpsForValue();
        when(valueOperations.setIfAbsent(eq(KEY), eq("7"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        boolean result = seatReservationService.reserveSeat(SEAT_ID, USER_ID);

        assertTrue(result);
        verify(valueOperations).setIfAbsent(eq(KEY), eq("7"), any(Duration.class));
    }

    @Test
    void reserveSeat_seatAlreadyReserved_returnsFalse() {
        stubOpsForValue();
        when(valueOperations.setIfAbsent(eq(KEY), eq("7"), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        boolean result = seatReservationService.reserveSeat(SEAT_ID, USER_ID);

        assertFalse(result);
    }

    @Test
    void reserveSeat_redisReturnsNull_returnsFalse() {
        stubOpsForValue();
        when(valueOperations.setIfAbsent(eq(KEY), eq("7"), any(Duration.class)))
                .thenReturn(null);

        boolean result = seatReservationService.reserveSeat(SEAT_ID, USER_ID);

        assertFalse(result);
    }

    @Test
    void releaseSeat_deletesKey() {
        seatReservationService.releaseSeat(SEAT_ID);
        verify(redisTemplate).delete(KEY);
    }

    @Test
    void getReservedBy_keyExists_returnsUserId() {
        stubOpsForValue();
        when(valueOperations.get(KEY)).thenReturn("7");
        String result = seatReservationService.getReservedBy(SEAT_ID);
        assertEquals("7", result);
    }

    @Test
    void getReservedBy_keyAbsent_returnsNull() {
        stubOpsForValue();
        when(valueOperations.get(KEY)).thenReturn(null);
        String result = seatReservationService.getReservedBy(SEAT_ID);
        assertNull(result);
    }

    @Test
    void isReservedByUser_seatReservedBySameUser_returnsTrue() {
        stubOpsForValue();
        when(valueOperations.get(KEY)).thenReturn("7");
        assertTrue(seatReservationService.isReservedByUser(SEAT_ID, USER_ID));
    }

    @Test
    void isReservedByUser_seatReservedByDifferentUser_returnsFalse() {
        stubOpsForValue();
        when(valueOperations.get(KEY)).thenReturn("99");
        assertFalse(seatReservationService.isReservedByUser(SEAT_ID, USER_ID));
    }

    @Test
    void isReservedByUser_seatNotReserved_returnsFalse() {
        stubOpsForValue();
        when(valueOperations.get(KEY)).thenReturn(null);
        assertFalse(seatReservationService.isReservedByUser(SEAT_ID, USER_ID));
    }

    @Test
    void getSeatReservations_multipleSeats_returnsValues() {
        stubOpsForValue();
        List<String> expectedValues = new java.util.ArrayList<>();
        expectedValues.add("7");
        expectedValues.add(null);
        when(valueOperations.multiGet(anyList())).thenReturn(expectedValues);

        List<String> result = seatReservationService.getSeatReservations(List.of(1L, 2L));

        assertEquals(2, result.size());
        assertEquals("7", result.get(0));
        assertNull(result.get(1));
    }

    @Test
    void getSeatReservations_emptyList_returnsEmptyList() {
        List<String> result = seatReservationService.getSeatReservations(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSeatReservations_nullList_returnsEmptyList() {
        List<String> result = seatReservationService.getSeatReservations(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void releaseUserSeats_deletesMatchingKeys() {
        stubOpsForValue();
        Set<String> keys = new java.util.HashSet<>();
        keys.add(KEY);
        keys.add("seat:reservation:99");
        when(redisTemplate.keys("seat:reservation:*")).thenReturn(keys);
        when(valueOperations.get(KEY)).thenReturn("7");
        when(valueOperations.get("seat:reservation:99")).thenReturn("99");

        seatReservationService.releaseUserSeats(USER_ID);

        verify(redisTemplate).delete(KEY);
        verify(redisTemplate, never()).delete("seat:reservation:99");
    }

    @Test
    void releaseUserSeats_noKeysFound_doesNothing() {
        when(redisTemplate.keys("seat:reservation:*")).thenReturn(null);
        assertDoesNotThrow(() -> seatReservationService.releaseUserSeats(USER_ID));
        verify(redisTemplate, never()).delete(anyString());
    }
}
