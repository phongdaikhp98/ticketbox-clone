package com.example.ticketbox.repository;

import com.example.ticketbox.model.PromoCode;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCodeIgnoreCase(String code);

    @Modifying
    @Transactional
    @Query("UPDATE PromoCode p SET p.usedCount = p.usedCount + 1 WHERE p.id = :id")
    void incrementUsedCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE PromoCode p SET p.usedCount = CASE WHEN p.usedCount > 0 THEN p.usedCount - 1 ELSE 0 END WHERE p.id = :id")
    void decrementUsedCount(@Param("id") Long id);
}
