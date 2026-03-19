package com.example.ticketbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PROMO_CODES")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "promo_code_seq")
    @SequenceGenerator(name = "promo_code_seq", sequenceName = "PROMO_CODE_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE", nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "DISCOUNT_TYPE", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "DISCOUNT_VALUE", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "MIN_ORDER_AMOUNT", precision = 14, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "USAGE_LIMIT")
    private Integer usageLimit;

    @Column(name = "USED_COUNT", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "START_DATE")
    private LocalDateTime startDate;

    @Column(name = "END_DATE")
    private LocalDateTime endDate;

    @Column(name = "ACTIVE", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (code != null) code = code.toUpperCase();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
