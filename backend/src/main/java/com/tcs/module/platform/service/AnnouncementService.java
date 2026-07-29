package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.request.UpsertAnnouncementRequest;
import com.tcs.module.platform.dto.response.AnnouncementResponse;
import com.tcs.module.profile.enums.UserRole;
import java.util.List;

public interface AnnouncementService {

    List<AnnouncementResponse> getAnnouncements();

    AnnouncementResponse getAnnouncement(Long announcementId);

    AnnouncementResponse createAnnouncement(UpsertAnnouncementRequest request);

    AnnouncementResponse updateAnnouncement(Long announcementId, UpsertAnnouncementRequest request);

    void deleteAnnouncement(Long announcementId);

    /** Public: danh sách announcement đang hiển thị cho vai trò hiện tại (role = null nếu khách). */
    List<AnnouncementResponse> getVisibleAnnouncements(UserRole role);
}
