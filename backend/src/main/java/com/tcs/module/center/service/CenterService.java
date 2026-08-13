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
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
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

    /** Gia sư rút đơn ứng tuyển (chỉ khi đơn còn ở trạng thái mới nộp). */
    void withdrawApplication(Long recruitmentAppId);

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

    /**
     * Duyệt (PASSED, chờ ký) hoặc từ chối (REJECTED) một đơn ứng tuyển.
     * {@code contractTemplateId} (tuỳ chọn): mẫu hợp đồng center chọn khi duyệt.
     * {@code contractContent} (tuỳ chọn): nội dung điều khoản center tự nhập/sửa khi duyệt.
     */
    RecruitmentApplicationResponse decideApplication(
            Long recruitmentAppId, boolean approve, Long contractTemplateId, String contractContent);

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

    CenterClassResponse closeEnrollment(Long classId);

    /** BF-04 bước 10: kích hoạt lớp (bắt đầu học) khi đủ sĩ số tối thiểu -> IN_PROGRESS. */
    CenterClassResponse activateClass(Long classId);

    /** Bước 13: trung tâm xác nhận khóa học đã hoàn thành -> tất toán + đóng lớp. */
    void confirmClassCompletion(Long classId);

    /** Thống kê tình trạng lớp (điểm danh có mặt/vắng/có phép) theo lớp + học sinh. */
    com.tcs.module.center.dto.response.CenterStatsResponse getClassStats();

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

    // ===== Yêu cầu mở lớp do phụ huynh gửi tới trung tâm =====

    /** Danh sách yêu cầu mở lớp gửi tới trung tâm này (mọi trạng thái). */
    List<ClassRequestResponse> listIncomingClassRequests();

    /** Trung tâm nhận tìm gia sư cho yêu cầu (chuyển sang trạng thái ĐANG TÌM). */
    void startSearch(String requestId);

    /** Trung tâm đề cử một gia sư (thuộc đội) vào shortlist của yêu cầu. */
    void proposeTutor(String requestId, Long tutorId);

    /** Trung tâm gỡ một gia sư khỏi shortlist của yêu cầu. */
    void removeCandidate(String requestId, Long tutorId);

    /** Đăng tin tuyển gia sư NGOÀI đội cho yêu cầu (tin ACTIVE, mọi gia sư ứng tuyển được). */
    ClassRequestResponse postRecruitmentForRequest(String requestId);

    /** Danh sách đơn ứng tuyển vào tin tuyển đã đăng cho yêu cầu (để duyệt vào shortlist). */
    List<com.tcs.module.center.dto.response.RecruitmentApplicationResponse>
            listRequestApplications(String requestId);

    /** Trung tâm chấp nhận yêu cầu: bổ sung chi tiết và tạo lớp EXTERNAL từ yêu cầu đó. */
    CenterClassResponse acceptClassRequest(String requestId, SaveClassRequest body);

    /** Trung tâm từ chối yêu cầu, kèm lý do. */
    void rejectClassRequest(String requestId, String reason);

    /** Trung tâm không tìm được gia sư -> đóng yêu cầu + thông báo cho phụ huynh. */
    void giveUpClassRequest(String requestId, String reason);

    // ===== Quản lý mẫu hợp đồng =====

    /** Danh sách mẫu hợp đồng trung tâm dùng được (mẫu hệ thống + của chính trung tâm). */
    List<com.tcs.module.center.dto.response.ContractTemplateResponse> listContractTemplates();

    /** Tạo mẫu hợp đồng của trung tâm. */
    com.tcs.module.center.dto.response.ContractTemplateResponse createContractTemplate(
            com.tcs.module.center.dto.request.SaveContractTemplateRequest request);

    /** Sửa mẫu hợp đồng của chính trung tâm (không sửa mẫu hệ thống). */
    com.tcs.module.center.dto.response.ContractTemplateResponse updateContractTemplate(
            Long templateId, com.tcs.module.center.dto.request.SaveContractTemplateRequest request);

    /** Thông tin trung tâm cho khối BÊN A của hợp đồng (hồ sơ + phần bổ sung). */
    com.tcs.module.center.dto.response.CenterContractInfoResponse getContractInfo();

    /** Lưu thông tin bổ sung BÊN A (website, đại diện, chức vụ). */
    com.tcs.module.center.dto.response.CenterContractInfoResponse saveContractInfo(
            com.tcs.module.center.dto.request.SaveCenterContractInfoRequest request);
}
