package com.tcs.module.center.controller;

import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.CreateRecruitmentPostRequest;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.service.CenterService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
