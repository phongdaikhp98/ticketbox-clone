package com.example.ticketbox.repository;

import com.example.ticketbox.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndTicketTypeId(Long userId, Long ticketTypeId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);

    Optional<CartItem> findByUserIdAndSeatId(Long userId, Long seatId);

    boolean existsByUserIdAndSeatId(Long userId, Long seatId);
}
