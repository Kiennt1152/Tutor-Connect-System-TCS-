package com.tcs.module.center.controller;

import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.AssignTutorRequest;
import com.tcs.module.center.dto.request.CreateRecruitmentPostRequest;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.dto.response.TutorOptionResponse;
import com.tcs.module.center.service.CenterService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/center")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centerService;

    @GetMapping("/recruitment")
    public List<RecruitmentPostResponse> listRecruitmentPosts() {
        return centerService.listRecruitmentPosts();
    }

    @PostMapping("/recruitment")
    @ResponseStatus(HttpStatus.CREATED)
    public RecruitmentPostResponse createRecruitmentPost(@RequestBody CreateRecruitmentPostRequest request) {
        return centerService.createRecruitmentPost(request);
    }

    @PostMapping("/recruitment/{recruitmentId}/publish")
    public RecruitmentPostResponse publishRecruitmentPost(@PathVariable Long recruitmentId) {
        return centerService.publishRecruitmentPost(recruitmentId);
    }

    @PostMapping("/recruitment/{recruitmentId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> applyToRecruitment(
            @PathVariable Long recruitmentId, @RequestBody ApplyRecruitmentRequest request) {
        centerService.applyToRecruitment(recruitmentId, request);
        return Map.of("message", "Đã gửi đơn ứng tuyển");
    }

    // ===== UC-14-B: Manage Classes (Tutor Center) =====

    @GetMapping("/classes")
    public List<CenterClassResponse> listMyClasses() {
        return centerService.listMyClasses();
    }

    @GetMapping("/classes/{classId}")
    public CenterClassResponse getMyClass(@PathVariable Long classId) {
        return centerService.getMyClass(classId);
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public CenterClassResponse createClass(@RequestBody SaveClassRequest request) {
        return centerService.createClass(request);
    }

    @PutMapping("/classes/{classId}")
    public CenterClassResponse updateClass(
            @PathVariable Long classId, @RequestBody SaveClassRequest request) {
        return centerService.updateClass(classId, request);
    }

    @PostMapping("/classes/{classId}/publish")
    public CenterClassResponse publishClass(@PathVariable Long classId) {
        return centerService.publishClass(classId);
    }

    @GetMapping("/tutors")
    public List<TutorOptionResponse> listTutors(@RequestParam(required = false) Long classId) {
        return centerService.listTutors(classId);
    }

    @PostMapping("/classes/{classId}/assign-tutor")
    public CenterClassResponse assignTutor(
            @PathVariable Long classId, @RequestBody AssignTutorRequest request) {
        return centerService.assignTutor(classId, request.getTutorId());
    }

    @DeleteMapping("/classes/{classId}/assign-tutor")
    public CenterClassResponse unassignTutor(@PathVariable Long classId) {
        return centerService.unassignTutor(classId);
    }

    // ===== Lịch lớp CENTER =====

    @GetMapping("/schedule")
    public List<CenterScheduleClassResponse> getSchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return centerService.getSchedule(date);
    }
}
