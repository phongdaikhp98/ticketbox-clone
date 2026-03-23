package com.example.ticketbox.repository;

import com.example.ticketbox.model.TicketTransfer;
import com.example.ticketbox.model.TicketTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTransferRepository extends JpaRepository<TicketTransfer, Long> {

    Optional<TicketTransfer> findByTransferToken(String token);

    List<TicketTransfer> findByFromUserIdOrderByCreatedDateDesc(Long fromUserId);

    boolean existsByTicketIdAndStatus(Long ticketId, TicketTransferStatus status);
}
