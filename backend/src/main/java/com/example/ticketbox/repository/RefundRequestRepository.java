package com.example.ticketbox.repository;

import com.example.ticketbox.model.RefundRequest;
import com.example.ticketbox.model.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Optional<RefundRequest> findByOrderId(Long orderId);

    boolean existsByOrderIdAndStatusNot(Long orderId, RefundStatus status);

    @Query(value = "SELECT r FROM RefundRequest r JOIN FETCH r.order o JOIN FETCH o.user " +
                   "WHERE (:status IS NULL OR r.status = :status)",
           countQuery = "SELECT COUNT(r) FROM RefundRequest r WHERE (:status IS NULL OR r.status = :status)")
    Page<RefundRequest> findAllWithFilter(@Param("status") RefundStatus status, Pageable pageable);
}
