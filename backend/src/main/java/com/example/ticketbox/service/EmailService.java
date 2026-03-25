package com.example.ticketbox.service;

import com.example.ticketbox.model.Order;
import com.example.ticketbox.model.OrderItem;
import com.example.ticketbox.model.Ticket;
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
import java.util.Optional;

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

    @Value("${app.mail.admin-email:}")
    private String adminEmail;

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

    @Async("emailExecutor")
    @Transactional(readOnly = true)
    public void sendAdminOrderNotification(Long orderId) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.debug("ADMIN_EMAIL not configured, skipping admin notification for order #{}", orderId);
            return;
        }
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("sendAdminOrderNotification: Order {} not found", orderId);
                return;
            }

            Context ctx = new Context();
            ctx.setVariable("orderId", order.getId());
            ctx.setVariable("userName", order.getUser().getFullName());
            ctx.setVariable("userEmail", order.getUser().getEmail());
            ctx.setVariable("totalAmount", formatCurrency(order.getTotalAmount().longValue()));
            ctx.setVariable("orderItems", buildItemList(order.getOrderItems()));
            ctx.setVariable("orderDetailUrl", frontendUrl + "/admin/orders/" + orderId);
            ctx.setVariable("createdDate", formatDate(order));
            ctx.setVariable("promoCode", order.getPromoCode());
            ctx.setVariable("discountAmount",
                    order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                            ? formatCurrency(order.getDiscountAmount().longValue()) : null);
            ctx.setVariable("originalAmount",
                    order.getOriginalAmount() != null
                            ? formatCurrency(order.getOriginalAmount().longValue()) : null);

            String html = templateEngine.process("email/admin-order-notification", ctx);
            String subject = "🛒 Đơn hàng mới #" + orderId + " — " + order.getUser().getFullName();
            sendHtmlEmail(adminEmail, subject, html);
            log.info("Admin order notification sent to {} for order #{}", adminEmail, orderId);
        } catch (Exception e) {
            log.warn("Failed to send admin order notification for order #{}: {}", orderId, e.getMessage());
        }
    }

    public void sendEventReminderEmail(Ticket ticket) {
        try {
            String eventDateFormatted = ticket.getEvent().getEventDate() != null
                    ? ticket.getEvent().getEventDate().format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"))
                    : "";

            Context ctx = new Context();
            ctx.setVariable("userName", ticket.getUser().getFullName());
            ctx.setVariable("eventTitle", ticket.getEvent().getTitle());
            ctx.setVariable("eventDate", eventDateFormatted);
            ctx.setVariable("eventLocation", Optional.ofNullable(ticket.getEvent().getLocation()).orElse(""));
            ctx.setVariable("ticketCode", ticket.getTicketCode());
            ctx.setVariable("seatCode", ticket.getSeatCode());
            ctx.setVariable("ticketTypeName", ticket.getTicketType().getName());
            ctx.setVariable("ticketUrl", frontendUrl + "/tickets/" + ticket.getId());

            String html = templateEngine.process("email/event-reminder", ctx);
            String subject = "Nhắc nhở: Sự kiện \"" + ticket.getEvent().getTitle() + "\" diễn ra vào ngày mai!";
            sendHtmlEmail(ticket.getUser().getEmail(), subject, html);
            log.info("Event reminder email sent to {} for ticket #{}", ticket.getUser().getEmail(), ticket.getId());
        } catch (Exception e) {
            log.warn("Failed to send event reminder email for ticket #{}: {}", ticket.getId(), e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendOrganizerApprovalEmail(String toEmail, String userName, String orgName) {
        try {
            Context ctx = new Context();
            ctx.setVariable("userName", userName);
            ctx.setVariable("orgName", orgName);
            ctx.setVariable("dashboardUrl", frontendUrl + "/organizer/dashboard");

            String html = templateEngine.process("email/organizer-approved", ctx);
            String subject = "Chúc mừng! Đơn đăng ký Organizer của bạn đã được duyệt";
            sendHtmlEmail(toEmail, subject, html);
            log.info("Organizer approval email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send organizer approval email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendOrganizerRejectionEmail(String toEmail, String userName, String orgName, String reviewNote) {
        try {
            Context ctx = new Context();
            ctx.setVariable("userName", userName);
            ctx.setVariable("orgName", orgName);
            ctx.setVariable("reviewNote", reviewNote);
            ctx.setVariable("reapplyUrl", frontendUrl + "/organizer-applications");

            String html = templateEngine.process("email/organizer-rejected", ctx);
            String subject = "Thông báo: Đơn đăng ký Organizer của bạn chưa được chấp thuận";
            sendHtmlEmail(toEmail, subject, html);
            log.info("Organizer rejection email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send organizer rejection email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendPasswordResetEmail(String email, String name, String resetUrl) {
        try {
            Context ctx = new Context();
            ctx.setVariable("userName", name);
            ctx.setVariable("resetUrl", resetUrl);
            ctx.setVariable("expireMinutes", 15);

            String html = templateEngine.process("email/reset-password", ctx);
            String subject = "🔑 Đặt lại mật khẩu Ticketbox";
            sendHtmlEmail(email, subject, html);
            log.info("Password reset email sent to {}", email);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendEmailVerificationEmail(String email, String name, String verifyUrl) {
        try {
            Context ctx = new Context();
            ctx.setVariable("userName", name);
            ctx.setVariable("verifyUrl", verifyUrl);
            ctx.setVariable("expireMinutes", 60);

            String html = templateEngine.process("email/verify-email", ctx);
            String subject = "✉️ Xác thực email Ticketbox";
            sendHtmlEmail(email, subject, html);
            log.info("Email verification sent to {}", email);
        } catch (Exception e) {
            log.warn("Failed to send email verification to {}: {}", email, e.getMessage());
        }
    }

    @Async("emailExecutor")
    @Transactional(readOnly = true)
    public void sendRefundSuccessEmail(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("sendRefundSuccessEmail: Order {} not found", orderId);
                return;
            }

            Context ctx = new Context();
            ctx.setVariable("orderId", order.getId());
            ctx.setVariable("userName", order.getUser().getFullName());
            ctx.setVariable("totalAmount", formatCurrency(order.getTotalAmount().longValue()));
            ctx.setVariable("orderItems", buildItemList(order.getOrderItems()));
            ctx.setVariable("ordersUrl", frontendUrl + "/orders");

            String html = templateEngine.process("email/refund-success", ctx);
            String subject = "💸 Hoàn tiền thành công - Đơn hàng #" + orderId;
            sendHtmlEmail(order.getUser().getEmail(), subject, html);
            log.info("Refund success email sent to {} for order #{}", order.getUser().getEmail(), orderId);
        } catch (Exception e) {
            log.warn("Failed to send refund success email for order #{}: {}", orderId, e.getMessage());
        }
    }

    @Async("emailExecutor")
    @Transactional(readOnly = true)
    public void sendRefundFailedEmail(Long orderId, String reason) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("sendRefundFailedEmail: Order {} not found", orderId);
                return;
            }

            Context ctx = new Context();
            ctx.setVariable("orderId", order.getId());
            ctx.setVariable("userName", order.getUser().getFullName());
            ctx.setVariable("totalAmount", formatCurrency(order.getTotalAmount().longValue()));
            ctx.setVariable("reason", reason);
            ctx.setVariable("orderDetailUrl", frontendUrl + "/orders/" + orderId);

            String html = templateEngine.process("email/refund-failed", ctx);
            String subject = "❌ Hoàn tiền thất bại - Đơn hàng #" + orderId;
            sendHtmlEmail(order.getUser().getEmail(), subject, html);
            log.info("Refund failed email sent to {} for order #{}", order.getUser().getEmail(), orderId);
        } catch (Exception e) {
            log.warn("Failed to send refund failed email for order #{}: {}", orderId, e.getMessage());
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
