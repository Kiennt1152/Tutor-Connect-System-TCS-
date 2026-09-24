package com.tcs.module.ai.service;

import com.tcs.module.ai.constants.AiConstants;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * [UC-65] LỌC QUYỀN TRUY CẬP TRI THỨC THEO VAI TRÒ (PERMISSION FILTER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Lọc các đoạn văn bản tri thức dựa trên vai trò của người dùng (GUEST, CLIENT, TUTOR, PLATFORM_ADMIN).
 * 
 * Chức năng chính:
 *   1. Lọc chunk nội bộ: Loại bỏ các tài liệu quy trình nội bộ của Admin khỏi kết quả tìm kiếm của khách.
 *   2. Bảo vệ tài liệu nhạy cảm: Đảm bảo người dùng chỉ xem được các tài liệu hướng dẫn phù hợp với vai trò.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận danh sách chunk tri thức được truy xuất từ cơ sở dữ liệu.
 *   - Bước 2: Đối chiếu trường `allowed_roles` của từng chunk với vai trò của người dùng.
 *   - Bước 3: Loại bỏ các chunk không đủ thẩm quyền trước khi gửi cho Prompt Builder.
 * ============================================================================
 */
@Service
public class AiPermissionFilterService {

    public boolean canAccess(AiKnowledgeChunk chunk, String userRole, Long userId) {
        String visibility = chunk.getVisibility() != null ? chunk.getVisibility() : AiConstants.VISIBILITY_PUBLIC;
        
        if (AiConstants.VISIBILITY_PUBLIC.equals(visibility)) {
            return true;
        }

        if (userRole == null || "GUEST".equals(userRole)) {
            return false;
        }

        if ("PLATFORM_ADMIN".equals(userRole)) {
            return true;
        }

        if (AiConstants.VISIBILITY_OWNER_PRIVATE.equals(visibility)) {
            return userId != null && userId.equals(chunk.getOwnerUserId());
        }

        if (AiConstants.VISIBILITY_ROLE_RESTRICTED.equals(visibility)) {
            String minRole = chunk.getMinRole();
            if (minRole == null) return true;
            if ("TUTOR_CENTER".equals(minRole) && "TUTOR_CENTER".equals(userRole)) return true;
            if ("TUTOR".equals(minRole) && ("TUTOR".equals(userRole) || "TUTOR_CENTER".equals(userRole))) return true;
            if ("CLIENT".equals(minRole) && "CLIENT".equals(userRole)) return true;
            return false;
        }

        if (AiConstants.VISIBILITY_ADMIN_ONLY.equals(visibility)) {
            return false; // Already checked for PLATFORM_ADMIN above
        }

        return false;
    }
}
