package com.tcs.module.finance.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.dto.response.AdminEscrowPageResponse;
import com.tcs.module.finance.dto.response.AdminEscrowResponse;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.service.AdminEscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.TutoringClass;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * [UC-58] HIỆN THỰC QUẢN TRỊ BẢO CHỨNG ESCROW (ADMIN ESCROW SERVICE IMPL)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Đồng tác giả: tienanh6677 (Nguyễn Tiến Anh)
 * Ngày tạo: 2026-08-11
 * 
 * Mô tả Use Case:
 *   - Hiện thực hóa dịch vụ quản trị và tra cứu chi tiết các khoản tiền ký quỹ Escrow trên toàn hệ thống.
 *   - Hỗ trợ Quản trị viên đối soát tài sản bảo chứng và giám sát tiến độ giải ngân học phí.
 * 
 * Chức năng chính:
 *   1. Tìm kiếm giao dịch ký quỹ: Hỗ trợ tìm kiếm theo mã đối soát, người nạp tiền, người thụ hưởng và trạng thái.
 *   2. Tra cứu chi tiết bảo chứng: Trả về thông tin lớp học, hợp đồng và tiến trình ký quỹ an toàn.
 *   3. Phân trang dữ liệu: Đảm bảo hiệu năng tải trang đối soát tài chính khi khối lượng dữ liệu lớn.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận yêu cầu tìm kiếm với các tham số lọc mốc ngày và từ khóa (`search`).
 *   - Bước 2: Gọi phương thức `repository.searchAdmin` với câu lệnh truy vấn tối ưu.
 *   - Bước 3: Chuyển đổi thực thể sang DTO `AdminEscrowResponse` chứa đầy đủ thông tin đối soát.
 *   - Bước 4: Đóng gói và trả về kết quả phân trang `AdminEscrowPageResponse`.
 * ============================================================================
 */
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AdminEscrowServiceImpl implements AdminEscrowService {
    private final EscrowTransactionRepository repository;

    public AdminEscrowPageResponse search(EscrowStatus status, LocalDate from, LocalDate to, String reference,
            String payer, String beneficiary, int page, int size) {
        Page<EscrowTransaction> result = repository.searchAdmin(status,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay(),
                blankToNull(reference), blankToNull(payer), blankToNull(beneficiary),
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        return AdminEscrowPageResponse.builder().content(result.map(this::map).getContent()).page(result.getNumber())
                .size(result.getSize()).totalElements(result.getTotalElements()).totalPages(result.getTotalPages()).build();
    }

    public AdminEscrowResponse get(Long escrowId) {
        return map(repository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy escrow: " + escrowId)));
    }

    private AdminEscrowResponse map(EscrowTransaction escrow) {
        User payer = escrow.getPayment().getWallet().getUser();
        User beneficiary = escrow.getAssignment() != null ? escrow.getAssignment().getTutor().getUser()
                : escrow.getClassStudent() != null && escrow.getClassStudent().getTutoringClass().getCenter() != null
                    ? escrow.getClassStudent().getTutoringClass().getCenter().getUser() : null;
        TutoringClass tutoringClass = escrow.getAssignment() != null
                && escrow.getAssignment().getApplication() != null
                ? escrow.getAssignment().getApplication().getTutoringClass()
                : escrow.getClassStudent() == null ? null : escrow.getClassStudent().getTutoringClass();
        boolean privateClassEscrow = escrow.getAssignment() != null;
        boolean centerClassEscrow = escrow.getClassStudent() != null;
        String transactionType = privateClassEscrow
                ? "PRIVATE_CLASS_ESCROW"
                : centerClassEscrow ? "CENTER_CLASS_ESCROW" : "UNKNOWN_ESCROW";
        String transactionTypeLabel = privateClassEscrow
                ? "Thanh toán lớp private"
                : centerClassEscrow ? "Thanh toán lớp trung tâm" : "Chưa xác định loại giao dịch";
        return AdminEscrowResponse.builder().escrowId(escrow.getEscrowId())
                .paymentId(escrow.getPayment().getTransactionId()).referenceCode(escrow.getPayment().getReferenceCode())
                .amount(escrow.getAmount()).status(escrow.getStatus()).payerUserId(payer.getUserId()).payerEmail(payer.getEmail())
                .beneficiaryUserId(beneficiary == null ? null : beneficiary.getUserId())
                .beneficiaryEmail(beneficiary == null ? null : beneficiary.getEmail())
                .transactionType(transactionType)
                .transactionTypeLabel(transactionTypeLabel)
                .classId(tutoringClass == null ? null : tutoringClass.getClassId())
                .classTitle(tutoringClass == null ? null : tutoringClass.getTitle())
                .assignmentId(escrow.getAssignment() == null ? null : escrow.getAssignment().getAssignmentId())
                .classStudentId(escrow.getClassStudent() == null ? null : escrow.getClassStudent().getClassStudentId())
                .depositedAt(escrow.getDepositedAt()).releasedAt(escrow.getReleasedAt())
                .createdAt(escrow.getCreatedAt()).updatedAt(escrow.getUpdatedAt()).build();
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
