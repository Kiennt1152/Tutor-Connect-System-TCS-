package com.tcs.module.marketplace.service;

import com.tcs.module.marketplace.dto.RescheduleEntry;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Quản lý các yêu cầu dời buổi học (lưu trong system_parameters, không cần migration). */
public interface RescheduleService {

    /** Gia sư tạo/đặt lại yêu cầu dời buổi (trạng thái PENDING). */
    RescheduleEntry request(Long classId, LocalDate originalDate, LocalDate newDate,
            LocalTime newStartTime, LocalTime newEndTime, Long tutorId, String reason);

    /** Trung tâm duyệt/từ chối yêu cầu. */
    RescheduleEntry decide(Long classId, LocalDate originalDate, boolean approve);

    Optional<RescheduleEntry> find(Long classId, LocalDate originalDate);

    List<RescheduleEntry> listByClassIds(Collection<Long> classIds);

    List<RescheduleEntry> listPendingByClassIds(Collection<Long> classIds);

    List<RescheduleEntry> listApprovedByClassIds(Collection<Long> classIds);
}
