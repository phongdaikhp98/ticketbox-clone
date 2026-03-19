package com.example.ticketbox.service;

import com.example.ticketbox.dto.PromoCodeRequest;
import com.example.ticketbox.dto.PromoCodeResponse;
import com.example.ticketbox.dto.ValidatePromoCodeResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.DiscountType;
import com.example.ticketbox.model.PromoCode;
import com.example.ticketbox.repository.PromoCodeRepository;
import com.example.ticketbox.repository.PromoCodeUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;

    /**
     * Validate promo code for FE preview — không apply, chỉ tính discount
     */
    public ValidatePromoCodeResponse validate(String code, Long userId, BigDecimal subtotal) {
        try {
            PromoCode promo = getValidPromoCode(code, userId, subtotal);
            BigDecimal discountAmount = calculateDiscount(promo, subtotal);
            return ValidatePromoCodeResponse.builder()
                    .valid(true)
                    .discountType(promo.getDiscountType())
                    .discountValue(promo.getDiscountValue())
                    .discountAmount(discountAmount)
                    .message("Áp dụng mã thành công")
                    .build();
        } catch (BadRequestException e) {
            return ValidatePromoCodeResponse.builder()
                    .valid(false)
                    .message(e.getMessage())
                    .build();
        }
    }

    /**
     * Validate và trả về PromoCode entity (dùng trong checkout). Throw nếu invalid.
     */
    public PromoCode getValidPromoCode(String code, Long userId, BigDecimal subtotal) {
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));

        if (!promo.isActive()) {
            throw new BadRequestException("Mã giảm giá đã bị vô hiệu hóa");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartDate() != null && now.isBefore(promo.getStartDate())) {
            throw new BadRequestException("Mã giảm giá chưa có hiệu lực");
        }

        if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
            throw new BadRequestException("Mã giảm giá đã hết hạn");
        }

        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
        }

        if (promoCodeUsageRepository.existsByPromoCodeIdAndUserId(promo.getId(), userId)) {
            throw new BadRequestException("Bạn đã sử dụng mã giảm giá này rồi");
        }

        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    "Đơn hàng tối thiểu " + formatVnd(promo.getMinOrderAmount()) + " để sử dụng mã này");
        }

        return promo;
    }

    /**
     * Tính discount amount từ promo và subtotal
     */
    public BigDecimal calculateDiscount(PromoCode promo, BigDecimal subtotal) {
        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            return subtotal
                    .multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            // FLAT — không giảm quá tổng đơn
            return promo.getDiscountValue().min(subtotal);
        }
    }

    // ===================== ADMIN CRUD =====================

    public PromoCodeResponse create(PromoCodeRequest request) {
        String upperCode = request.getCode().trim().toUpperCase();
        if (promoCodeRepository.findByCodeIgnoreCase(upperCode).isPresent()) {
            throw new BadRequestException("Mã '" + upperCode + "' đã tồn tại");
        }

        PromoCode promo = PromoCode.builder()
                .code(upperCode)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .usageLimit(request.getUsageLimit())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(request.isActive())
                .build();

        return toResponse(promoCodeRepository.save(promo));
    }

    public PromoCodeResponse update(Long id, PromoCodeRequest request) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromoCode", id));

        String upperCode = request.getCode().trim().toUpperCase();
        // Kiểm tra duplicate code (bỏ qua chính nó)
        promoCodeRepository.findByCodeIgnoreCase(upperCode).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Mã '" + upperCode + "' đã tồn tại");
            }
        });

        promo.setCode(upperCode);
        promo.setDiscountType(request.getDiscountType());
        promo.setDiscountValue(request.getDiscountValue());
        promo.setMinOrderAmount(request.getMinOrderAmount());
        promo.setUsageLimit(request.getUsageLimit());
        promo.setStartDate(request.getStartDate());
        promo.setEndDate(request.getEndDate());
        promo.setActive(request.isActive());

        return toResponse(promoCodeRepository.save(promo));
    }

    public List<PromoCodeResponse> findAll() {
        return promoCodeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public PromoCodeResponse findById(Long id) {
        return toResponse(promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromoCode", id)));
    }

    public PromoCodeResponse toggleActive(Long id) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromoCode", id));
        promo.setActive(!promo.isActive());
        return toResponse(promoCodeRepository.save(promo));
    }

    // ===================== Private helpers =====================

    private PromoCodeResponse toResponse(PromoCode promo) {
        return PromoCodeResponse.builder()
                .id(promo.getId())
                .code(promo.getCode())
                .discountType(promo.getDiscountType())
                .discountValue(promo.getDiscountValue())
                .minOrderAmount(promo.getMinOrderAmount())
                .usageLimit(promo.getUsageLimit())
                .usedCount(promo.getUsedCount())
                .startDate(promo.getStartDate())
                .endDate(promo.getEndDate())
                .active(promo.isActive())
                .createdDate(promo.getCreatedDate())
                .updatedDate(promo.getUpdatedDate())
                .build();
    }

    private String formatVnd(BigDecimal amount) {
        return String.format("%,.0fđ", amount);
    }
}
