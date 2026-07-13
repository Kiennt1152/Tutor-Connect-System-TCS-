package com.tcs.module.center.service;

import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.CreateRecruitmentPostRequest;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.dto.response.TutorOptionResponse;
import java.time.LocalDate;
import java.util.List;

public interface CenterService {

    List<RecruitmentPostResponse> listRecruitmentPosts();

    RecruitmentPostResponse createRecruitmentPost(CreateRecruitmentPostRequest request);

    RecruitmentPostResponse publishRecruitmentPost(Long recruitmentId);

    void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request);

    // UC-14-B: Quản lý lớp học của Trung tâm gia sư.
    List<CenterClassResponse> listMyClasses();

    CenterClassResponse getMyClass(Long classId);

    CenterClassResponse createClass(SaveClassRequest request);

    CenterClassResponse updateClass(Long classId, SaveClassRequest request);

    /** Đăng tải lớp (DRAFT -> OPEN) để hiển thị ở "Tìm lớp". */
    CenterClassResponse publishClass(Long classId);

    /**
     * Danh sách gia sư để trung tâm chọn gán (tạm thời lấy tất cả).
     * Nếu truyền {@code classId}, mỗi gia sư sẽ được đánh dấu có trùng lịch với lớp đó hay không.
     */
    List<TutorOptionResponse> listTutors(Long classId);

    /** Gán 1 gia sư cho lớp của trung tâm (thay gia sư cũ nếu đã có). */
    CenterClassResponse assignTutor(Long classId, Long tutorId);

    /** Gỡ gia sư đang gán khỏi lớp. */
    CenterClassResponse unassignTutor(Long classId);

    // ===== Lịch lớp CENTER (theo ngày) — chỉ xem =====

    /** Các lớp của trung tâm có buổi học trong ngày, kèm gia sư + học sinh + trạng thái điểm danh (chỉ xem). */
    List<CenterScheduleClassResponse> getSchedule(LocalDate date);
}
