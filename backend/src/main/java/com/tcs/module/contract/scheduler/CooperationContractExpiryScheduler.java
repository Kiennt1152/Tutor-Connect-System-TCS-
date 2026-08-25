package com.tcs.module.contract.scheduler;

import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * BF-03 (exception - "Agreement Declined or Unsigned"): thỏa thuận hợp tác gia sư chưa ký quá 48 giờ
 * sẽ tự HẾT HIỆU LỰC. Hợp đồng chuyển TERMINATED và đơn ứng tuyển được đóng (WITHDRAWN) để trung tâm
 * có thể chọn ứng viên khác. Chỉ áp dụng cho hợp đồng gắn với đơn tuyển dụng (recruitmentApplication).
 */
@Component
public class CooperationContractExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(CooperationContractExpiryScheduler.class);

    private final ContractRepository contractRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;

    public CooperationContractExpiryScheduler(ContractRepository contractRepository, RecruitmentApplicationRepository recruitmentApplicationRepository) {
        this.contractRepository = contractRepository;
        this.recruitmentApplicationRepository = recruitmentApplicationRepository;
    }

    /** Rà mỗi giờ (chạy lần đầu ngay khi khởi động). */
    @Scheduled(fixedRate = 3_600_000L)
    @Transactional
    public void expireUnsignedCooperationContracts() {
        LocalDateTime now = LocalDateTime.now();
        int expired = 0;
        for (Contract c : contractRepository.findByStatus(ContractStatus.PENDING)) {
            RecruitmentApplication app = c.getRecruitmentApplication();
            if (app == null) {
                continue; // chỉ xét thỏa thuận hợp tác gia sư
            }
            if (c.getExpiresAt() == null || c.getExpiresAt().isAfter(now)) {
                continue; // chưa tới hạn
            }
            c.setStatus(ContractStatus.TERMINATED);
            contractRepository.save(c);
            if (app.getStatus() == RecruitmentApplicationStatus.PASSED) {
                app.setStatus(RecruitmentApplicationStatus.WITHDRAWN);
                recruitmentApplicationRepository.save(app);
            }
            expired++;
        }
        if (expired > 0) {
            log.info("BF-03: đã cho hết hiệu lực {} thỏa thuận hợp tác chưa ký quá 48h", expired);
        }
    }
}
