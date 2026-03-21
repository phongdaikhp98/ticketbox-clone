package com.example.ticketbox.service;

import com.example.ticketbox.dto.RefundResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SeatRepository seatRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final VNPayService vnPayService;
    private final TicketService ticketService;
    private final SeatReservationService seatReservationService;
    private final EmailService emailService;

    @Transactional
    public RefundResponse requestRefund(Long userId, Long orderId, String clientIp) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Validate order is paid and completed
        if (order.getStatus() != OrderStatus.COMPLETED || order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Chỉ có thể hoàn tiền đơn hàng đã thanh toán thành công");
        }

        // Validate VNPay payment info exists
        if (order.getVnpayTxnRef() == null || order.getVnpayTransactionNo() == null) {
            throw new BadRequestException("Không tìm thấy thông tin giao dịch VNPay cho đơn hàng này");
        }

        // Validate all events are > 24h away
        LocalDateTime cutoff = LocalDateTime.now().plusHours(24);
        for (OrderItem item : order.getOrderItems()) {
            LocalDateTime eventDate = item.getEvent().getEventDate();
            if (eventDate != null && eventDate.isBefore(cutoff)) {
                throw new BadRequestException(
                        "Không thể hoàn tiền: sự kiện '" + item.getEvent().getTitle()
                                + "' bắt đầu trong vòng 24 giờ");
            }
        }

        // Validate no active refund request (ignore FAILED ones — allow retry)
        if (refundRequestRepository.existsByOrderIdAndStatusNot(orderId, RefundStatus.FAILED)) {
            RefundRequest existing = refundRequestRepository.findByOrderId(orderId).orElse(null);
            String status = existing != null ? existing.getStatus().name() : "UNKNOWN";
            throw new BadRequestException("Đơn hàng này đã có yêu cầu hoàn tiền (trạng thái: " + status + ")");
        }

        // Generate unique request ID (max 32 chars for VNPay)
        String vnpayRequestId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        // Reuse existing FAILED record (unique constraint per order) or create new
        RefundRequest refundRequest = refundRequestRepository.findByOrderId(orderId)
                .orElseGet(() -> RefundRequest.builder()
                        .order(order)
                        .amount(order.getTotalAmount())
                        .build());

        refundRequest.setStatus(RefundStatus.PROCESSING);
        refundRequest.setVnpayRequestId(vnpayRequestId);
        refundRequest.setAmount(order.getTotalAmount());
        refundRequest.setVnpayResponseCode(null);
        refundRequest.setVnpayResponseMessage(null);
        refundRequestRepository.save(refundRequest);

        // Call VNPay refund API
        VNPayService.VNPayRefundResult result = vnPayService.callRefundApi(
                order.getVnpayTxnRef(),
                order.getVnpayTransactionNo(),
                order.getVnpayTransactionDate(),
                order.getTotalAmount().longValue(),
                vnpayRequestId,
                clientIp
        );

        refundRequest.setVnpayResponseCode(result.responseCode());
        refundRequest.setVnpayResponseMessage(result.message());

        if ("00".equals(result.responseCode())) {
            // Refund approved — execute all side effects
            refundRequest.setStatus(RefundStatus.COMPLETED);
            executeRefundSideEffects(order);
            emailService.sendRefundSuccessEmail(orderId);
            log.info("Refund COMPLETED for order #{} — txnRef={}", orderId, order.getVnpayTxnRef());
        } else {
            refundRequest.setStatus(RefundStatus.FAILED);
            emailService.sendRefundFailedEmail(orderId, result.message());
            log.warn("Refund FAILED for order #{}: code={}, message={}", orderId, result.responseCode(), result.message());
        }

        refundRequestRepository.save(refundRequest);
        return toResponse(refundRequest);
    }

    private void executeRefundSideEffects(Order order) {
        // 1. Cancel all tickets
        ticketService.cancelTicketsByOrderId(order.getId());

        // 2. Decrement soldCount for each ticket type
        for (OrderItem item : order.getOrderItems()) {
            TicketType tt = ticketTypeRepository.findById(item.getTicketType().getId()).orElse(null);
            if (tt != null) {
                tt.setSoldCount(Math.max(0, tt.getSoldCount() - item.getQuantity()));
                ticketTypeRepository.save(tt);
            }
        }

        // 3. Release seats (set AVAILABLE + clear Redis reservation)
        for (OrderItem item : order.getOrderItems()) {
            if (item.getSeat() != null) {
                Seat seat = seatRepository.findById(item.getSeat().getId()).orElse(null);
                if (seat != null) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatRepository.save(seat);
                }
                seatReservationService.releaseSeat(item.getSeat().getId());
            }
        }

        // 4. Decrement promo code usage if applicable
        if (order.getPromoCode() != null) {
            promoCodeRepository.findByCodeIgnoreCase(order.getPromoCode()).ifPresent(promo -> {
                promo.setUsedCount(Math.max(0, promo.getUsedCount() - 1));
                promoCodeRepository.save(promo);
            });
            promoCodeUsageRepository.findByOrderId(order.getId())
                    .ifPresent(promoCodeUsageRepository::delete);
        }

        // 5. Update order status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public Optional<RefundResponse> getRefundByOrderId(Long userId, Long orderId) {
        // Validate order belongs to user
        orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return refundRequestRepository.findByOrderId(orderId).map(this::toResponse);
    }

    // Admin: view all refund requests
    public Page<RefundResponse> getAllRefunds(RefundStatus status, Pageable pageable) {
        return refundRequestRepository.findAllWithFilter(status, pageable).map(this::toResponse);
    }

    private RefundResponse toResponse(RefundRequest r) {
        return RefundResponse.builder()
                .id(r.getId())
                .orderId(r.getOrder().getId())
                .amount(r.getAmount())
                .status(r.getStatus().name())
                .vnpayRequestId(r.getVnpayRequestId())
                .vnpayResponseCode(r.getVnpayResponseCode())
                .vnpayResponseMessage(r.getVnpayResponseMessage())
                .createdDate(r.getCreatedDate())
                .updatedDate(r.getUpdatedDate())
                .build();
    }
}
