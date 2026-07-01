package com.tcs.module.center.service;

import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.CreateRecruitmentPostRequest;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import java.util.List;

public interface CenterService {

    List<RecruitmentPostResponse> listRecruitmentPosts();

    RecruitmentPostResponse createRecruitmentPost(CreateRecruitmentPostRequest request);

    RecruitmentPostResponse publishRecruitmentPost(Long recruitmentId);

    void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request);
}
