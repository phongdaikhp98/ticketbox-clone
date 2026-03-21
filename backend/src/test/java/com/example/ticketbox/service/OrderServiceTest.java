package com.example.ticketbox.service;

import com.example.ticketbox.dto.CheckoutRequest;
import com.example.ticketbox.dto.OrderResponse;
import com.example.ticketbox.dto.PaymentUrlResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.InsufficientStockException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatReservationService seatReservationService;

    @Mock
    private VNPayService vnPayService;

    @Mock
    private TicketService ticketService;

    @Mock
    private EmailService emailService;

    @Mock
    private PromoCodeService promoCodeService;

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @Mock
    private PromoCodeUsageRepository promoCodeUsageRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Event testEvent;
    private TicketType testTicketType;
    private CartItem testCartItem;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .build();

        testEvent = Event.builder()
                .id(1L)
                .title("Test Event")
                .location("Test Location")
                .eventDate(LocalDateTime.now().plusDays(7))
                .status(EventStatus.PUBLISHED)
                .organizer(testUser)
                .ticketTypes(new ArrayList<>())
                .build();

        testTicketType = TicketType.builder()
                .id(1L)
                .name("VIP")
                .price(new BigDecimal("500000"))
                .capacity(100)
                .soldCount(10)
                .event(testEvent)
                .build();

        testCartItem = CartItem.builder()
                .id(1L)
                .user(testUser)
                .ticketType(testTicketType)
                .quantity(2)
                .createdDate(LocalDateTime.now())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .event(testEvent)
                .ticketType(testTicketType)
                .quantity(2)
                .unitPrice(new BigDecimal("500000"))
                .ticketTypeName("VIP")
                .build();

        testOrder = Order.builder()
                .id(1L)
                .user(testUser)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1000000"))
                .originalAmount(new BigDecimal("1000000"))
                .discountAmount(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentStatus(PaymentStatus.PENDING)
                .orderItems(new ArrayList<>(List.of(orderItem)))
                .createdDate(LocalDateTime.now())
                .build();

        orderItem.setOrder(testOrder);
    }

    @Test
    void checkout_shouldCreateOrder() {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(testCartItem));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse response = orderService.checkout(1L, request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        verify(cartItemRepository).deleteByUserId(1L);
    }

    @Test
    void checkout_shouldThrowWhenCartEmpty() {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> orderService.checkout(1L, request));
    }

    @Test
    void checkout_shouldThrowWhenInsufficientStock() {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        testTicketType.setSoldCount(99);
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(testCartItem));
        assertThrows(InsufficientStockException.class, () -> orderService.checkout(1L, request));
    }

    @Test
    void getMyOrders_shouldReturnPage() {
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));
        when(orderRepository.findByUserIdOrderByCreatedDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(orderPage);
        Page<OrderResponse> result = orderService.getMyOrders(1L, 0, 10);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrderDetail_shouldReturnOrder() {
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));
        OrderResponse response = orderService.getOrderDetail(1L, 1L);
        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getOrderDetail_shouldThrowWhenNotFound() {
        when(orderRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderDetail(1L, 999L));
    }

    @Test
    void createPaymentUrl_shouldReturnPaymentUrl() {
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(vnPayService.createPaymentUrl(anyString(), anyLong(), anyString(), anyString()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?param=value");

        PaymentUrlResponse response = orderService.createPaymentUrl(1L, 1L, "127.0.0.1");

        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
        assertNotNull(response.getPaymentUrl());
        verify(vnPayService).createPaymentUrl(anyString(), eq(1000000L), anyString(), eq("127.0.0.1"));
    }

    @Test
    void createPaymentUrl_shouldThrowWhenNotPending() {
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testOrder));
        assertThrows(BadRequestException.class, () -> orderService.createPaymentUrl(1L, 1L, "127.0.0.1"));
    }

    @Test
    void processVnPayIpn_shouldCompleteOrderOnSuccess() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "1_123456");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "VNP123");
        params.put("vnp_SecureHash", "abc");

        when(vnPayService.validateSignature(params)).thenReturn(true);
        when(orderRepository.findByVnpayTxnRef("1_123456")).thenReturn(Optional.of(testOrder));
        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));
        when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(ticketService.generateTickets(any(Order.class))).thenReturn(Collections.emptyList());
        doNothing().when(emailService).sendPaymentSuccessEmail(anyLong());
        doNothing().when(emailService).sendAdminOrderNotification(anyLong());

        Map<String, String> result = orderService.processVnPayIpn(params);

        assertEquals("00", result.get("RspCode"));
        assertEquals(OrderStatus.COMPLETED, testOrder.getStatus());
        assertEquals(PaymentStatus.SUCCESS, testOrder.getPaymentStatus());
        verify(ticketService).generateTickets(testOrder);
        verify(emailService).sendPaymentSuccessEmail(1L);
    }

    @Test
    void processVnPayIpn_shouldRejectInvalidSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_SecureHash", "invalid");
        when(vnPayService.validateSignature(params)).thenReturn(false);
        Map<String, String> result = orderService.processVnPayIpn(params);
        assertEquals("97", result.get("RspCode"));
    }

    @Test
    void cancelOrder_success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        assertDoesNotThrow(() -> orderService.cancelOrder(1L, 1L));
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    void cancelOrder_wrongUser_throwsException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        assertThrows(BadRequestException.class, () -> orderService.cancelOrder(1L, 999L));
    }

    @Test
    void cancelOrder_notPending_throwsException() {
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        assertThrows(BadRequestException.class, () -> orderService.cancelOrder(1L, 1L));
    }
}
