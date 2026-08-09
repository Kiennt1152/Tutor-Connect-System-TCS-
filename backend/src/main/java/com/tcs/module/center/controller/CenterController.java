package com.tcs.module.center.controller;

import com.tcs.module.center.dto.request.ApplicationDecisionBody;
import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.AssignTutorRequest;
import com.tcs.module.center.dto.request.RejectClassRequestBody;
import com.tcs.module.center.dto.request.SaveContractTemplateRequest;
import com.tcs.module.center.dto.request.RescheduleDecisionBody;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.request.SaveRecruitmentPostRequest;
import com.tcs.module.center.dto.request.SubstitutionDecisionBody;
import com.tcs.module.center.dto.request.UpdateMembershipStatusBody;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.CenterTutorResponse;
import com.tcs.module.center.dto.response.RecruitmentApplicationResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.dto.response.RescheduleResponse;
import com.tcs.module.center.dto.response.SubstitutionResponse;
import com.tcs.module.center.dto.response.TutorOptionResponse;
import com.tcs.module.center.service.CenterService;
import com.tcs.module.center.dto.response.ContractTemplateResponse;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    // ===== FT-33: tin tuyển gia sư — phía gia sư =====

    /** Tin đang mở, cho gia sư tự do xem. */
    @GetMapping("/recruitment")
    public List<RecruitmentPostResponse> listOpenRecruitmentPosts() {
        return centerService.listOpenRecruitmentPosts();
    }

    @PostMapping("/recruitment/{recruitmentId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> applyToRecruitment(
            @PathVariable Long recruitmentId, @RequestBody ApplyRecruitmentRequest request) {
        centerService.applyToRecruitment(recruitmentId, request);
        return Map.of("message", "Đã gửi đơn ứng tuyển");
    }

    @GetMapping("/recruitment/applications/mine")
    public List<RecruitmentApplicationResponse> myApplications() {
        return centerService.listMyApplications();
    }

    // ===== FT-33: tin tuyển gia sư — phía trung tâm =====

    @GetMapping("/recruitment/my-posts")
    public List<RecruitmentPostResponse> listMyRecruitmentPosts() {
        return centerService.listMyRecruitmentPosts();
    }

    @PostMapping("/recruitment")
    @ResponseStatus(HttpStatus.CREATED)
    public RecruitmentPostResponse createRecruitmentPost(
            @RequestBody SaveRecruitmentPostRequest request) {
        return centerService.createRecruitmentPost(request);
    }

    @PutMapping("/recruitment/{recruitmentId}")
    public RecruitmentPostResponse updateRecruitmentPost(
            @PathVariable Long recruitmentId, @RequestBody SaveRecruitmentPostRequest request) {
        return centerService.updateRecruitmentPost(recruitmentId, request);
    }

    @PostMapping("/recruitment/{recruitmentId}/publish")
    public RecruitmentPostResponse publishRecruitmentPost(@PathVariable Long recruitmentId) {
        return centerService.publishRecruitmentPost(recruitmentId);
    }

    @PostMapping("/recruitment/{recruitmentId}/close")
    public RecruitmentPostResponse closeRecruitmentPost(@PathVariable Long recruitmentId) {
        return centerService.closeRecruitmentPost(recruitmentId);
    }

    @GetMapping("/recruitment/{recruitmentId}/applications")
    public List<RecruitmentApplicationResponse> listApplications(@PathVariable Long recruitmentId) {
        return centerService.listApplications(recruitmentId);
    }

    @PostMapping("/recruitment/applications/{recruitmentAppId}/decision")
    public RecruitmentApplicationResponse decideApplication(
            @PathVariable Long recruitmentAppId, @RequestBody ApplicationDecisionBody request) {
        return centerService.decideApplication(
                recruitmentAppId, request.isApprove(),
                request.getContractTemplateId(), request.getContractContent());
    }

    // ===== Quản lý danh sách gia sư của trung tâm =====

    @GetMapping("/members")
    public List<CenterTutorResponse> listMyTutors() {
        return centerService.listMyTutors();
    }

    @PatchMapping("/members/{membershipId}/status")
    public CenterTutorResponse updateMembershipStatus(
            @PathVariable Long membershipId, @RequestBody UpdateMembershipStatusBody request) {
        return centerService.updateMembershipStatus(membershipId, request.getStatus());
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

    // ===== Yêu cầu mở lớp do phụ huynh gửi tới trung tâm =====

    @GetMapping("/class-requests")
    public List<ClassRequestResponse> incomingClassRequests() {
        return centerService.listIncomingClassRequests();
    }

    @PostMapping("/class-requests/{requestId}/start-search")
    public Map<String, String> startSearchClassRequest(@PathVariable String requestId) {
        centerService.startSearch(requestId);
        return Map.of("message", "Đã nhận tìm gia sư cho yêu cầu");
    }

    @PostMapping("/class-requests/{requestId}/candidates/{tutorId}")
    public Map<String, String> proposeTutor(
            @PathVariable String requestId, @PathVariable Long tutorId) {
        centerService.proposeTutor(requestId, tutorId);
        return Map.of("message", "Đã đề cử gia sư cho yêu cầu");
    }

    @DeleteMapping("/class-requests/{requestId}/candidates/{tutorId}")
    public Map<String, String> removeCandidate(
            @PathVariable String requestId, @PathVariable Long tutorId) {
        centerService.removeCandidate(requestId, tutorId);
        return Map.of("message", "Đã gỡ gia sư khỏi đề cử");
    }

    @PostMapping("/class-requests/{requestId}/recruitment")
    public ClassRequestResponse postRecruitmentForRequest(@PathVariable String requestId) {
        return centerService.postRecruitmentForRequest(requestId);
    }

    @GetMapping("/class-requests/{requestId}/applications")
    public List<RecruitmentApplicationResponse> listRequestApplications(
            @PathVariable String requestId) {
        return centerService.listRequestApplications(requestId);
    }

    @PostMapping("/class-requests/{requestId}/accept")
    public CenterClassResponse acceptClassRequest(
            @PathVariable String requestId, @RequestBody SaveClassRequest body) {
        return centerService.acceptClassRequest(requestId, body);
    }

    @PostMapping("/class-requests/{requestId}/reject")
    public Map<String, String> rejectClassRequest(
            @PathVariable String requestId, @RequestBody RejectClassRequestBody body) {
        centerService.rejectClassRequest(requestId, body.getReason());
        return Map.of("message", "Đã từ chối yêu cầu mở lớp");
    }

    @PostMapping("/class-requests/{requestId}/give-up")
    public Map<String, String> giveUpClassRequest(
            @PathVariable String requestId, @RequestBody(required = false) RejectClassRequestBody body) {
        centerService.giveUpClassRequest(requestId, body != null ? body.getReason() : null);
        return Map.of("message", "Đã đóng yêu cầu và thông báo cho phụ huynh");
    }

    // ===== Quản lý mẫu hợp đồng =====

    @GetMapping("/contract-templates")
    public List<ContractTemplateResponse> listContractTemplates() {
        return centerService.listContractTemplates();
    }

    @PostMapping("/contract-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractTemplateResponse createContractTemplate(
            @RequestBody SaveContractTemplateRequest request) {
        return centerService.createContractTemplate(request);
    }

    @PutMapping("/contract-templates/{templateId}")
    public ContractTemplateResponse updateContractTemplate(
            @PathVariable Long templateId, @RequestBody SaveContractTemplateRequest request) {
        return centerService.updateContractTemplate(templateId, request);
    }

    // ===== Thông tin trung tâm trên hợp đồng (BÊN A) =====

    @GetMapping("/contract-info")
    public com.tcs.module.center.dto.response.CenterContractInfoResponse getContractInfo() {
        return centerService.getContractInfo();
    }

    @PutMapping("/contract-info")
    public com.tcs.module.center.dto.response.CenterContractInfoResponse saveContractInfo(
            @RequestBody com.tcs.module.center.dto.request.SaveCenterContractInfoRequest request) {
        return centerService.saveContractInfo(request);
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

    @PostMapping("/classes/{classId}/close-enrollment")
    public CenterClassResponse closeEnrollment(@PathVariable Long classId) {
        return centerService.closeEnrollment(classId);
    }

    @PostMapping("/classes/{classId}/activate")
    public CenterClassResponse activateClass(@PathVariable Long classId) {
        return centerService.activateClass(classId);
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

    @PostMapping("/classes/{classId}/assign-assistant")
    public CenterClassResponse assignAssistant(
            @PathVariable Long classId, @RequestBody AssignTutorRequest request) {
        return centerService.assignAssistant(classId, request.getTutorId());
    }

    @DeleteMapping("/classes/{classId}/assign-assistant")
    public CenterClassResponse unassignAssistant(@PathVariable Long classId) {
        return centerService.unassignAssistant(classId);
    }

    // ===== Lịch lớp CENTER =====

    @GetMapping("/schedule")
    public List<CenterScheduleClassResponse> getSchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return centerService.getSchedule(date);
    }

    // ===== Duyệt yêu cầu dời buổi học =====

    @GetMapping("/reschedules")
    public List<RescheduleResponse> listReschedules() {
        return centerService.listReschedules();
    }

    @PostMapping("/reschedules/decision")
    public RescheduleResponse decideReschedule(@RequestBody RescheduleDecisionBody request) {
        return centerService.decideReschedule(request);
    }

    // ===== Duyệt yêu cầu nhờ gia sư phụ dạy thay =====

    @GetMapping("/substitutions")
    public List<SubstitutionResponse> listSubstitutions() {
        return centerService.listSubstitutions();
    }

    @PostMapping("/substitutions/decision")
    public SubstitutionResponse decideSubstitution(@RequestBody SubstitutionDecisionBody request) {
        return centerService.decideSubstitution(request);
    }
}
