package com.example.ticketbox.repository;

import com.example.ticketbox.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT COALESCE(SUM(oi.unitPrice * oi.quantity), 0) FROM OrderItem oi " +
           "JOIN oi.order o WHERE o.status = 'COMPLETED' AND oi.event.organizer.id = :organizerId")
    BigDecimal sumRevenueByOrganizerId(@Param("organizerId") Long organizerId);

    @Query("SELECT COALESCE(SUM(oi.unitPrice * oi.quantity), 0) FROM OrderItem oi " +
           "JOIN oi.order o WHERE o.status = 'COMPLETED'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(oi.unitPrice * oi.quantity), 0) FROM OrderItem oi " +
           "JOIN oi.order o WHERE o.status = 'COMPLETED' AND oi.event.id = :eventId")
    BigDecimal sumRevenueByEventId(@Param("eventId") Long eventId);
}
