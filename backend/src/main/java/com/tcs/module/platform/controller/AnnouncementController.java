package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.request.UpsertAnnouncementRequest;
import com.tcs.module.platform.dto.response.AnnouncementResponse;
import com.tcs.module.platform.service.AnnouncementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * [UC-59] QUẢN LÝ BẢN TIN & THÔNG BÁO TOÀN SÀN (ANNOUNCEMENT CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Quản trị viên quản lý và phát hành các thông báo chính sách, sự kiện và lịch bảo trì tới các nhóm người dùng.
 *   - Phân phối tin tức chính xác tới từng đối tượng mục tiêu: Gia sư, Phụ huynh, Trung tâm hoặc toàn bộ hệ thống.
 * 
 * Chức năng chính:
 *   1. Danh sách thông báo: Truy xuất toàn bộ danh sách thông báo quản trị viên đã cấu hình.
 *   2. Tạo và cập nhật: Soạn thảo thông báo mới hoặc điều chỉnh nội dung, liên kết và thời hạn hiển thị.
 *   3. Chuyển đổi trạng thái: Bật hoặc tắt nhanh hiển thị của bản tin trên trang chủ và giao diện người dùng.
 *   4. Xóa thông báo: Thu hồi vĩnh viễn các bản tin không còn phù hợp.
 *   5. Ghi vết kiểm toán: Lưu lịch sử thao tác vào Audit Log phục vụ thanh tra.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên truy vấn danh sách thông báo qua API `getAnnouncements`.
 *   - Bước 2: Tạo hoặc cập nhật thông báo với dữ liệu đối tượng và thời gian bắt đầu/kết thúc (`upsertAnnouncement`).
 *   - Bước 3: Chuyển đổi trạng thái kích hoạt thông báo khi cần (`toggleAnnouncement`).
 *   - Bước 4: Xóa thông báo đã hết hạn hiệu lực (`deleteAnnouncement`).
 * ============================================================================
 */
@RestController
@RequestMapping("/api/platform/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * [UC-64]: Truy xuất toàn bộ danh sách các bản tin thông báo quản trị viên đã cấu hình.
     * 
     * @return Danh sách các bản tin thông báo {@link AnnouncementResponse}
     */
    @GetMapping
    public List<AnnouncementResponse> getAnnouncements() {
        return announcementService.getAnnouncements();
    }

    /**
     * [UC-64]: Xem chi tiết một bản tin thông báo theo mã định danh.
     * 
     * @param announcementId ID bản tin thông báo
     * @return {@link AnnouncementResponse} Chi tiết tiêu đề, nội dung, đối tượng hướng tới và thời hạn
     */
    @GetMapping("/{announcementId}")
    public AnnouncementResponse getAnnouncement(@PathVariable Long announcementId) {
        return announcementService.getAnnouncement(announcementId);
    }

    /**
     * [UC-64]: Tạo mới một bản tin thông báo phát thanh trên toàn hệ thống.
     * 
     * @param request Dữ liệu thông báo mới {@link UpsertAnnouncementRequest}
     * @return {@link AnnouncementResponse} Bản tin thông báo vừa được tạo
     */
    @PostMapping
    public AnnouncementResponse createAnnouncement(@Valid @RequestBody UpsertAnnouncementRequest request) {
        return announcementService.createAnnouncement(request);
    }

    /**
     * [UC-64]: Cập nhật nội dung, liên kết đính kèm hoặc thay đổi trạng thái kích hoạt của bản tin.
     * 
     * @param announcementId ID bản tin cần chỉnh sửa
     * @param request Dữ liệu cập nhật {@link UpsertAnnouncementRequest}
     * @return {@link AnnouncementResponse} Bản tin sau khi cập nhật
     */
    @PatchMapping("/{announcementId}")
    public AnnouncementResponse updateAnnouncement(
            @PathVariable Long announcementId, @Valid @RequestBody UpsertAnnouncementRequest request) {
        return announcementService.updateAnnouncement(announcementId, request);
    }

    /**
     * [UC-64]: Xóa vĩnh viễn một bản tin thông báo khỏi hệ thống.
     * 
     * @param announcementId ID bản tin cần xóa
     */
    @DeleteMapping("/{announcementId}")
    public void deleteAnnouncement(@PathVariable Long announcementId) {
        announcementService.deleteAnnouncement(announcementId);
    }
}
