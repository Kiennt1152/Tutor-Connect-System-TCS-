package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.dto.response.PagePenaltyResponse;
import com.tcs.module.platform.dto.response.PenaltyResponse;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;

/**
 * ============================================================================
 * [UC-63] QUẢN LÝ & THI HÀNH CHẾ TÀI XỬ PHẠT (PENALTY SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Định nghĩa các nghiệp vụ ban hành và quản lý các quyết định chế tài kỷ luật người dùng vi phạm.
 *   - Bảo vệ trật tự và kỷ cương vận hành của nền tảng kết nối gia sư Tutor Connect.
 * 
 * Chức năng chính:
 *   1. Danh sách chế tài: Phân trang và tra cứu án phạt theo người dùng, trạng thái và loại hình phạt.
 *   2. Ban hành án phạt: Áp dụng các mức chế tài (Cảnh cáo, Cấm chat, Cấm mở lớp, Khóa tài khoản).
 *   3. Thu hồi án phạt: Hủy bỏ hiệu lực án phạt trước thời hạn khi có giải trình hợp lệ.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận yêu cầu xử phạt từ Quản trị viên (`issuePenalty`).
 *   - Bước 2: Kiểm tra ràng buộc và lưu vết án phạt vào CSDL.
 *   - Bước 3: Đồng bộ trạng thái tài khoản người dùng và gửi thông báo kỷ luật.
 *   - Bước 4: Tiếp nhận yêu cầu thu hồi và phục hồi quyền lợi (`revokePenalty`).
 * ============================================================================
 */
public interface PenaltyService {
    PagePenaltyResponse listPenalties(Long userId, UserPenaltyStatus status, UserPenaltyType type, String sourceType, int page, int size);

    default PagePenaltyResponse listPenalties(Long userId, UserPenaltyStatus status, UserPenaltyType type, int page, int size) {
        return listPenalties(userId, status, type, null, page, size);
    }

    PenaltyResponse issuePenalty(IssuePenaltyRequest request);
    PenaltyResponse revokePenalty(Long penaltyId, RevokePenaltyRequest request);
}
