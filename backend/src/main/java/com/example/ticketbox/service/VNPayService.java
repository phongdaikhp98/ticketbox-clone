package com.example.ticketbox.service;

import com.example.ticketbox.config.VNPayConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    private static final DateTimeFormatter VNPAY_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String createPaymentUrl(String txnRef, long amount, String orderInfo, String clientIp) {
        LocalDateTime now = LocalDateTime.now();

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", now.format(VNPAY_DATE_FMT));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNPAY_DATE_FMT));

        String queryString = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        return vnPayConfig.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    public boolean validateSignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");

        String queryString = sortedParams.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String computedHash = hmacSHA512(vnPayConfig.getHashSecret(), queryString);
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    /**
     * Call VNPay refund API and return (responseCode, message).
     * Hash for refund is pipe-delimited raw string (NOT URL-encoded).
     */
    public VNPayRefundResult callRefundApi(String txnRef, String transactionNo,
                                           String transactionDate, long amount,
                                           String requestId, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        String createDate = now.format(VNPAY_DATE_FMT);
        String createBy = "ticketbox_system";
        String orderInfo = "Hoan tien don hang " + txnRef;
        String transactionType = "02"; // full refund
        String amountStr = String.valueOf(amount * 100L);

        String safeTransactionNo = transactionNo != null ? transactionNo : "";
        String safeTransactionDate = transactionDate != null ? transactionDate : "";

        // Hash data: pipe-separated raw values per VNPay refund API spec
        String hashData = String.join("|",
                requestId, vnPayConfig.getVersion(), "refund",
                vnPayConfig.getTmnCode(), transactionType, txnRef, amountStr,
                orderInfo, safeTransactionNo, safeTransactionDate,
                createBy, createDate, clientIp);

        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", vnPayConfig.getVersion());
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        body.put("vnp_TransactionType", transactionType);
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_Amount", amountStr);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionNo", safeTransactionNo);
        body.put("vnp_TransactionDate", safeTransactionDate);
        body.put("vnp_CreateBy", createBy);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", clientIp);
        body.put("vnp_SecureHash", secureHash);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(body);
            String refundApiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

            String responseStr = RestClient.create()
                    .post()
                    .uri(refundApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseStr, Map.class);
            String responseCode = (String) responseMap.getOrDefault("vnp_ResponseCode", "99");
            String message = (String) responseMap.getOrDefault("vnp_Message", "Unknown error");

            log.info("VNPay refund API response: code={}, message={}", responseCode, message);
            return new VNPayRefundResult(responseCode, message);

        } catch (Exception e) {
            log.error("VNPay refund API call failed for txnRef={}: {}", txnRef, e.getMessage());
            return new VNPayRefundResult("99", "Không thể kết nối tới cổng thanh toán: " + e.getMessage());
        }
    }

    public record VNPayRefundResult(String responseCode, String message) {}

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA512", e);
        }
    }
}
