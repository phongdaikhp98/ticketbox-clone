package com.example.ticketbox.service;

import com.example.ticketbox.dto.*;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.CartItemRepository;
import com.example.ticketbox.repository.TicketTypeRepository;
import com.example.ticketbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Event testEvent;
    private TicketType testTicketType;
    private CartItem testCartItem;

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
                .category(EventCategory.MUSIC)
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
    }

    @Test
    void getCart_shouldReturnCartWithItems() {
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of(testCartItem));

        CartResponse response = cartService.getCart(1L);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getTotalItems());
        assertEquals(new BigDecimal("1000000"), response.getTotalAmount());
    }

    @Test
    void getCart_shouldReturnEmptyCart() {
        when(cartItemRepository.findByUserId(1L)).thenReturn(List.of());

        CartResponse response = cartService.getCart(1L);

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
        assertEquals(0, response.getTotalItems());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
    }

    @Test
    void addToCart_shouldAddNewItem() {
        AddToCartRequest request = new AddToCartRequest();
        request.setTicketTypeId(1L);
        request.setQuantity(2);

        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));
        when(cartItemRepository.findByUserIdAndTicketTypeId(1L, 1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(testCartItem);

        CartItemResponse response = cartService.addToCart(1L, request);

        assertNotNull(response);
        assertEquals(2, response.getQuantity());
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addToCart_shouldUpdateExistingItem() {
        AddToCartRequest request = new AddToCartRequest();
        request.setTicketTypeId(1L);
        request.setQuantity(3);

        CartItem existingItem = CartItem.builder()
                .id(1L).user(testUser).ticketType(testTicketType).quantity(2)
                .createdDate(LocalDateTime.now()).build();

        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));
        when(cartItemRepository.findByUserIdAndTicketTypeId(1L, 1L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(existingItem);

        cartService.addToCart(1L, request);

        assertEquals(5, existingItem.getQuantity());
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addToCart_shouldThrowWhenInsufficientStock() {
        AddToCartRequest request = new AddToCartRequest();
        request.setTicketTypeId(1L);
        request.setQuantity(100); // only 90 available

        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));

        assertThrows(BadRequestException.class, () -> cartService.addToCart(1L, request));
    }

    @Test
    void addToCart_shouldThrowWhenTicketTypeNotFound() {
        AddToCartRequest request = new AddToCartRequest();
        request.setTicketTypeId(999L);
        request.setQuantity(1);

        when(ticketTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.addToCart(1L, request));
    }

    @Test
    void updateCartItem_shouldUpdateQuantity() {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        when(cartItemRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(testCartItem);

        CartItemResponse response = cartService.updateCartItem(1L, 1L, request);

        assertNotNull(response);
        verify(cartItemRepository).save(testCartItem);
    }

    @Test
    void updateCartItem_shouldThrowWhenNotFound() {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        when(cartItemRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.updateCartItem(1L, 999L, request));
    }

    @Test
    void removeCartItem_shouldDeleteItem() {
        when(cartItemRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testCartItem));

        cartService.removeCartItem(1L, 1L);

        verify(cartItemRepository).delete(testCartItem);
    }

    @Test
    void clearCart_shouldDeleteAllItems() {
        cartService.clearCart(1L);

        verify(cartItemRepository).deleteByUserId(1L);
    }
}
