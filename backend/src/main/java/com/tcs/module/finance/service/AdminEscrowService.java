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
    /**
     * [UC-58] Tìm kiếm và phân trang các giao dịch ký quỹ bảo chứng Escrow phục vụ Quản trị viên đối soát tài chính.
     * 
     * Luồng xử lý:
     * 1. Tiếp nhận các tiêu chí lọc: trạng thái ký quỹ (status), khoảng ngày gửi tiền (from, to), mã tham chiếu (reference), email/tên người nạp (payer), người thụ hưởng (beneficiary).
     * 2. Thực thi truy vấn phân trang trên EscrowTransactionRepository.
     * 3. Ánh xạ danh sách giao dịch kèm thông tin lớp học, hợp đồng và đối tác giao dịch.
     * 
     * @param status Trạng thái bảo chứng (HELD, RELEASED, REFUNDED, DISPUTED)
     * @param from Ngày bắt đầu phát sinh giao dịch
     * @param to Ngày kết thúc phát sinh giao dịch
     * @param reference Mã tham chiếu giao dịch thanh toán
     * @param payer Email hoặc tên người thanh toán (Phụ huynh)
     * @param beneficiary Email hoặc tên người thụ hưởng (Gia sư / Trung tâm)
     * @param page Số trang truy vấn (bắt đầu từ 0)
     * @param size Kích thước trang
     * @return AdminEscrowPageResponse kết quả phân trang giao dịch ký quỹ bảo chứng
     */
    AdminEscrowPageResponse search(EscrowStatus status, LocalDate from, LocalDate to, String reference,
            String payer, String beneficiary, int page, int size);

    /**
     * [UC-58] Tra cứu chi tiết một giao dịch ký quỹ Escrow theo mã định danh.
     * 
     * @param escrowId Định danh khoản ký quỹ Escrow
     * @return AdminEscrowResponse thông tin chi tiết đầy đủ của giao dịch ký quỹ
     * @throws com.tcs.exception.ResourceNotFoundException nếu không tìm thấy giao dịch ký quỹ
     */
    AdminEscrowResponse get(Long escrowId);
}
