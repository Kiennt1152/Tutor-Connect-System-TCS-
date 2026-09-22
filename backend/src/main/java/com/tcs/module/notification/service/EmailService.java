package com.tcs.module.notification.service;

/**
 * ============================================================================
 * [UC-35] DỊCH VỤ GỬI THƯ ĐIỆN TỬ HỆ THỐNG (EMAIL SERVICE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-25
 * 
 * Mô tả Use Case:
 *   - Cung cấp giao diện dịch vụ gửi thư điện tử cho toàn bộ nền tảng Tutor Connect System.
 *   - Chịu trách nhiệm gửi mã xác thực ký hợp đồng (OTP), thông báo tài chính và thư chào mừng.
 * 
 * Chức năng chính:
 *   1. Gửi email thông báo: Gửi thư định dạng HTML với tiêu đề và nội dung tùy biến tới người dùng.
 *   2. Gửi mã OTP xác thực hợp đồng: Chuyển phát nhanh mã OTP bảo mật dùng một lần xác nhận ký hợp đồng điện tử.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận yêu cầu gửi email cùng tham số người nhận, tiêu đề và nội dung HTML.
 *   - Bước 2: Định dạng cấu trúc thư và kích hoạt luồng gửi thư ngầm qua hạ tầng email.
 *   - Bước 3: Ghi nhận trạng thái gửi thành công hoặc cảnh báo thất bại vào log hệ thống.
 * ============================================================================
 */
public interface EmailService {

    void sendEmail(String to, String subject, String htmlContent);

    void sendContractOtp(String to, String otpCode, String contractNo);
}
