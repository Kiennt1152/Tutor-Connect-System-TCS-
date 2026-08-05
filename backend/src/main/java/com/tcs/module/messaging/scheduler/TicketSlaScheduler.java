package com.tcs.module.messaging.scheduler;

import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.SupportTicketRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSlaScheduler {

    private final SupportTicketRepository supportTicketRepository;

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void checkAndMarkSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();
        List<SupportTicketStatus> excludedStatuses = List.of(SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED);
        List<SupportTicket> breachedCandidates = supportTicketRepository.findBreachedCandidateTickets(excludedStatuses, now);

        if (breachedCandidates.isEmpty()) {
            return;
        }

        log.info("TicketSlaScheduler found {} tickets exceeding SLA dueAt", breachedCandidates.size());
        for (SupportTicket ticket : breachedCandidates) {
            ticket.setSlaBreached(true);
        }
        supportTicketRepository.saveAll(breachedCandidates);
    }
}
