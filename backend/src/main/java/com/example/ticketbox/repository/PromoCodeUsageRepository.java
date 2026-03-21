package com.example.ticketbox.repository;

import com.example.ticketbox.model.PromoCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {
    boolean existsByPromoCodeIdAndUserId(Long promoCodeId, Long userId);

    java.util.Optional<PromoCodeUsage> findByOrderId(Long orderId);
}
