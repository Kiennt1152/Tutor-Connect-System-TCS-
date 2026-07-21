package com.tcs.module.center.service;

import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.RescheduleDecisionBody;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.request.SaveRecruitmentPostRequest;
import com.tcs.module.center.dto.request.SubstitutionDecisionBody;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.CenterTutorResponse;
import com.tcs.module.center.dto.response.RecruitmentApplicationResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.dto.response.RescheduleResponse;
import com.tcs.module.center.dto.response.SubstitutionResponse;
import com.tcs.module.center.dto.response.TutorOptionResponse;
import com.tcs.module.center.enums.CenterTutorMembershipStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Trung tâm gia sư: đăng tin tuyển gia sư (FT-33) + quản lý lớp học của trung tâm (UC-14-B).
 */
public interface CenterService {

    // ===================== Tin tuyển gia sư — phía gia sư / công khai =====================

    /** Các tin đang mở (ACTIVE) để gia sư tự do xem và ứng tuyển. */
    List<RecruitmentPostResponse> listOpenRecruitmentPosts();

    /** Gia sư nộp đơn ứng tuyển vào một tin đang mở. */
    void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request);

    /** Các đơn ứng tuyển của gia sư đang đăng nhập. */
    List<RecruitmentApplicationResponse> listMyApplications();

    // ===================== Tin tuyển gia sư — phía trung tâm =====================

    /** Tin tuyển dụng của trung tâm đang đăng nhập (mọi trạng thái). */
    List<RecruitmentPostResponse> listMyRecruitmentPosts();

    /** Tạo tin mới ở trạng thái DRAFT. */
    RecruitmentPostResponse createRecruitmentPost(SaveRecruitmentPostRequest request);

    /** Sửa tin — chỉ khi tin còn ở trạng thái DRAFT. */
    RecruitmentPostResponse updateRecruitmentPost(Long recruitmentId, SaveRecruitmentPostRequest request);

    /** Đăng tin (DRAFT -> ACTIVE) để gia sư nhìn thấy và ứng tuyển. */
    RecruitmentPostResponse publishRecruitmentPost(Long recruitmentId);

    /** Đóng tin (ACTIVE -> CLOSED), không nhận đơn mới nữa. */
    RecruitmentPostResponse closeRecruitmentPost(Long recruitmentId);

    /** Danh sách đơn ứng tuyển của một tin (chỉ tin của trung tâm mình). */
    List<RecruitmentApplicationResponse> listApplications(Long recruitmentId);

    /** Duyệt (HIRED) hoặc từ chối (REJECTED) một đơn ứng tuyển. */
    RecruitmentApplicationResponse decideApplication(Long recruitmentAppId, boolean approve);

    // ===================== Quản lý danh sách gia sư của trung tâm =====================

    /** Danh sách gia sư (thành viên) của trung tâm đang đăng nhập. */
    List<CenterTutorResponse> listMyTutors();

    /** Đổi trạng thái thành viên: ACTIVE / INACTIVE / TERMINATED. */
    CenterTutorResponse updateMembershipStatus(Long membershipId, CenterTutorMembershipStatus status);

    // ===================== UC-14-B: Quản lý lớp học của Trung tâm gia sư =====================

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

    /** Gán 1 gia sư phụ (backup) cho lớp để dạy thay khi gia sư chính báo ốm/bận. */
    CenterClassResponse assignAssistant(Long classId, Long tutorId);

    /** Gỡ gia sư phụ khỏi lớp. */
    CenterClassResponse unassignAssistant(Long classId);

    // ===== Lịch lớp CENTER (theo ngày) — chỉ xem =====

    /** Các lớp của trung tâm có buổi học trong ngày, kèm gia sư + học sinh + trạng thái điểm danh. */
    List<CenterScheduleClassResponse> getSchedule(LocalDate date);

    // ===== Duyệt yêu cầu dời buổi học của gia sư =====

    /** Danh sách yêu cầu dời buổi (mọi trạng thái) thuộc các lớp của trung tâm. */
    List<RescheduleResponse> listReschedules();

    /** Trung tâm duyệt/từ chối một yêu cầu dời buổi. */
    RescheduleResponse decideReschedule(RescheduleDecisionBody body);

    // ===== Duyệt yêu cầu nhờ gia sư phụ dạy thay =====

    /** Danh sách yêu cầu dạy thay (mọi trạng thái) thuộc các lớp của trung tâm. */
    List<SubstitutionResponse> listSubstitutions();

    /** Trung tâm duyệt/từ chối một yêu cầu dạy thay. */
    SubstitutionResponse decideSubstitution(SubstitutionDecisionBody body);
}
