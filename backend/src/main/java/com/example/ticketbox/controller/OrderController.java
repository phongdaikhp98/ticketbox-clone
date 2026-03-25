package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.config.AppProperties;
import com.example.ticketbox.dto.CheckoutRequest;
import com.example.ticketbox.dto.OrderResponse;
import com.example.ticketbox.dto.RefundResponse;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.OrderService;
import com.example.ticketbox.service.RefundService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final RefundService refundService;
    private final AppProperties appProperties;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse order = orderService.checkout(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> orders = orderService.getMyOrders(userDetails.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        OrderResponse order = orderService.getOrderDetail(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        orderService.cancelOrder(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Đơn hàng đã được hủy thành công", null));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> requestRefund(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id,
            HttpServletRequest request) {
        String clientIp = getClientIp(request);
        RefundResponse refund = refundService.requestRefund(userDetails.getId(), id, clientIp);
        return ResponseEntity.ok(ApiResponse.success(refund));
    }

    @GetMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        return refundService.getRefundByOrderId(userDetails.getId(), id)
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElse(ResponseEntity.ok(ApiResponse.success(null)));
    }

    // [SECURITY] Only trust X-Forwarded-For when running behind a known proxy (H1)
    private String getClientIp(HttpServletRequest request) {
        if (appProperties.getRateLimit().isTrustedProxyEnabled()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        String ip = request.getRemoteAddr();
        // Normalize IPv6 localhost to IPv4 — VNPay expects IPv4 format
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
