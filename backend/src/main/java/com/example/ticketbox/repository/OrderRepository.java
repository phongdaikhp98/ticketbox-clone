package com.example.ticketbox.repository;

import com.example.ticketbox.model.Order;
import com.example.ticketbox.model.OrderStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByCreatedDateDesc(Long userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByVnpayTxnRef(String vnpayTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT o FROM Order o WHERE o.vnpayTxnRef = :txnRef")
    Optional<Order> findByVnpayTxnRefForUpdate(@Param("txnRef") String txnRef);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi " +
           "WHERE o.status = 'COMPLETED' AND oi.event.organizer.id = :organizerId " +
           "ORDER BY o.createdDate DESC")
    Page<Order> findRecentCompletedOrdersByOrganizerId(@Param("organizerId") Long organizerId, Pageable pageable);

    @Query(value = "SELECT o FROM Order o JOIN FETCH o.user WHERE " +
                   "(:status IS NULL OR o.status = :status) AND " +
                   "(:search IS NULL OR LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                   "OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o JOIN o.user WHERE " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:search IS NULL OR LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> findAllWithFilters(@Param("status") OrderStatus status,
                                    @Param("search") String search,
                                    Pageable pageable);

    Long countByStatus(OrderStatus status);
}
