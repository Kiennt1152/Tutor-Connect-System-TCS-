package com.tcs.module.messaging.scheduler;

import com.tcs.module.platform.service.PlatformService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketSlaSchedulerTest {

    @Mock
    private PlatformService platformService;

    @InjectMocks
    private TicketSlaScheduler ticketSlaScheduler;

    @Test
    @DisplayName("checkAndMarkSlaBreaches: gọi scanAndEscalateSlaBreaches từ platformService")
    void checkAndMarkSlaBreaches_CallsPlatformService() {
        when(platformService.scanAndEscalateSlaBreaches()).thenReturn(3);

        ticketSlaScheduler.checkAndMarkSlaBreaches();

        verify(platformService, times(1)).scanAndEscalateSlaBreaches();
    }

    @Test
    @DisplayName("checkAndMarkSlaBreaches: hoạt động bình thường khi trả về 0")
    void checkAndMarkSlaBreaches_NoBreachedTickets() {
        when(platformService.scanAndEscalateSlaBreaches()).thenReturn(0);

        ticketSlaScheduler.checkAndMarkSlaBreaches();

        verify(platformService, times(1)).scanAndEscalateSlaBreaches();
    }
}
