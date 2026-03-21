package com.example.ticketbox.service;

import com.example.ticketbox.dto.PromoCodeRequest;
import com.example.ticketbox.dto.PromoCodeResponse;
import com.example.ticketbox.dto.ValidatePromoCodeResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.model.DiscountType;
import com.example.ticketbox.model.PromoCode;
import com.example.ticketbox.repository.PromoCodeRepository;
import com.example.ticketbox.repository.PromoCodeUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @Mock
    private PromoCodeUsageRepository promoCodeUsageRepository;

    @InjectMocks
    private PromoCodeService promoCodeService;

    private PromoCode activePromoPercentage;
    private PromoCode activePromoFlat;
    private final Long userId = 10L;
    private final BigDecimal subtotal = BigDecimal.valueOf(500_000);

    @BeforeEach
    void setUp() {
        activePromoPercentage = PromoCode.builder()
                .id(1L)
                .code("SAVE20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .active(true)
                .usedCount(0)
                .build();

        activePromoFlat = PromoCode.builder()
                .id(2L)
                .code("FLAT50K")
                .discountType(DiscountType.FLAT)
                .discountValue(BigDecimal.valueOf(50_000))
                .active(true)
                .usedCount(0)
                .build();
    }

    @Test
    void validate_validCode_returnsValidResponseWithDiscount() {
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(activePromoPercentage));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndUserId(1L, userId)).thenReturn(false);

        ValidatePromoCodeResponse response = promoCodeService.validate("SAVE20", userId, subtotal);

        assertTrue(response.isValid());
        assertNotNull(response.getDiscountAmount());
        assertEquals(new BigDecimal("100000"), response.getDiscountAmount());
        assertEquals(DiscountType.PERCENTAGE, response.getDiscountType());
        assertNotNull(response.getMessage());
    }

    @Test
    void validate_codeNotFound_returnsInvalidResponse() {
        when(promoCodeRepository.findByCodeIgnoreCase("NOTEXIST")).thenReturn(Optional.empty());

        ValidatePromoCodeResponse response = promoCodeService.validate("NOTEXIST", userId, subtotal);

        assertFalse(response.isValid());
        assertNotNull(response.getMessage());
    }

    @Test
    void getValidPromoCode_codeNotFound_throwsBadRequestException() {
        when(promoCodeRepository.findByCodeIgnoreCase("GHOST")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> promoCodeService.getValidPromoCode("GHOST", userId, subtotal));
    }

    @Test
    void getValidPromoCode_codeExpired_throwsBadRequestException() {
        PromoCode expired = PromoCode.builder()
                .id(3L)
                .code("OLD")
                .discountType(DiscountType.FLAT)
                .discountValue(BigDecimal.valueOf(10_000))
                .active(true)
                .usedCount(0)
                .endDate(LocalDateTime.now().minusDays(1))
                .build();

        when(promoCodeRepository.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(expired));

        assertThrows(BadRequestException.class,
                () -> promoCodeService.getValidPromoCode("OLD", userId, subtotal));
    }

    @Test
    void getValidPromoCode_codeNotYetActive_throwsBadRequestException() {
        PromoCode future = PromoCode.builder()
                .id(4L)
                .code("FUTURE")
                .discountType(DiscountType.FLAT)
                .discountValue(BigDecimal.valueOf(10_000))
                .active(true)
                .usedCount(0)
                .startDate(LocalDateTime.now().plusDays(5))
                .build();

        when(promoCodeRepository.findByCodeIgnoreCase("FUTURE")).thenReturn(Optional.of(future));

        assertThrows(BadRequestException.class,
                () -> promoCodeService.getValidPromoCode("FUTURE", userId, subtotal));
    }

    @Test
    void getValidPromoCode_usageLimitExceeded_throwsBadRequestException() {
        PromoCode exhausted = PromoCode.builder()
                .id(5L)
                .code("FULL")
                .discountType(DiscountType.FLAT)
                .discountValue(BigDecimal.valueOf(10_000))
                .active(true)
                .usageLimit(100)
                .usedCount(100)
                .build();

        when(promoCodeRepository.findByCodeIgnoreCase("FULL")).thenReturn(Optional.of(exhausted));

        assertThrows(BadRequestException.class,
                () -> promoCodeService.getValidPromoCode("FULL", userId, subtotal));
    }

    @Test
    void getValidPromoCode_orderBelowMinAmount_throwsBadRequestException() {
        PromoCode minRequired = PromoCode.builder()
                .id(6L)
                .code("MINREQ")
                .discountType(DiscountType.FLAT)
                .discountValue(BigDecimal.valueOf(50_000))
                .active(true)
                .usedCount(0)
                .minOrderAmount(BigDecimal.valueOf(1_000_000))
                .build();

        when(promoCodeRepository.findByCodeIgnoreCase("MINREQ")).thenReturn(Optional.of(minRequired));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndUserId(6L, userId)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> promoCodeService.getValidPromoCode("MINREQ", userId, BigDecimal.valueOf(200_000)));
    }

    @Test
    void getValidPromoCode_userAlreadyUsedCode_throwsBadRequestException() {
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(activePromoPercentage));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndUserId(1L, userId)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> promoCodeService.getValidPromoCode("SAVE20", userId, subtotal));
    }

    @Test
    void getValidPromoCode_validCode_returnsPromoCodeEntity() {
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(activePromoPercentage));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndUserId(1L, userId)).thenReturn(false);

        PromoCode result = promoCodeService.getValidPromoCode("SAVE20", userId, subtotal);

        assertNotNull(result);
        assertEquals("SAVE20", result.getCode());
        assertEquals(DiscountType.PERCENTAGE, result.getDiscountType());
    }

    @Test
    void calculateDiscount_percentageType_returnsCorrectPercentageAmount() {
        BigDecimal result = promoCodeService.calculateDiscount(activePromoPercentage, BigDecimal.valueOf(500_000));

        assertEquals(new BigDecimal("100000"), result);
    }

    @Test
    void calculateDiscount_flatType_returnsCorrectFlatAmount() {
        BigDecimal result = promoCodeService.calculateDiscount(activePromoFlat, BigDecimal.valueOf(500_000));

        assertEquals(BigDecimal.valueOf(50_000), result);
    }

    @Test
    void calculateDiscount_percentageCappedAt100Percent_resultNeverNegative() {
        PromoCode bigPercent = PromoCode.builder()
                .id(7L)
                .code("OVER100")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(150))
                .active(true)
                .usedCount(0)
                .build();

        BigDecimal result = promoCodeService.calculateDiscount(bigPercent, BigDecimal.valueOf(100_000));

        assertTrue(result.compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void calculateDiscount_flatTypeExceedsSubtotal_cappedAtSubtotal() {
        PromoCode bigFlat = PromoCode.builder()
                .id(8L)
                .code("HUGE")
                .discountType(DiscountType.FLAT)
                .discountValue(BigDecimal.valueOf(999_999))
                .active(true)
                .usedCount(0)
                .build();

        BigDecimal tinySubtotal = BigDecimal.valueOf(10_000);
        BigDecimal result = promoCodeService.calculateDiscount(bigFlat, tinySubtotal);

        assertEquals(tinySubtotal, result);
    }

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCode("NEW10");
        request.setDiscountType(DiscountType.PERCENTAGE);
        request.setDiscountValue(BigDecimal.valueOf(10));
        request.setActive(true);

        PromoCode savedPromo = PromoCode.builder()
                .id(99L)
                .code("NEW10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .active(true)
                .usedCount(0)
                .build();

        when(promoCodeRepository.findByCodeIgnoreCase("NEW10")).thenReturn(Optional.empty());
        when(promoCodeRepository.save(any(PromoCode.class))).thenReturn(savedPromo);

        PromoCodeResponse response = promoCodeService.create(request);

        assertNotNull(response);
        assertEquals("NEW10", response.getCode());
        assertEquals(99L, response.getId());
        verify(promoCodeRepository).save(any(PromoCode.class));
    }

    @Test
    void create_duplicateCode_throwsBadRequestException() {
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCode("SAVE20");
        request.setDiscountType(DiscountType.PERCENTAGE);
        request.setDiscountValue(BigDecimal.valueOf(20));

        when(promoCodeRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(activePromoPercentage));

        assertThrows(BadRequestException.class, () -> promoCodeService.create(request));
        verify(promoCodeRepository, never()).save(any());
    }
}
