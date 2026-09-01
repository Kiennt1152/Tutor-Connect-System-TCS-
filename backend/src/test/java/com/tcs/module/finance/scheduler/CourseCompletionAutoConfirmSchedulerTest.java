package com.tcs.module.finance.scheduler;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.finance.service.CenterEscrowAutoSettlementService;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test cho cơ chế tự xác nhận hoàn thành sau 7 ngày.
 *
 * <p>Đặc tả BF-04 và BF-05 (exception "Completion Not Confirmed"):
 * nếu không bên nào xác nhận, hệ thống tự xác nhận hoàn thành sau 7 ngày kể từ buổi cuối.
 * BF-04 = lớp CENTER, BF-05 = lớp PRIVATE thuộc sở hữu phụ huynh.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseCompletionAutoConfirmSchedulerTest {

    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private CenterEscrowAutoSettlementService settlementService;

    @InjectMocks private CourseCompletionAutoConfirmScheduler scheduler;

    private TutoringClass overdueClass(Long id, ClassType type) {
        TutoringClass c = new TutoringClass();
        c.setClassId(id);
        c.setClassType(type);
        c.setStatus(TutoringClassStatus.IN_PROGRESS);
        c.setEndDate(LocalDate.now().minusDays(8)); // quá hạn 7 ngày
        return c;
    }

    /** Sheet autoConfirmCompletion - UTCID01 (N). */
    @Test
    @DisplayName("UTCID01 (N) - BF-04: lớp CENTER quá 7 ngày -> tự xác nhận và tất toán")
    void utcid01_centerClassAutoConfirmed() {
        when(tutoringClassRepository.findAll()).thenReturn(List.of(overdueClass(1L, ClassType.CENTER)));
        when(settlementService.trySettleCompletedCenterClass(1L)).thenReturn(true);

        scheduler.autoConfirmStaleCompletions();

        verify(settlementService).trySettleCompletedCenterClass(1L);
    }

    /** Sheet autoConfirmCompletion - UTCID02 (B). */
    @Test
    @DisplayName("UTCID02 (B) - Lớp CENTER mới kết thúc 3 ngày (chưa quá hạn) -> chưa tự xác nhận")
    void utcid02_centerClassNotYetOverdue() {
        TutoringClass recent = overdueClass(2L, ClassType.CENTER);
        recent.setEndDate(LocalDate.now().minusDays(3));
        when(tutoringClassRepository.findAll()).thenReturn(List.of(recent));

        scheduler.autoConfirmStaleCompletions();

        verify(settlementService, never()).trySettleCompletedCenterClass(anyLong());
    }

    /** Sheet autoConfirmCompletion - UTCID03 (A). */
    @Test
    @DisplayName("UTCID03 (A) - Lớp PRIVATE quá 7 ngày -> scheduler center không xử lý")
    void utcid03_privateClassIsIgnoredByCenterScheduler() {
        when(tutoringClassRepository.findAll()).thenReturn(List.of(overdueClass(3L, ClassType.PRIVATE)));

        scheduler.autoConfirmStaleCompletions();

        verify(settlementService, never()).trySettleCompletedCenterClass(3L);
    }
}
