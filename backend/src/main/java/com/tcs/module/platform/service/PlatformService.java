package com.tcs.module.platform.service;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.dto.response.VerificationRequestResponse;
import com.tcs.module.profile.enums.UserRole;
import java.util.List;

public interface PlatformService {

    PageUserListResponse getUsers(int page, int size, UserStatus status, UserRole role, String keyword);

    UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);

    DashboardResponse getDashboard();

    List<VerificationRequestResponse> listVerificationRequests();

    VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request);

    List<ReportResponse> listReports();
}
