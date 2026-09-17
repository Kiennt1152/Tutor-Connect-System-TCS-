package com.tcs.module.marketplace.controller;

import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.request.ClassRequestCreateRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.request.RescheduleDecisionRequest;
import com.tcs.module.marketplace.dto.request.RescheduleLessonRequest;
import com.tcs.module.marketplace.dto.response.ApplicantResponse;
import com.tcs.module.marketplace.dto.response.AssignmentResponse;
import com.tcs.module.marketplace.dto.response.ContractViewResponse;
import com.tcs.module.marketplace.dto.response.CenterProfileResponse;
import com.tcs.module.marketplace.dto.response.CenterSummaryResponse;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.dto.response.LessonResponse;
import com.tcs.module.marketplace.dto.response.RescheduleRequestResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.service.MarketplaceService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/classes")
    public List<ClassResponse> listClasses(@RequestParam(required = false) TutoringClassStatus status) {
        return marketplaceService.listClasses(status);
    }

    /**
     * GET /classes/board — tin cho bảng "Danh sách tin đã đăng" (lớp đang mở + lớp đã chọn gia sư nhưng chưa hoàn tất hợp đồng).
     */
    @GetMapping("/classes/board")
    public List<ClassResponse> listBoardClasses() {
        return marketplaceService.listBoardClasses();
    }

    /** GET /classes/mine — tin/lớp do người đăng nhập tạo. */
    @GetMapping("/classes/mine")
    public List<ClassResponse> listMyClasses() {
        return marketplaceService.listMyClasses();
    }

    @GetMapping("/classes/{classId}")
    public ClassResponse getClass(
            @PathVariable Long classId,
            @RequestParam(required = false) Long assignmentId,
            @RequestParam(required = false) Long classStudentId) {
        return marketplaceService.getClass(classId, assignmentId, classStudentId);
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse createClass(@RequestBody CreateClassRequest request) {
        return marketplaceService.createClass(request);
    }

    /** PUT /classes/{classId} — chủ lớp sửa tin tìm gia sư. */
    @PutMapping("/classes/{classId}")
    public ClassResponse updateClass(@PathVariable Long classId, @RequestBody CreateClassRequest request) {
        return marketplaceService.updateClass(classId, request);
    }

    // ===== Yêu cầu mở lớp gửi tới một trung tâm cụ thể (phụ huynh) =====

    @GetMapping("/centers")
    public List<CenterSummaryResponse> listCenters() {
        return marketplaceService.listCenters();
    }

    /** Hồ sơ công khai của một trung tâm (trang "Xem hồ sơ" từ danh sách trung tâm). */
    @GetMapping("/centers/{centerId}")
    public CenterProfileResponse getCenterProfile(@PathVariable Long centerId) {
        return marketplaceService.getCenterProfile(centerId);
    }

    @PostMapping("/centers/{centerId}/class-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassRequestResponse createClassRequest(
            @PathVariable Long centerId, @RequestBody ClassRequestCreateRequest request) {
        return marketplaceService.createClassRequest(centerId, request);
    }

    @GetMapping("/class-requests/mine")
    public List<ClassRequestResponse> myClassRequests() {
        return marketplaceService.listMyClassRequests();
    }

    @DeleteMapping("/class-requests/{requestId}")
    public Map<String, String> cancelClassRequest(@PathVariable String requestId) {
        marketplaceService.cancelClassRequest(requestId);
        return Map.of("message", "Đã hủy yêu cầu mở lớp");
    }

    @PostMapping("/class-requests/{requestId}/choose-tutor/{tutorId}")
    public ClassResponse chooseTutorForClassRequest(
            @PathVariable String requestId, @PathVariable Long tutorId) {
        return marketplaceService.fulfillClassRequest(requestId, tutorId);
    }

    @PostMapping("/classes/{classId}/publish")
    public ClassResponse publishClass(@PathVariable Long classId) {
        return marketplaceService.publishClass(classId);
    }

    /** POST /classes/{classId}/unpublish — gỡ đăng lớp về Nháp. */
    @PostMapping("/classes/{classId}/unpublish")
    public ClassResponse unpublishClass(@PathVariable Long classId) {
        return marketplaceService.unpublishClass(classId);
    }

    @PostMapping("/classes/{classId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> applyToClass(@PathVariable Long classId, @RequestBody ApplyClassRequest request) {
        marketplaceService.applyToClass(classId, request);
        return Map.of("message", "Đã gửi đơn ứng tuyển");
    }

    @PostMapping("/classes/{classId}/termination")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassTerminationResponse requestClassTermination(
            @PathVariable Long classId, @RequestBody CreateClassTerminationRequest request) {
        return marketplaceService.requestClassTermination(classId, request);
    }

    /** UC "Hoàn thành lớp": gia sư bấm hoàn thành; lớp đóng sau khi học viên đánh giá gia sư. */
    @PostMapping("/classes/{classId}/complete")
    public Map<String, String> confirmClassCompletion(@PathVariable Long classId) {
        return Map.of("message", marketplaceService.confirmClassCompletion(classId));
    }

    /** Đăng ký lớp đang mở: gia sư -> nộp đơn dạy; phụ huynh/học viên -> ghi danh. */
    @PostMapping("/classes/{classId}/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> registerToClass(@PathVariable Long classId) {
        return Map.of("message", marketplaceService.registerToClass(classId));
    }

    /** GET /applications/mine — id các lớp gia sư đã ứng tuyển. */
    @GetMapping("/applications/mine")
    public List<Long> listMyAppliedClassIds() {
        return marketplaceService.listMyAppliedClassIds();
    }

    /** GET /classes/{classId}/applications — danh sách ứng viên đã xếp hạng (chỉ chủ lớp). */
    @GetMapping("/classes/{classId}/applications")
    public List<ApplicantResponse> listApplicants(@PathVariable Long classId) {
        return marketplaceService.listApplicants(classId);
    }

    /** Gia sư kiểm tra trước: lớp nào trùng thời gian bận đã đăng ký, ví dụ {@code ?classIds=1,2,3}. */
    @GetMapping("/busy-conflicts")
    public List<com.tcs.module.marketplace.dto.response.ClassBusyConflictResponse> listMyBusyConflicts(
            @RequestParam List<Long> classIds) {
        return marketplaceService.listMyBusyConflicts(classIds);
    }

    /** POST /classes/{classId}/applications/{applicationId}/choose — chủ lớp chọn gia sư. */
    @PostMapping("/classes/{classId}/applications/{applicationId}/choose")
    public Map<String, String> chooseApplicant(
            @PathVariable Long classId, @PathVariable Long applicationId) {
        marketplaceService.chooseApplicant(classId, applicationId);
        return Map.of("message", "Đã chọn gia sư — đang chờ gia sư nhận lớp");
    }

    /** POST /classes/{classId}/applications/{applicationId}/reject — chủ lớp từ chối ứng viên, body có "reason". */
    @PostMapping("/classes/{classId}/applications/{applicationId}/reject")
    public Map<String, String> rejectApplicant(
            @PathVariable Long classId,
            @PathVariable Long applicationId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        marketplaceService.rejectApplicant(classId, applicationId, reason);
        return Map.of("message", "Đã bỏ chọn gia sư");
    }

    /** GET /assignments/mine — phân công lớp riêng của người đăng nhập. */
    @GetMapping("/assignments/mine")
    public List<AssignmentResponse> listMyAssignments() {
        return marketplaceService.listMyAssignments();
    }

    /** POST /assignments/{assignmentId}/accept — gia sư nhận lớp (sau khi đã ký và có ký quỹ). */
    @PostMapping("/assignments/{assignmentId}/accept")
    public Map<String, String> acceptAssignment(@PathVariable Long assignmentId) {
        marketplaceService.acceptAssignment(assignmentId);
        return Map.of("message", "Đã nhận lớp — lịch dạy đã được tạo");
    }

    /** POST /assignments/{assignmentId}/decline — gia sư từ chối lời mời nhận lớp. */
    @PostMapping("/assignments/{assignmentId}/decline")
    public Map<String, String> declineAssignment(@PathVariable Long assignmentId) {
        marketplaceService.declineAssignment(assignmentId);
        return Map.of("message", "Đã từ chối lớp");
    }

    /** GET /assignments/{assignmentId}/contract — dữ liệu trang ký hợp đồng lớp riêng. */
    @GetMapping("/assignments/{assignmentId}/contract")
    public ContractViewResponse getAssignmentContract(@PathVariable Long assignmentId) {
        return marketplaceService.getAssignmentContract(assignmentId);
    }

    /** POST /assignments/{assignmentId}/sign/request-otp — gửi mã OTP ký hợp đồng qua email. */
    @PostMapping("/assignments/{assignmentId}/sign/request-otp")
    public Map<String, String> requestSignOtp(@PathVariable Long assignmentId) {
        marketplaceService.requestSignOtp(assignmentId);
        return Map.of("message", "Đã gửi mã OTP tới email của bạn");
    }

    /** POST /assignments/{assignmentId}/sign — ký hợp đồng bằng OTP (body có "otp"). */
    @PostMapping("/assignments/{assignmentId}/sign")
    public Map<String, String> signAssignmentContract(
            @PathVariable Long assignmentId,
            @RequestBody(required = false) Map<String, String> body) {
        String otp = body != null ? body.get("otp") : null;
        marketplaceService.signAssignmentContract(assignmentId, otp);
        return Map.of("message", "Đã ký hợp đồng");
    }

    /** POST /assignments/{assignmentId}/contract-terms — bên A lưu điều khoản bổ sung ("termsB"). */
    @PostMapping("/assignments/{assignmentId}/contract-terms")
    public Map<String, String> saveContractTerms(
            @PathVariable Long assignmentId,
            @RequestBody(required = false) Map<String, String> body) {
        marketplaceService.saveContractTermsB(assignmentId, body != null ? body.get("termsB") : null);
        return Map.of("message", "Đã lưu điều khoản");
    }

    @PostMapping("/assignments/{assignmentId}/refund-payout")
    public Map<String, String> saveAssignmentRefundPayoutInfo(
            @PathVariable Long assignmentId,
            @RequestBody SaveRefundPayoutRequest request) {
        marketplaceService.saveAssignmentRefundPayoutInfo(assignmentId, request);
        return Map.of("message", "Đã lưu tài khoản nhận hoàn tiền");
    }

    /** GET /lessons/mine — thời khoá biểu lớp riêng của người đăng nhập. */
    @GetMapping("/lessons/mine")
    public List<LessonResponse> listMyLessons() {
        return marketplaceService.listMyLessons();
    }

    @GetMapping("/center-schedule")
    public List<com.tcs.module.center.dto.response.CenterScheduleClassResponse> getMyEnrolledSchedule(
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate date) {
        return marketplaceService.getMyEnrolledSchedule(date);
    }

    /** POST /lessons/{lessonId}/checkin — gia sư bắt đầu buổi học. */
    @PostMapping("/lessons/{lessonId}/checkin")
    public Map<String, String> checkInLesson(@PathVariable Long lessonId) {
        marketplaceService.checkInLesson(lessonId);
        return Map.of("message", "Đã điểm danh vào buổi học");
    }

    /** POST /lessons/{lessonId}/checkout — gia sư kết thúc buổi học (buổi hoàn thành). */
    @PostMapping("/lessons/{lessonId}/checkout")
    public Map<String, String> checkOutLesson(@PathVariable Long lessonId) {
        marketplaceService.checkOutLesson(lessonId);
        return Map.of("message", "Đã kết thúc buổi học");
    }

    /** POST /lessons/{lessonId}/reschedule — gửi yêu cầu đổi lịch một buổi. */
    @PostMapping("/lessons/{lessonId}/reschedule")
    @ResponseStatus(HttpStatus.CREATED)
    public RescheduleRequestResponse requestReschedule(
            @PathVariable Long lessonId, @RequestBody RescheduleLessonRequest request) {
        return marketplaceService.requestReschedule(lessonId, request);
    }

    /** GET /lessons/requests — các yêu cầu đổi lịch/thêm buổi của các lớp mình tham gia. */
    @GetMapping("/lessons/requests")
    public List<RescheduleRequestResponse> listRescheduleRequests() {
        return marketplaceService.listMyRescheduleRequests();
    }

    /** POST /lessons/requests/{requestId}/decision — duyệt hoặc từ chối yêu cầu đổi lịch. */
    @PostMapping("/lessons/requests/{requestId}/decision")
    public Map<String, String> decideRescheduleRequest(
            @PathVariable Long requestId, @RequestBody RescheduleDecisionRequest decision) {
        marketplaceService.decideRescheduleRequest(requestId, decision);
        return Map.of(
                "message",
                Boolean.TRUE.equals(decision.getApprove())
                        ? "Đã duyệt — lịch đã được cập nhật"
                        : "Đã từ chối yêu cầu");
    }

    /** POST /lessons/requests/{requestId}/cancel — người gửi thu hồi yêu cầu. */
    @PostMapping("/lessons/requests/{requestId}/cancel")
    public Map<String, String> cancelRescheduleRequest(@PathVariable Long requestId) {
        marketplaceService.cancelRescheduleRequest(requestId);
        return Map.of("message", "Đã thu hồi yêu cầu");
    }

    /** POST /lessons/{lessonId}/attend?present= — điểm danh nhanh có mặt/vắng trong ngày học. */
    @PostMapping("/lessons/{lessonId}/attend")
    public Map<String, String> markAttendance(
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "true") boolean present) {
        marketplaceService.markAttendance(lessonId, present);
        return Map.of("message", present ? "Đã điểm danh có mặt" : "Đã đánh dấu vắng mặt");
    }

    @GetMapping("/tutors/search")
    public List<TutorSearchResponse> searchTutors(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Long subjectId) {
        return marketplaceService.searchTutors(keyword, subjectId);
    }

    @PostMapping("/favorites/{tutorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> addFavorite(@PathVariable Long tutorId) {
        marketplaceService.addFavorite(tutorId);
        return Map.of("message", "Đã thêm vào yêu thích");
    }

    @DeleteMapping("/favorites/{tutorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable Long tutorId) {
        marketplaceService.removeFavorite(tutorId);
    }

    @GetMapping("/favorites")
    public List<TutorSearchResponse> getFavorites() {
        return marketplaceService.getFavorites();
    }
}
