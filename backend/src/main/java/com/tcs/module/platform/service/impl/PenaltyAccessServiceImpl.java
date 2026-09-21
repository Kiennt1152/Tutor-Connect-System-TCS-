package com.tcs.module.platform.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.repository.UserPenaltyRepository;
import com.tcs.module.platform.service.PenaltyAccessService;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * [UC-63] HIỆN THỰC CỔNG CHẮN KIỂM SOÁT TÍNH NĂNG (PENALTY ACCESS SERVICE IMPL)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Hiện thực cổng chắn bảo vệ các tính năng nghiệp vụ trước các tài khoản đang chịu chế tài xử phạt.
 *   - Chặn tức thì các hành vi vi phạm khi người dùng cố tình thực hiện thao tác bị cấm.
 * 
 * Chức năng chính:
 *   1. Kiểm tra án phạt hiệu lực: Truy vấn các án phạt `FEATURE_RESTRICTION` chưa hết hạn của người dùng.
 *   2. Phân tích chi tiết hạn chế: Giải mã danh sách mã tính năng bị cấm lưu trong án phạt.
 *   3. Ngăn chặn và cảnh báo: Ném `ForbiddenException` kèm thông điệp chi tiết để giao diện hiển thị cho người dùng.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận ID người dùng và mã tính năng cần kiểm tra (`requireFeature`).
 *   - Bước 2: Truy xuất danh sách án phạt đang hoạt động của người dùng từ `UserPenaltyRepository`.
 *   - Bước 3: Lọc các án phạt hạn chế tính năng còn trong thời hạn hiệu lực.
 *   - Bước 4: Nếu phát hiện mã tính năng nằm trong danh sách bị cấm, ném ngoại lệ `ForbiddenException`.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class PenaltyAccessServiceImpl implements PenaltyAccessService {
    private final UserPenaltyRepository repository;

    @Override
    @Transactional(readOnly = true)
    public void requireFeature(Long userId, String featureCode) {
        String normalized = featureCode.toUpperCase(Locale.ROOT);
        boolean restricted = repository.findByUser_UserIdAndStatus(userId, UserPenaltyStatus.ACTIVE).stream()
                .filter(item -> item.getPenaltyType() == UserPenaltyType.FEATURE_RESTRICTION)
                .filter(item -> item.getExpiresAt() == null || item.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(item -> item.getRestrictionDetails() == null ? "" : item.getRestrictionDetails().toUpperCase(Locale.ROOT))
                .anyMatch(details -> details.contains('"' + normalized + '"'));
        if (restricted) throw new ForbiddenException("Tính năng " + normalized + " đang bị hạn chế trên tài khoản này.");
    }
}
