package com.example.ticketbox.repository;

import com.example.ticketbox.model.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    boolean existsByTicketId(Long ticketId);
}
