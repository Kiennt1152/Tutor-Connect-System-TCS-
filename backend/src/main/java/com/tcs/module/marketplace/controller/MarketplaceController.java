package com.tcs.module.marketplace.controller;

import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.response.ApplicantResponse;
import com.tcs.module.marketplace.dto.response.AssignmentResponse;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.LessonResponse;
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

    @GetMapping("/classes/mine")
    public List<ClassResponse> listMyClasses() {
        return marketplaceService.listMyClasses();
    }

    @GetMapping("/classes/{classId}")
    public ClassResponse getClass(@PathVariable Long classId) {
        return marketplaceService.getClass(classId);
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse createClass(@RequestBody CreateClassRequest request) {
        return marketplaceService.createClass(request);
    }

    @PutMapping("/classes/{classId}")
    public ClassResponse updateClass(@PathVariable Long classId, @RequestBody CreateClassRequest request) {
        return marketplaceService.updateClass(classId, request);
    }

    @PostMapping("/classes/{classId}/publish")
    public ClassResponse publishClass(@PathVariable Long classId) {
        return marketplaceService.publishClass(classId);
    }

    @PostMapping("/classes/{classId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> applyToClass(@PathVariable Long classId, @RequestBody ApplyClassRequest request) {
        marketplaceService.applyToClass(classId, request);
        return Map.of("message", "Đã gửi đơn ứng tuyển");
    }

    /** Lớp mà gia sư đang đăng nhập đã nộp đơn — UI dùng để hiện "Đã ứng tuyển". */
    @GetMapping("/applications/mine")
    public List<Long> listMyAppliedClassIds() {
        return marketplaceService.listMyAppliedClassIds();
    }

    @GetMapping("/classes/{classId}/applications")
    public List<ApplicantResponse> listApplicants(@PathVariable Long classId) {
        return marketplaceService.listApplicants(classId);
    }

    @PostMapping("/classes/{classId}/applications/{applicationId}/choose")
    public Map<String, String> chooseApplicant(
            @PathVariable Long classId, @PathVariable Long applicationId) {
        marketplaceService.chooseApplicant(classId, applicationId);
        return Map.of("message", "Đã chọn gia sư — đang chờ gia sư nhận lớp");
    }

    // --- Phía gia sư: nhận lớp → lịch dạy → điểm danh ---

    @GetMapping("/assignments/mine")
    public List<AssignmentResponse> listMyAssignments() {
        return marketplaceService.listMyAssignments();
    }

    @PostMapping("/assignments/{assignmentId}/accept")
    public Map<String, String> acceptAssignment(@PathVariable Long assignmentId) {
        marketplaceService.acceptAssignment(assignmentId);
        return Map.of("message", "Đã nhận lớp — lịch dạy đã được tạo");
    }

    @PostMapping("/assignments/{assignmentId}/decline")
    public Map<String, String> declineAssignment(@PathVariable Long assignmentId) {
        marketplaceService.declineAssignment(assignmentId);
        return Map.of("message", "Đã từ chối lớp");
    }

    @GetMapping("/lessons/mine")
    public List<LessonResponse> listMyLessons() {
        return marketplaceService.listMyLessons();
    }

    @PostMapping("/lessons/{lessonId}/checkin")
    public Map<String, String> checkInLesson(@PathVariable Long lessonId) {
        marketplaceService.checkInLesson(lessonId);
        return Map.of("message", "Đã điểm danh vào buổi học");
    }

    @PostMapping("/lessons/{lessonId}/checkout")
    public Map<String, String> checkOutLesson(@PathVariable Long lessonId) {
        marketplaceService.checkOutLesson(lessonId);
        return Map.of("message", "Đã kết thúc buổi học");
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
