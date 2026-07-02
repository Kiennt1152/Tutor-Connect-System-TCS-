package com.tcs.module.platform.service;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.dto.response.VerificationDetailResponse;
import com.tcs.module.platform.dto.response.VerificationRequestResponse;
import com.tcs.module.profile.enums.UserRole;
import java.util.List;

public interface PlatformService {

    PageUserListResponse getUsers(int page, int size, UserStatus status, UserRole role, String keyword);

    UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);

    DashboardResponse getDashboard();

    List<VerificationRequestResponse> listVerificationRequests();

    /** UC-11: mo mot yeu cau de xet duyet — SUBMITTED -> UNDER_REVIEW + ghi lich su (BR-01). */
    VerificationDetailResponse openVerification(Long verificationId);

    VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request);

    List<ReportResponse> listReports();
}
