package com.example.ticketbox.service;

import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.OrderRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private SpringTemplateEngine templateEngine;
    @Mock private OrderRepository orderRepository;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private User user;
    private Event event;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@ticketbox.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Ticketbox");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(emailService, "adminEmail", "admin@ticketbox.com");

        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("Nguyen Van A")
                .role(Role.CUSTOMER)
                .build();

        event = Event.builder()
                .id(10L)
                .title("Rock Concert 2026")
                .build();

        orderItem = OrderItem.builder()
                .id(1L)
                .event(event)
                .ticketTypeName("VIP")
                .quantity(2)
                .unitPrice(new BigDecimal("500000"))
                .build();

        order = Order.builder()
                .id(100L)
                .user(user)
                .totalAmount(new BigDecimal("1000000"))
                .originalAmount(new BigDecimal("1000000"))
                .discountAmount(BigDecimal.ZERO)
                .orderItems(List.of(orderItem))
                .createdDate(LocalDateTime.of(2026, 3, 21, 10, 0))
                .build();

        orderItem.setOrder(order);
    }

    // ===================== sendPaymentSuccessEmail =====================

    @Test
    void sendPaymentSuccessEmail_shouldSendEmail() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(templateEngine.process(eq("email/payment-success"), any(Context.class)))
                .thenReturn("<html>success</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPaymentSuccessEmail(100L);

        verify(templateEngine).process(eq("email/payment-success"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPaymentSuccessEmail_shouldDoNothingWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        emailService.sendPaymentSuccessEmail(999L);

        verify(mailSender, never()).createMimeMessage();
        verify(templateEngine, never()).process(anyString(), any(Context.class));
    }

    // ===================== sendPaymentFailedEmail =====================

    @Test
    void sendPaymentFailedEmail_shouldSendEmail() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(templateEngine.process(eq("email/payment-failed"), any(Context.class)))
                .thenReturn("<html>failed</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPaymentFailedEmail(100L);

        verify(templateEngine).process(eq("email/payment-failed"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPaymentFailedEmail_shouldDoNothingWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        emailService.sendPaymentFailedEmail(999L);

        verify(mailSender, never()).createMimeMessage();
    }

    // ===================== sendAdminOrderNotification =====================

    @Test
    void sendAdminOrderNotification_shouldSendEmail() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(templateEngine.process(eq("email/admin-order-notification"), any(Context.class)))
                .thenReturn("<html>admin</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendAdminOrderNotification(100L);

        verify(templateEngine).process(eq("email/admin-order-notification"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendAdminOrderNotification_shouldSkipWhenAdminEmailBlank() {
        ReflectionTestUtils.setField(emailService, "adminEmail", "");

        emailService.sendAdminOrderNotification(100L);

        verify(orderRepository, never()).findById(anyLong());
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendAdminOrderNotification_shouldSkipWhenAdminEmailNull() {
        ReflectionTestUtils.setField(emailService, "adminEmail", null);

        emailService.sendAdminOrderNotification(100L);

        verify(orderRepository, never()).findById(anyLong());
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void sendAdminOrderNotification_shouldDoNothingWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        emailService.sendAdminOrderNotification(999L);

        verify(mailSender, never()).createMimeMessage();
    }

    // ===================== sendPaymentSuccessEmail — exception handling =====================

    @Test
    void sendPaymentSuccessEmail_shouldNotThrowWhenMailFails() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        // Should swallow exception gracefully
        assertDoesNotThrow(() -> emailService.sendPaymentSuccessEmail(100L));
    }
}
