package com.tcs.module.messaging.scheduler;

import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.SupportTicketRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketSlaSchedulerTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @InjectMocks
    private TicketSlaScheduler ticketSlaScheduler;

    @Test
    @DisplayName("checkAndMarkSlaBreaches: tìm và đánh dấu các ticket quá hạn thành slaBreached = true")
    void checkAndMarkSlaBreaches_UpdatesBreachedTickets() {
        SupportTicket ticket1 = new SupportTicket();
        ticket1.setTicketId(1L);
        ticket1.setStatus(SupportTicketStatus.OPEN);
        ticket1.setPriority(SupportTicketPriority.URGENT);
        ticket1.setDueAt(LocalDateTime.now().minusHours(2));
        ticket1.setSlaBreached(false);

        when(supportTicketRepository.findBreachedCandidateTickets(any(), any()))
                .thenReturn(List.of(ticket1));

        ticketSlaScheduler.checkAndMarkSlaBreaches();

        assertTrue(ticket1.getSlaBreached());
        verify(supportTicketRepository, times(1)).saveAll(List.of(ticket1));
    }

    @Test
    @DisplayName("checkAndMarkSlaBreaches: không làm gì khi không có ticket nào quá hạn")
    void checkAndMarkSlaBreaches_NoBreachedTickets() {
        when(supportTicketRepository.findBreachedCandidateTickets(any(), any()))
                .thenReturn(List.of());

        ticketSlaScheduler.checkAndMarkSlaBreaches();

        verify(supportTicketRepository, never()).saveAll(any());
    }
}
