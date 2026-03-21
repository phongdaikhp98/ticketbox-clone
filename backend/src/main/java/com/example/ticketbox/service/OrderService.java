package com.example.ticketbox.service;

import com.example.ticketbox.dto.CheckoutRequest;
import com.example.ticketbox.dto.OrderResponse;
import com.example.ticketbox.dto.PaymentUrlResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.InsufficientStockException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.CartItemRepository;
import com.example.ticketbox.repository.OrderRepository;
import com.example.ticketbox.repository.PromoCodeRepository;
import com.example.ticketbox.repository.PromoCodeUsageRepository;
import com.example.ticketbox.repository.SeatRepository;
import com.example.ticketbox.repository.TicketTypeRepository;
import com.example.ticketbox.repository.UserRepository;
import org.springframework.util.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final SeatReservationService seatReservationService;
    private final VNPayService vnPayService;
    private final TicketService ticketService;
    private final EmailService emailService;
    private final PromoCodeService promoCodeService;
    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;

    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Validate stock availability
        for (CartItem item : cartItems) {
            TicketType tt = item.getTicketType();
            int available = tt.getCapacity() - tt.getSoldCount();
            if (item.getQuantity() > available) {
                throw new InsufficientStockException(
                        "Insufficient stock for '" + tt.getName() + "'. Available: " + available + ", Requested: " + item.getQuantity());
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Create order
        Order order = Order.builder()
                .user(user)
                .paymentMethod(request.getPaymentMethod())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            TicketType tt = cartItem.getTicketType();
            BigDecimal lineTotal = tt.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .event(tt.getEvent())
                    .ticketType(tt)
                    .seat(cartItem.getSeat())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(tt.getPrice())
                    .ticketTypeName(tt.getName())
                    .build();
            orderItems.add(orderItem);
        }

        // Apply promo code if provided
        BigDecimal subtotal = totalAmount;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCode = null;
        PromoCode promoCodeEntity = null;

        if (StringUtils.hasText(request.getPromoCode())) {
            promoCodeEntity = promoCodeService.getValidPromoCode(request.getPromoCode(), userId, subtotal);
            discountAmount = promoCodeService.calculateDiscount(promoCodeEntity, subtotal);
            totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
            appliedCode = promoCodeEntity.getCode().toUpperCase();
            promoCodeEntity.setUsedCount(promoCodeEntity.getUsedCount() + 1);
            promoCodeRepository.save(promoCodeEntity);
        }

        order.setOriginalAmount(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setPromoCode(appliedCode);
        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        Order saved = orderRepository.save(order);

        // Save promo code usage record
        if (promoCodeEntity != null) {
            PromoCodeUsage usage = PromoCodeUsage.builder()
                    .promoCode(promoCodeEntity)
                    .user(user)
                    .order(saved)
                    .build();
            promoCodeUsageRepository.save(usage);
        }

        // Clear cart
        cartItemRepository.deleteByUserId(userId);

        return toOrderResponse(saved);
    }

    public PaymentUrlResponse createPaymentUrl(Long userId, Long orderId, String clientIp) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in PENDING status");
        }

        String txnRef = orderId + "_" + System.currentTimeMillis();
        order.setVnpayTxnRef(txnRef);
        orderRepository.save(order);

        long amount = order.getTotalAmount().longValue();
        String orderInfo = "Thanh toan don hang #" + orderId;

        String paymentUrl = vnPayService.createPaymentUrl(txnRef, amount, orderInfo, clientIp);

        return PaymentUrlResponse.builder()
                .orderId(orderId)
                .paymentUrl(paymentUrl)
                .build();
    }

    @Transactional
    public Map<String, String> processVnPayIpn(Map<String, String> params) {
        if (!vnPayService.validateSignature(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid signature");
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        Order order = orderRepository.findByVnpayTxnRef(txnRef).orElse(null);
        if (order == null) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            return Map.of("RspCode", "02", "Message", "Order already processed");
        }

        if (!"00".equals(responseCode)) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setVnpayTransactionNo(transactionNo);
            orderRepository.save(order);
            emailService.sendPaymentFailedEmail(order.getId());
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }

        // Payment success - increment soldCount
        for (OrderItem item : order.getOrderItems()) {
            TicketType tt = ticketTypeRepository.findById(item.getTicketType().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("TicketType", item.getTicketType().getId()));

            int available = tt.getCapacity() - tt.getSoldCount();
            if (item.getQuantity() > available) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setVnpayTransactionNo(transactionNo);
                orderRepository.save(order);
                return Map.of("RspCode", "04", "Message", "Insufficient stock");
            }

            tt.setSoldCount(tt.getSoldCount() + item.getQuantity());
            try {
                ticketTypeRepository.save(tt);
            } catch (ObjectOptimisticLockingFailureException e) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setVnpayTransactionNo(transactionNo);
                orderRepository.save(order);
                return Map.of("RspCode", "04", "Message", "Stock conflict");
            }
        }

        // Update seat status to SOLD for seat-based items
        for (OrderItem item : order.getOrderItems()) {
            if (item.getSeat() != null) {
                Seat seat = seatRepository.findById(item.getSeat().getId()).orElse(null);
                if (seat != null) {
                    seat.setStatus(SeatStatus.SOLD);
                    seatRepository.save(seat);
                    seatReservationService.releaseSeat(seat.getId());
                }
            }
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setVnpayTransactionNo(transactionNo);
        order.setVnpayTransactionDate(params.get("vnp_PayDate"));
        orderRepository.save(order);

        // Generate tickets after successful payment
        ticketService.generateTickets(order);
        emailService.sendPaymentSuccessEmail(order.getId());
        emailService.sendAdminOrderNotification(order.getId());

        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(userId))
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này");

        if (order.getStatus() != OrderStatus.PENDING)
            throw new BadRequestException("Chỉ có thể hủy đơn hàng chưa thanh toán (PENDING)");

        LocalDateTime cutoff = LocalDateTime.now().plusHours(24);
        for (OrderItem item : order.getOrderItems()) {
            LocalDateTime eventDate = item.getEvent().getEventDate();
            if (eventDate != null && eventDate.isBefore(cutoff)) {
                throw new BadRequestException(
                        "Không thể hủy: sự kiện '" + item.getEvent().getTitle() + "' bắt đầu trong vòng 24 giờ");
            }
        }

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

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public Page<OrderResponse> getMyOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable)
                .map(this::toOrderResponse);
    }

    public OrderResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return toOrderResponse(order);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getOrderItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .originalAmount(order.getOriginalAmount())
                .discountAmount(order.getDiscountAmount())
                .promoCode(order.getPromoCode())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentStatus(order.getPaymentStatus().name())
                .vnpayTxnRef(order.getVnpayTxnRef())
                .orderItems(items)
                .createdDate(order.getCreatedDate())
                .updatedDate(order.getUpdatedDate())
                .build();
    }

    private OrderResponse.OrderItemResponse toOrderItemResponse(OrderItem item) {
        Event event = item.getEvent();
        return OrderResponse.OrderItemResponse.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .ticketTypeName(item.getTicketTypeName())
                .event(OrderResponse.EventSummary.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(event.getImageUrl())
                        .eventDate(event.getEventDate())
                        .location(event.getLocation())
                        .build())
                .build();
    }
}
