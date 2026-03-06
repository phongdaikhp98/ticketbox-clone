package com.example.ticketbox.service;

import com.example.ticketbox.config.VNPayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VNPayServiceTest {

    private VNPayService vnPayService;

    @BeforeEach
    void setUp() {
        VNPayConfig config = new VNPayConfig();
        config.setTmnCode("TESTCODE");
        config.setHashSecret("TESTSECRET123");
        config.setPaymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        config.setReturnUrl("http://localhost:3000/payment/vnpay-return");
        config.setVersion("2.1.0");
        config.setCommand("pay");
        config.setOrderType("other");

        vnPayService = new VNPayService(config);
    }

    @Test
    void createPaymentUrl_shouldGenerateValidUrl() {
        String url = vnPayService.createPaymentUrl("REF001", 500000, "Test order", "127.0.0.1");

        assertNotNull(url);
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(url.contains("vnp_TmnCode=TESTCODE"));
        assertTrue(url.contains("vnp_Amount=50000000")); // 500000 * 100
        assertTrue(url.contains("vnp_TxnRef=REF001"));
        assertTrue(url.contains("vnp_SecureHash="));
    }

    @Test
    void validateSignature_shouldReturnFalseForMissingHash() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "REF001");

        assertFalse(vnPayService.validateSignature(params));
    }

    @Test
    void validateSignature_shouldReturnFalseForInvalidHash() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "REF001");
        params.put("vnp_SecureHash", "invalidhash");

        assertFalse(vnPayService.validateSignature(params));
    }

    @Test
    void validateSignature_shouldReturnFalseForEmptyHash() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "REF001");
        params.put("vnp_SecureHash", "");

        assertFalse(vnPayService.validateSignature(params));
    }
}
