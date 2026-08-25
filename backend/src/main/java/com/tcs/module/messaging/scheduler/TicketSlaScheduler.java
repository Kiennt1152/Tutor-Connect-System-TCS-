package com.tcs.module.messaging.scheduler;

import com.tcs.module.platform.service.PlatformService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * =========================================================================
 * LUỒNG 7: QUÉT ĐỊNH KỲ & NÂNG CẤP KHẨN CẤP TICKET QUÁ HẠN SLA (JOB-11)
 * =========================================================================
 * Bộ lập lịch chạy ngầm 10 phút/lần giám sát tính tuân thủ cam kết chất lượng dịch vụ SLA.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSlaScheduler {

    private final PlatformService platformService;

    // Luồng 7 - Kích hoạt tự động vào phút 00, 10, 20, 30, 40, 50 của mỗi giờ
    @Scheduled(cron = "0 */10 * * * *")
    public void checkAndMarkSlaBreaches() {
        int escalatedCount = platformService.scanAndEscalateSlaBreaches();
        if (escalatedCount > 0) {
            log.info("TicketSlaScheduler đã hoàn tất quét và nâng cấp {} ticket quá hạn SLA", escalatedCount);
        }
    }
}
