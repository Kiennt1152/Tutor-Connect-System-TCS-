package com.tcs.module.center.service;

import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.CreateRecruitmentPostRequest;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import java.util.List;

public interface CenterService {

    List<RecruitmentPostResponse> listRecruitmentPosts();

    RecruitmentPostResponse createRecruitmentPost(CreateRecruitmentPostRequest request);

    RecruitmentPostResponse publishRecruitmentPost(Long recruitmentId);

    void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request);

    // UC-14-B: Quản lý lớp học của Trung tâm gia sư.
    List<CenterClassResponse> listMyClasses();

    CenterClassResponse getMyClass(Long classId);

    CenterClassResponse createClass(SaveClassRequest request);

    CenterClassResponse updateClass(Long classId, SaveClassRequest request);
}
