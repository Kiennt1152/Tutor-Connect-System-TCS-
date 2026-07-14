package com.tcs.module.tutor.service;

import com.tcs.module.center.dto.request.MarkAttendanceRequest;
import com.tcs.module.center.dto.request.RescheduleRequestBody;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.RescheduleResponse;
import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import java.time.LocalDate;
import java.util.List;

public interface TutorService {

    /** Các lớp gia sư phụ trách có buổi học trong ngày (kèm học sinh + điểm danh). */
    List<CenterScheduleClassResponse> getSchedule(LocalDate date);

    /** Gia sư yêu cầu dời một buổi học sang ngày khác (báo ốm) — chờ trung tâm duyệt. */
    RescheduleResponse requestReschedule(Long classId, RescheduleRequestBody body);

    /** Danh sách yêu cầu dời lịch của gia sư (các lớp mình phụ trách). */
    List<RescheduleResponse> listMyReschedules();

    /** Chi tiết một buổi học của lớp (để mở trang điểm danh). */
    CenterScheduleClassResponse getClassSession(Long classId, LocalDate date);

    /** Gia sư điểm danh một học sinh cho buổi học của lớp trong ngày (chỉ lớp mình phụ trách). */
    CenterScheduleClassResponse markAttendance(
            Long classId, LocalDate date, Long classStudentId, LessonAttendanceStatus status);

    /** Lưu điểm danh cả buổi (nhiều học sinh) sau khi gia sư xác nhận. */
    CenterScheduleClassResponse markAttendanceBatch(
            Long classId, LocalDate date, List<MarkAttendanceRequest> records);
}
