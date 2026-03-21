package com.example.ticketbox.service;

import com.example.ticketbox.model.ReminderLog;
import com.example.ticketbox.model.Ticket;
import com.example.ticketbox.repository.ReminderLogRepository;
import com.example.ticketbox.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventReminderService {

    private final TicketRepository ticketRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 */23 * * *")
    @Transactional
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusHours(23);
        LocalDateTime windowEnd = now.plusHours(25);

        List<Ticket> tickets = ticketRepository.findIssuedTicketsForReminderWindow(windowStart, windowEnd);
        log.info("Event reminder job: found {} issued tickets in window [{} - {}]", tickets.size(), windowStart, windowEnd);

        int sentCount = 0;
        for (Ticket ticket : tickets) {
            if (reminderLogRepository.existsByTicketId(ticket.getId())) {
                log.debug("Reminder already sent for ticket #{}, skipping", ticket.getId());
                continue;
            }

            emailService.sendEventReminderEmail(ticket);

            ReminderLog reminderLog = ReminderLog.builder()
                    .ticket(ticket)
                    .event(ticket.getEvent())
                    .build();
            reminderLogRepository.save(reminderLog);
            sentCount++;
        }

        log.info("Event reminder job completed: sent {} reminders out of {} eligible tickets", sentCount, tickets.size());
    }
}
