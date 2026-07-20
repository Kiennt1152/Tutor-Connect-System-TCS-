package com.tcs.module.center.service;

import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.SaveRecruitmentPostRequest;
import com.tcs.module.center.dto.response.CenterTutorResponse;
import com.tcs.module.center.dto.response.RecruitmentApplicationResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.enums.CenterTutorMembershipStatus;
import java.util.List;

/** FT-33: Trung tâm đăng tin tuyển gia sư; gia sư ứng tuyển; trung tâm duyệt/từ chối. */
public interface CenterService {

    // ===================== Phía gia sư / công khai =====================

    /** Các tin đang mở (ACTIVE) để gia sư tự do xem và ứng tuyển. */
    List<RecruitmentPostResponse> listOpenRecruitmentPosts();

    /** Gia sư nộp đơn ứng tuyển vào một tin đang mở. */
    void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request);

    /** Các đơn ứng tuyển của gia sư đang đăng nhập. */
    List<RecruitmentApplicationResponse> listMyApplications();

    // ===================== Phía trung tâm =====================

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
}
