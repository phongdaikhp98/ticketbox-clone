package com.example.ticketbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.vnpay")
@Getter
@Setter
public class VNPayConfig {

    private String tmnCode;
    private String hashSecret;
    private String paymentUrl;
    private String returnUrl;
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    /** Payment URL expiration in minutes (passed as vnp_ExpireDate). */
    private int paymentExpirationMinutes = 15;
    /** VNPay refund API endpoint. */
    private String refundApiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
}
