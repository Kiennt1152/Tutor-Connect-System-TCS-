package com.tcs.module.marketplace.service;

import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.request.ExtraLessonRequest;
import com.tcs.module.marketplace.dto.request.RescheduleDecisionRequest;
import com.tcs.module.marketplace.dto.request.RescheduleLessonRequest;
import com.tcs.module.marketplace.dto.response.ApplicantResponse;
import com.tcs.module.marketplace.dto.response.AssignmentResponse;
import com.tcs.module.marketplace.dto.response.CenterSummaryResponse;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.dto.response.LessonResponse;
import com.tcs.module.marketplace.dto.response.RescheduleRequestResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import java.util.List;

public interface MarketplaceService {

    List<ClassResponse> listClasses(TutoringClassStatus status);

    ClassResponse getClass(Long classId, Long assignmentId, Long classStudentId);

    List<ClassResponse> listMyClasses();

    ClassResponse createClass(CreateClassRequest request);

    ClassResponse updateClass(Long classId, CreateClassRequest request);

    ClassResponse publishClass(Long classId);

    ClassResponse unpublishClass(Long classId);

    void applyToClass(Long classId, ApplyClassRequest request);

    ClassTerminationResponse requestClassTermination(Long classId, CreateClassTerminationRequest request);

    /** Đăng ký lớp đang mở: gia sư -> nộp đơn dạy; phụ huynh/học viên -> ghi danh. */
    void registerToClass(Long classId);

    List<Long> listMyAppliedClassIds();

    List<ApplicantResponse> listApplicants(Long classId);

    void chooseApplicant(Long classId, Long applicationId);

    void rejectApplicant(Long classId, Long applicationId, String reason);

    List<AssignmentResponse> listMyAssignments();

    void acceptAssignment(Long assignmentId);

    void declineAssignment(Long assignmentId);

    List<LessonResponse> listMyLessons();

    void checkInLesson(Long lessonId);

    void checkOutLesson(Long lessonId);

    void markAttendance(Long lessonId, boolean present);

    RescheduleRequestResponse requestReschedule(Long lessonId, RescheduleLessonRequest request);

    RescheduleRequestResponse requestExtraLesson(ExtraLessonRequest request);

    List<RescheduleRequestResponse> listMyRescheduleRequests();

    void decideRescheduleRequest(Long requestId, RescheduleDecisionRequest decision);

    void cancelRescheduleRequest(Long requestId);

    List<TutorSearchResponse> searchTutors(String keyword, Long subjectId);

    void addFavorite(Long tutorId);

    void removeFavorite(Long tutorId);

    List<TutorSearchResponse> getFavorites();

    // ===== Yêu cầu mở lớp gửi tới một trung tâm cụ thể (phía phụ huynh) =====

    /** Danh sách trung tâm đã xác minh để phụ huynh chọn khi gửi yêu cầu. */
    List<CenterSummaryResponse> listCenters();

    /** Phụ huynh gửi yêu cầu mở lớp tới một trung tâm đã xác minh. */
    ClassRequestResponse createClassRequest(Long centerId, ClassRequestCreateRequest request);

    /** Danh sách yêu cầu mở lớp phụ huynh đã gửi (mọi trạng thái). */
    List<ClassRequestResponse> listMyClassRequests();

    /** Phụ huynh hủy yêu cầu khi còn đang chờ xử lý. */
    void cancelClassRequest(String requestId);
}
