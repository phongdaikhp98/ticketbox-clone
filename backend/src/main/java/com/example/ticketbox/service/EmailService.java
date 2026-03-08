package com.example.ticketbox.service;

import com.example.ticketbox.model.Order;
import com.example.ticketbox.model.OrderItem;
import com.example.ticketbox.repository.OrderRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final OrderRepository orderRepository;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.frontend-url}")
    private String frontendUrl;

    @Async("emailExecutor")
    @Transactional(readOnly = true)
    public void sendPaymentSuccessEmail(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("sendPaymentSuccessEmail: Order {} not found", orderId);
                return;
            }

            Context ctx = new Context();
            ctx.setVariable("orderId", order.getId());
            ctx.setVariable("userName", order.getUser().getFullName());
            ctx.setVariable("userEmail", order.getUser().getEmail());
            ctx.setVariable("totalAmount", formatCurrency(order.getTotalAmount().longValue()));
            ctx.setVariable("orderItems", buildItemList(order.getOrderItems()));
            ctx.setVariable("ticketsUrl", frontendUrl + "/tickets");
            ctx.setVariable("createdDate", formatDate(order));

            String html = templateEngine.process("email/payment-success", ctx);
            String subject = "✅ Thanh toán thành công - Đơn hàng #" + orderId;
            sendHtmlEmail(order.getUser().getEmail(), subject, html);
            log.info("Payment success email sent to {} for order #{}", order.getUser().getEmail(), orderId);
        } catch (Exception e) {
            log.warn("Failed to send payment success email for order #{}: {}", orderId, e.getMessage());
        }
    }

    @Async("emailExecutor")
    @Transactional(readOnly = true)
    public void sendPaymentFailedEmail(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("sendPaymentFailedEmail: Order {} not found", orderId);
                return;
            }

            Context ctx = new Context();
            ctx.setVariable("orderId", order.getId());
            ctx.setVariable("userName", order.getUser().getFullName());
            ctx.setVariable("userEmail", order.getUser().getEmail());
            ctx.setVariable("totalAmount", formatCurrency(order.getTotalAmount().longValue()));
            ctx.setVariable("retryUrl", frontendUrl + "/orders/" + orderId);

            String html = templateEngine.process("email/payment-failed", ctx);
            String subject = "❌ Thanh toán thất bại - Đơn hàng #" + orderId;
            sendHtmlEmail(order.getUser().getEmail(), subject, html);
            log.info("Payment failed email sent to {} for order #{}", order.getUser().getEmail(), orderId);
        } catch (Exception e) {
            log.warn("Failed to send payment failed email for order #{}: {}", orderId, e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String formatCurrency(long amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " ₫";
    }

    private String formatDate(Order order) {
        if (order.getCreatedDate() == null) return "";
        return order.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private List<EmailOrderItem> buildItemList(List<OrderItem> items) {
        return items.stream()
                .map(item -> new EmailOrderItem(
                        item.getEvent().getTitle(),
                        item.getTicketTypeName(),
                        item.getQuantity(),
                        formatCurrency(item.getUnitPrice().longValue()),
                        formatCurrency(item.getUnitPrice().multiply(
                                java.math.BigDecimal.valueOf(item.getQuantity())).longValue())
                ))
                .toList();
    }

    public record EmailOrderItem(
            String eventTitle,
            String ticketTypeName,
            int quantity,
            String unitPrice,
            String lineTotal
    ) {}
}
