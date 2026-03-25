package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.config.AppProperties;
import com.example.ticketbox.dto.PaymentUrlResponse;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final AppProperties appProperties;

    @PostMapping("/vnpay/create/{orderId}")
    public ResponseEntity<ApiResponse<PaymentUrlResponse>> createVnPayUrl(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long orderId,
            HttpServletRequest request) {
        String clientIp = getClientIp(request);
        PaymentUrlResponse response = orderService.createPaymentUrl(userDetails.getId(), orderId, clientIp);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnPayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> result = orderService.processVnPayIpn(params);
        return ResponseEntity.ok(result);
    }

    /**
     * [SECURITY] Read-only endpoint — only checks current order status.
     * State mutation happens exclusively in the IPN endpoint (Critical #2).
     */
    @PostMapping("/vnpay/verify-return")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyReturn(
            @RequestBody Map<String, String> params) {
        Map<String, String> result = orderService.getOrderStatusByTxnRef(params);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // [SECURITY] Only trust X-Forwarded-For when running behind a known proxy (High #7)
    private String getClientIp(HttpServletRequest request) {
        if (appProperties.getRateLimit().isTrustedProxyEnabled()) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
