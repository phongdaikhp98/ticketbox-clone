package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
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

    @PostMapping("/vnpay/verify-return")
    public ResponseEntity<ApiResponse<Map<String, String>>> verifyReturn(
            @RequestBody Map<String, String> params) {
        Map<String, String> result = orderService.processVnPayIpn(params);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
