package com.tcs.module.center.controller;

import com.tcs.module.center.dto.request.ApplicationDecisionBody;
import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.SaveRecruitmentPostRequest;
import com.tcs.module.center.dto.request.UpdateMembershipStatusBody;
import com.tcs.module.center.dto.response.CenterTutorResponse;
import com.tcs.module.center.dto.response.RecruitmentApplicationResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.service.CenterService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        return centerService.decideApplication(recruitmentAppId, request.isApprove());
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
}
