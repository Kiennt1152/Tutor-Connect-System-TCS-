package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.request.UpsertAnnouncementRequest;
import com.tcs.module.platform.dto.response.AnnouncementResponse;
import com.tcs.module.profile.enums.UserRole;
import java.util.List;

/**
 * ============================================================================
 * [UC-59] QUẢN LÝ BẢN TIN & THÔNG BÁO TOÀN SÀN (ANNOUNCEMENT SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Định nghĩa các thao tác quản lý bản tin thông báo chính sách, sự kiện và bảo trì hệ thống.
 *   - Hỗ trợ phân phối thông báo đúng đối tượng mục tiêu: Tất cả, Gia sư, Phụ huynh, Trung tâm.
 * 
 * Chức năng chính:
 *   1. Quản lý bản tin: Thêm mới, chỉnh sửa, xóa và truy vấn danh sách thông báo.
 *   2. Phân phối theo vai trò: Lọc danh sách thông báo hiển thị phù hợp với người dùng hiện tại.
 *   3. Điều phối thời gian: Cấu hình mốc thời gian hiển thị bắt đầu và kết thúc của thông báo.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên cập nhật nội dung thông báo (`createAnnouncement`, `updateAnnouncement`).
 *   - Bước 2: Hệ thống lưu trữ cấu hình dưới dạng tham số động.
 *   - Bước 3: Người dùng truy vấn danh sách thông báo khả dụng theo vai trò (`getVisibleAnnouncements`).
 *   - Bước 4: Hiển thị bản tin trên giao diện ứng dụng.
 * ============================================================================
 */
public interface AnnouncementService {

    List<AnnouncementResponse> getAnnouncements();

    AnnouncementResponse getAnnouncement(Long announcementId);

    AnnouncementResponse createAnnouncement(UpsertAnnouncementRequest request);

    AnnouncementResponse updateAnnouncement(Long announcementId, UpsertAnnouncementRequest request);

    void deleteAnnouncement(Long announcementId);

    /** Public: danh sách announcement đang hiển thị cho vai trò hiện tại (role = null nếu khách). */
    List<AnnouncementResponse> getVisibleAnnouncements(UserRole role);
}
