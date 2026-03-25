package com.example.ticketbox.repository;

import com.example.ticketbox.model.TicketTransfer;
import com.example.ticketbox.model.TicketTransferStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTransferRepository extends JpaRepository<TicketTransfer, Long> {

    Optional<TicketTransfer> findByTransferToken(String token);

    List<TicketTransfer> findByFromUserIdOrderByCreatedDateDesc(Long fromUserId);

    boolean existsByTicketIdAndStatus(Long ticketId, TicketTransferStatus status);

    // [SECURITY] Pessimistic locks prevent race condition between accept and cancel (H1)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT t FROM TicketTransfer t WHERE t.transferToken = :token")
    Optional<TicketTransfer> findByTransferTokenForUpdate(@Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT t FROM TicketTransfer t WHERE t.id = :id")
    Optional<TicketTransfer> findByIdForUpdate(@Param("id") Long id);
}
