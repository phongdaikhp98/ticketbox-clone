package com.example.ticketbox.service;

import com.example.ticketbox.dto.AddToCartRequest;
import com.example.ticketbox.dto.CartItemResponse;
import com.example.ticketbox.dto.CartResponse;
import com.example.ticketbox.dto.UpdateCartItemRequest;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.CartItem;
import com.example.ticketbox.model.Event;
import com.example.ticketbox.model.TicketType;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.CartItemRepository;
import com.example.ticketbox.repository.TicketTypeRepository;
import com.example.ticketbox.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;

    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toCartItemResponse)
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(item -> item.getTicketType().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();

        return CartResponse.builder()
                .items(itemResponses)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }

    @Transactional
    public CartItemResponse addToCart(Long userId, AddToCartRequest request) {
        TicketType ticketType = ticketTypeRepository.findById(request.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("TicketType", request.getTicketTypeId()));

        int available = ticketType.getCapacity() - ticketType.getSoldCount();
        if (request.getQuantity() > available) {
            throw new BadRequestException("Only " + available + " tickets available");
        }

        Optional<CartItem> existing = cartItemRepository.findByUserIdAndTicketTypeId(userId, request.getTicketTypeId());
        if (existing.isPresent()) {
            CartItem cartItem = existing.get();
            int newQty = cartItem.getQuantity() + request.getQuantity();
            if (newQty > available) {
                throw new BadRequestException("Only " + available + " tickets available. You already have " + cartItem.getQuantity() + " in cart");
            }
            cartItem.setQuantity(newQty);
            return toCartItemResponse(cartItemRepository.save(cartItem));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        CartItem cartItem = CartItem.builder()
                .user(user)
                .ticketType(ticketType)
                .quantity(request.getQuantity())
                .build();

        return toCartItemResponse(cartItemRepository.save(cartItem));
    }

    @Transactional
    public CartItemResponse updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));

        TicketType ticketType = cartItem.getTicketType();
        int available = ticketType.getCapacity() - ticketType.getSoldCount();
        if (request.getQuantity() > available) {
            throw new BadRequestException("Only " + available + " tickets available");
        }

        cartItem.setQuantity(request.getQuantity());
        return toCartItemResponse(cartItemRepository.save(cartItem));
    }

    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));
        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        TicketType tt = item.getTicketType();
        Event event = tt.getEvent();

        return CartItemResponse.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .ticketType(CartItemResponse.TicketTypeSummary.builder()
                        .id(tt.getId())
                        .name(tt.getName())
                        .price(tt.getPrice())
                        .availableCount(tt.getCapacity() - tt.getSoldCount())
                        .build())
                .event(CartItemResponse.EventSummary.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .imageUrl(event.getImageUrl())
                        .eventDate(event.getEventDate())
                        .location(event.getLocation())
                        .build())
                .createdDate(item.getCreatedDate())
                .build();
    }
}
