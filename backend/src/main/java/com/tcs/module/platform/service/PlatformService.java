package com.tcs.module.platform.service;

import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.request.UpsertAnnouncementRequest;
import com.tcs.module.platform.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.platform.dto.response.AnnouncementResponse;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.SystemParameterResponse;
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

    /** Chi tiet yeu cau xac minh: ho so nguoi nop + tai lieu (chi doc). */
    VerificationDetailResponse getVerificationDetail(Long verificationId);

    VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request);

    List<ReportResponse> listReports();

    /** 4P.7: quan ly tham so he thong (system_parameters). */
    List<SystemParameterResponse> listSystemParameters(String prefix);

    SystemParameterResponse createSystemParameter(UpsertSystemParameterRequest request);

    SystemParameterResponse updateSystemParameter(Long parameterId, UpsertSystemParameterRequest request);

    void deleteSystemParameter(Long parameterId);

    /** 4P.7: quan ly thong bao he thong (announcements). */
    List<AnnouncementResponse> listAnnouncements();

    AnnouncementResponse createAnnouncement(UpsertAnnouncementRequest request);

    AnnouncementResponse updateAnnouncement(Long announcementId, UpsertAnnouncementRequest request);

    void deleteAnnouncement(Long announcementId);
}
