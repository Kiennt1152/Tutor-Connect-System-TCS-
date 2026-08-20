package com.tcs.module.platform.scheduler;

import com.tcs.module.platform.service.PlatformAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAnalyticsScheduler {

    private final PlatformAnalyticsService analyticsService;

    @Scheduled(cron = "0 0 1 * * *")
    public void scheduleDailyAnalyticsReport() {
        try {
            int count = analyticsService.generateScheduledDailyReport();
            log.info("PlatformAnalyticsScheduler: Tạo báo cáo tổng kết tự động thành công ({} báo cáo)", count);
        } catch (Exception e) {
            log.error("PlatformAnalyticsScheduler: Lỗi khi tạo báo cáo định kỳ tự động", e);
        }
    }
}
