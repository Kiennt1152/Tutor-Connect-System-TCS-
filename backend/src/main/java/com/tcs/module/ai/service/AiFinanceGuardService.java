package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * [UC-65] BẢO VỆ DỮ LIỆU TÀI CHÍNH & CHỐNG RÒ RỈ SỐ DƯ (AI FINANCE GUARD)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Lớp kiểm soát bảo mật tài chính ngăn chặn mô hình AI vô tình tiết lộ số dư ví hoặc giao dịch của người dùng khác.
 * 
 * Chức năng chính:
 *   1. Phát hiện truy vấn tài chính chéo: Chặn câu hỏi yêu cầu xem số dư ví của tài khoản khác.
 *   2. Làm sạch dữ liệu nhạy cảm: Loại bỏ mã thẻ, số tài khoản ngân hàng chi tiết khỏi câu trả lời của AI.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận câu truy vấn và ngữ cảnh người dùng hiện tại.
 *   - Bước 2: Kiểm tra đối chiếu quyền hạn sở hữu tài sản tài chính.
 *   - Bước 3: Cho phép tiếp tục hoặc kích hoạt phản hồi từ chối an toàn.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class AiFinanceGuardService {

    private final AiFallbackService fallbackService;

    /**
     * Check if user has permission to query personal finance data.
     * @return Error message if unauthorized, null if allowed
     */
    public String checkFinanceAccess(AiDomain domain, String rawMessage, String userRole, Long userId) {
        if (domain != AiDomain.FINANCE_WALLET || rawMessage == null) {
            return null;
        }

        String lowerQuery = rawMessage.toLowerCase();
        boolean isPersonalQuery = lowerQuery.contains("của tôi") || 
                                  lowerQuery.contains("lương của tôi") || 
                                  lowerQuery.contains("thu nhập của tôi") || 
                                  lowerQuery.contains("ví của tôi") || 
                                  lowerQuery.contains("tiền của tôi");

        if (isPersonalQuery && (userId == null || 
            (!"TUTOR".equals(userRole) && !"TUTOR_CENTER".equals(userRole)))) {
            if (fallbackService != null && fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance") != null) {
                return fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance").message();
            }
            return "Chức năng xem thông tin tài chính cá nhân yêu cầu tài khoản Gia sư hoặc Trung tâm gia sư.";
        }

        return null;
    }
}
