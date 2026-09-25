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

    /**
     * [UC-59] Lấy danh sách toàn bộ các bản tin thông báo hệ thống dành cho Quản trị viên.
     * 
     * @return Danh sách AnnouncementResponse sắp xếp theo thời gian tạo mới nhất
     */
    List<AnnouncementResponse> getAnnouncements();

    /**
     * [UC-59] Xem thông tin chi tiết một bản tin thông báo theo ID.
     * 
     * @param announcementId Định danh bản tin thông báo
     * @return AnnouncementResponse chi tiết nội dung, đối tượng hướng tới và thời gian hiển thị
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy thông báo
     */
    AnnouncementResponse getAnnouncement(Long announcementId);

    /**
     * [UC-59] Tạo mới một bản tin thông báo toàn sàn.
     * 
     * Luồng xử lý:
     * 1. Xác thực tính hợp lệ của tiêu đề, nội dung và khoảng thời gian hiệu lực.
     * 2. Tự động sinh ID bản tin tăng tiến và gán thông tin Quản trị viên khởi tạo.
     * 3. Lưu trữ vào cấu hình tham số hệ thống dạng JSON Array.
     * 4. Ghi nhận nhật ký kiểm toán CREATE_ANNOUNCEMENT.
     * 
     * @param request Dữ liệu tạo mới bản tin thông báo
     * @return AnnouncementResponse thông tin bản tin vừa khởi tạo
     */
    AnnouncementResponse createAnnouncement(UpsertAnnouncementRequest request);

    /**
     * [UC-59] Cập nhật thông tin bản tin thông báo hệ thống đã có.
     * 
     * @param announcementId Định danh bản tin cần cập nhật
     * @param request Dữ liệu cập nhật mới
     * @return AnnouncementResponse thông tin bản tin sau khi chỉnh sửa
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy bản tin
     */
    AnnouncementResponse updateAnnouncement(Long announcementId, UpsertAnnouncementRequest request);

    /**
     * [UC-59] Xóa bản tin thông báo khỏi hệ thống.
     * 
     * @param announcementId Định danh bản tin cần xóa
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy bản tin
     */
    void deleteAnnouncement(Long announcementId);

    /**
     * [UC-59] Truy vấn danh sách các bản tin thông báo công khai đang có hiệu lực theo vai trò người dùng.
     * 
     * Luồng xử lý:
     * 1. Lọc các thông báo có trạng thái kích hoạt (active = true).
     * 2. Kiểm tra khoảng thời gian hiệu lực: thời điểm hiện tại phải nằm giữa startsAt và endsAt (nếu có).
     * 3. Lọc theo đối tượng mục tiêu: khớp với role của người dùng hoặc áp dụng cho tất cả (targetRole = null).
     * 
     * @param role Vai trò người dùng hiện tại (null nếu là khách vãng lai)
     * @return Danh sách AnnouncementResponse đang hiển thị phù hợp
     */
    List<AnnouncementResponse> getVisibleAnnouncements(UserRole role);
}
