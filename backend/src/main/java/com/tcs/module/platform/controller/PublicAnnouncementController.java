package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.response.AnnouncementResponse;
import com.tcs.module.platform.service.AnnouncementService;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * [UC-59] XEM THÔNG BÁO CÔNG KHAI TOÀN SÀN (PUBLIC ANNOUNCEMENT CONTROLLER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Cung cấp API công khai cho phép khách vãng lai và người dùng đã đăng nhập tiếp nhận các bản tin quan trọng.
 *   - Tự động điều phối hiển thị thông báo theo vai trò tài khoản hiện tại.
 * 
 * Chức năng chính:
 *   1. Xem danh sách thông báo công khai: Trả về các thông báo đang kích hoạt (Active) và còn trong thời hạn hiển thị.
 *   2. Phân phối theo vai trò người dùng: Nhận diện vai trò từ phiên bảo mật để chỉ hiển thị thông báo phù hợp.
 *   3. Bảo vệ dữ liệu bảo mật: Không tiết lộ các thông tin nội bộ của quản trị viên qua luồng công khai.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Ứng dụng client gửi request tới `/api/home/announcements`.
 *   - Bước 2: Bộ điều khiển phân giải vai trò của người dùng (`currentRoleOrNull`) từ Spring Security.
 *   - Bước 3: Gọi dịch vụ `announcementService.getVisibleAnnouncements` với vai trò tương ứng.
 *   - Bước 4: Trả về danh sách thông báo hợp lệ hiển thị lên thanh banner hoặc trang chủ.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/home/announcements")
@RequiredArgsConstructor
public class PublicAnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public List<AnnouncementResponse> getVisibleAnnouncements() {
        return announcementService.getVisibleAnnouncements(currentRoleOrNull());
    }

    private UserRole currentRoleOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getRole();
        }
        return null;
    }
}
