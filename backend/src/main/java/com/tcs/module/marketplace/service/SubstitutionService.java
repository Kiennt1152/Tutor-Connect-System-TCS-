package com.tcs.module.marketplace.service;

import com.tcs.module.marketplace.dto.SubstitutionEntry;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Quản lý gia sư phụ của lớp và các yêu cầu "dạy thay" một buổi (lưu trong system_parameters,
 * không cần migration). Song song với {@link RescheduleService}.
 */
public interface SubstitutionService {

    // ===== Gia sư phụ gán cho lớp =====

    /** Gán (hoặc thay) gia sư phụ cho lớp. */
    void assignAssistant(Long classId, Long tutorId);

    /** Gỡ gia sư phụ khỏi lớp. */
    void removeAssistant(Long classId);

    /** Gia sư phụ hiện tại của lớp (nếu có). */
    Optional<Long> findAssistant(Long classId);

    /** Bản đồ classId -> tutorId gia sư phụ, cho tập lớp truyền vào. */
    Map<Long, Long> findAssistants(Collection<Long> classIds);

    /** Các lớp mà {@code tutorId} đang là gia sư phụ. */
    List<Long> findClassIdsByAssistant(Long tutorId);

    // ===== Yêu cầu dạy thay một buổi =====

    /** Gia sư chính tạo/đặt lại yêu cầu nhờ gia sư phụ dạy thay buổi {@code date} (PENDING). */
    SubstitutionEntry request(Long classId, LocalDate date, Long assistantTutorId, String reason);

    /** Trung tâm duyệt/từ chối yêu cầu dạy thay. */
    SubstitutionEntry decide(Long classId, LocalDate date, boolean approve);

    Optional<SubstitutionEntry> find(Long classId, LocalDate date);

    List<SubstitutionEntry> listByClassIds(Collection<Long> classIds);

    List<SubstitutionEntry> listApprovedByClassIds(Collection<Long> classIds);
}
