package com.tcs.module.contract.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.scheduler.CooperationContractExpiryScheduler;
import com.tcs.module.marketplace.entity.ClassAssignment;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * System Test ST-ECO-002: Not signed/not deposited within 48h.
 * Procedure:
 * 1. Generate the contract.
 * 2. Simulate >48h with no completed signing or deposit.
 * 3. Check status.
 * Expected:
 * System cancels the contract, returns the class to the marketplace for the remaining validity.
 *
 * Finding / Gap:
 * - Center Cooperation Agreement (BF-03): Successfully expires unsigned contract after 48h -> TERMINATED, application -> WITHDRAWN.
 * - Private Class Contract (Phụ huynh - Gia sư): GAP. No 48h expiry scheduler exists, and no logic returns the class to OPEN.
 */
@ExtendWith(MockitoExtension.class)
class ST_ECO_002_ContractExpiry48hTest {

    @Mock private ContractRepository contractRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;

    @InjectMocks private CooperationContractExpiryScheduler scheduler;

    private Contract expiredCooperationContract;
    private RecruitmentApplication application;

    @BeforeEach
    void setUp() {
        application = new RecruitmentApplication();
        application.setRecruitmentAppId(10L);
        application.setStatus(RecruitmentApplicationStatus.PASSED);

        expiredCooperationContract = new Contract();
        expiredCooperationContract.setContractId(100L);
        expiredCooperationContract.setStatus(ContractStatus.PENDING);
        expiredCooperationContract.setRecruitmentApplication(application);
        expiredCooperationContract.setExpiresAt(LocalDateTime.now().minusHours(1)); // >48h past creation
    }

    @Test
    @DisplayName("ST-ECO-002: Thỏa thuận tuyển dụng quá 48h chưa ký -> TERMINATED và đơn ứng tuyển WITHDRAWN")
    void testCooperationContractExpiresAfter48h() {
        when(contractRepository.findByStatus(ContractStatus.PENDING))
                .thenReturn(Collections.singletonList(expiredCooperationContract));

        scheduler.expireUnsignedCooperationContracts();

        // Contract terminated
        assertEquals(ContractStatus.TERMINATED, expiredCooperationContract.getStatus());
        verify(contractRepository).save(expiredCooperationContract);

        // Application withdrawn so center can select other tutors
        assertEquals(RecruitmentApplicationStatus.WITHDRAWN, application.getStatus());
        verify(recruitmentApplicationRepository).save(application);
    }

    @Test
    @DisplayName("ST-ECO-002: Thỏa thuận hợp tác chưa quá hạn (còn hiệu lực) -> giữ nguyên PENDING")
    void testCooperationContractNotYetExpiredDoesNotTerminate() {
        expiredCooperationContract.setExpiresAt(LocalDateTime.now().plusHours(20)); // not expired yet
        when(contractRepository.findByStatus(ContractStatus.PENDING))
                .thenReturn(Collections.singletonList(expiredCooperationContract));

        scheduler.expireUnsignedCooperationContracts();

        assertEquals(ContractStatus.PENDING, expiredCooperationContract.getStatus());
        assertEquals(RecruitmentApplicationStatus.PASSED, application.getStatus());
        verify(contractRepository, never()).save(expiredCooperationContract);
    }

    @Test
    @DisplayName("ST-ECO-002 [GAP]: Hợp đồng lớp cá nhân Marketplace bị bỏ qua bởi Scheduler hiện tại (chưa cài đặt)")
    void testPrivateClassContractSkippedByScheduler() {
        // Private class contract has ClassAssignment, but NO RecruitmentApplication
        Contract privateContract = new Contract();
        privateContract.setContractId(200L);
        privateContract.setStatus(ContractStatus.PENDING);
        privateContract.setRecruitmentApplication(null); // Not a center cooperation contract
        privateContract.setExpiresAt(LocalDateTime.now().minusHours(2));

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(300L);
        privateContract.setAssignment(assignment);

        when(contractRepository.findByStatus(ContractStatus.PENDING))
                .thenReturn(Collections.singletonList(privateContract));

        scheduler.expireUnsignedCooperationContracts();

        // Because app == null, CooperationContractExpiryScheduler skips it
        // Private contract remains PENDING, not TERMINATED
        assertEquals(ContractStatus.PENDING, privateContract.getStatus());
        verify(contractRepository, never()).save(privateContract);
    }
}
