package com.tcs.module.ai.enums;

/**
 * 15 business domains covering the entire Tutor Connect System (TCS) plus Open Domain.
 */
public enum AiDomain {
    CONVERSATION_SAFETY,   // Chào hỏi, cảm ơn, chửi tục, gõ rác, yêu cầu gặp người
    IDENTITY_AUTH,         // Đăng nhập, đăng ký, OTP, quên mật khẩu, phân quyền
    PROFILE_GUARDIAN,      // Hồ sơ cá nhân, CCCD, phụ huynh, hồ sơ con, kinh nghiệm
    VERIFICATION,          // Xác minh hồ sơ gia sư, trung tâm, duyệt tài liệu
    MARKETPLACE,           // Tìm gia sư, tìm lớp, tạo lớp, ứng tuyển, chọn gia sư
    TUTOR_OPS,             // Lịch dạy, điểm danh, xin dời lịch, dạy thay, nhận lớp
    CENTER_OPS,            // Quản lý gia sư trung tâm, tuyển dụng, hợp đồng mẫu
    FINANCE_WALLET,        // Ví tiền, nạp tiền, rút tiền, Escrow, hoàn tiền, phí sàn
    CONTRACT_REVIEW,       // Hợp đồng, ký OTP, đánh giá, uy tín gia sư
    MESSAGING_TICKET,      // Nhắn tin, tạo ticket, phản hồi ticket, SLA, thông báo
    TRUST_SAFETY,          // Báo cáo lách sàn, mở tranh chấp, khiếu nại, chế tài phạt
    CATALOG_FAQ,           // FAQ, chính sách nền tảng, môn học, khối lớp, hỗ trợ chung
    PLATFORM_ADMIN,        // Dashboard admin, báo cáo doanh thu, duyệt queue, audit log
    AI_TUTORING,           // Trợ giảng giải bài, học tập, giải thích khái niệm
    OPEN_DOMAIN,           // Tri thức phổ thông, toán học, ngày giờ, thời tiết, giải trí
    OUT_OF_SCOPE           // Ngoài phạm vi hệ thống
}
