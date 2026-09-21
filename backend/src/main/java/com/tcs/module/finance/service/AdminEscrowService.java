package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.response.AdminEscrowPageResponse;
import com.tcs.module.finance.dto.response.AdminEscrowResponse;
import com.tcs.module.finance.enums.EscrowStatus;
import java.time.LocalDate;

/**
 * ============================================================================
 * [UC-58] QUẢN TRỊ & ĐỐI SOÁT BẢO CHỨNG ESCROW (ADMIN ESCROW SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Dịch vụ dành cho Quản trị viên giám sát và đối soát toàn bộ dòng tiền ký quỹ Escrow trên nền tảng.
 *   - Đảm bảo an toàn tài chính bảo chứng giữa Phụ huynh, Gia sư và Trung tâm gia sư.
 * 
 * Chức năng chính:
 *   1. Tra cứu đa tiêu chí: Lọc giao dịch ký quỹ theo trạng thái, khoảng ngày, mã đối soát, người nạp và người nhận.
 *   2. Chi tiết bảo chứng: Xem thông tin hợp đồng giảng dạy, lớp học liên quan và trạng thái giải ngân.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên gửi tiêu chí lọc giao dịch ký quỹ bảo chứng (`search`).
 *   - Bước 2: Hệ thống truy vấn CSDL và trả về danh sách phân trang các giao dịch Escrow.
 *   - Bước 3: Xem chi tiết từng khoản ký quỹ phong tỏa (`get`).
 * ============================================================================
 */
public interface AdminEscrowService {
    AdminEscrowPageResponse search(EscrowStatus status, LocalDate from, LocalDate to, String reference,
            String payer, String beneficiary, int page, int size);
    AdminEscrowResponse get(Long escrowId);
}
