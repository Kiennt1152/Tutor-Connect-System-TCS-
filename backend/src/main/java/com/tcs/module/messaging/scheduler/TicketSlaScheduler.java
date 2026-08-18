package com.tcs.module.messaging.scheduler;

import com.tcs.module.platform.service.PlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSlaScheduler {

    private final PlatformService platformService;

    @Scheduled(cron = "0 */10 * * * *")
    public void checkAndMarkSlaBreaches() {
        int escalatedCount = platformService.scanAndEscalateSlaBreaches();
        if (escalatedCount > 0) {
            log.info("TicketSlaScheduler đã hoàn tất quét và nâng cấp {} ticket quá hạn SLA", escalatedCount);
        }
    }
}
