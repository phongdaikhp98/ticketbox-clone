package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.PromoCodeRequest;
import com.example.ticketbox.dto.PromoCodeResponse;
import com.example.ticketbox.dto.ValidatePromoCodeResponse;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.PromoCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    /**
     * Validate promo code — dùng cho FE preview discount trước khi checkout
     */
    @GetMapping("/v1/promo-codes/validate")
    public ResponseEntity<ApiResponse<ValidatePromoCodeResponse>> validate(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        ValidatePromoCodeResponse result = promoCodeService.validate(code, userDetails.getId(), subtotal);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ===================== ADMIN endpoints =====================

    @GetMapping("/v1/admin/promo-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PromoCodeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.findAll()));
    }

    @PostMapping("/v1/admin/promo-codes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> create(
            @Valid @RequestBody PromoCodeRequest request) {
        PromoCodeResponse created = promoCodeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @PutMapping("/v1/admin/promo-codes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PromoCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.update(id, request)));
    }

    @PatchMapping("/v1/admin/promo-codes/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(promoCodeService.toggleActive(id)));
    }
}
