-- Demo Data Seed Script
-- Correct schema alignment & includes Admin Demo dataset

USE tutorconnectsystem;

-- 1. System Parameters
INSERT INTO system_parameters (parameter_key, parameter_value, description, data_type, is_active, created_at) VALUES 
('PLATFORM_FEE_RATE', '0.10', 'Phí nền tảng (10%)', 'DECIMAL', 1, NOW()),
('MAX_TUTOR_APPLICATIONS', '5', 'Số lượng ứng tuyển tối đa', 'INTEGER', 1, NOW()),
('ESCROW_HOLD_DAYS', '7', 'Số ngày tạm giữ tiền ký quỹ', 'INTEGER', 1, NOW()),
('AUTO_CLOSE_TICKET_DAYS', '3', 'Tự động đóng phiếu hỗ trợ sau 3 ngày không phản hồi', 'INTEGER', 1, NOW())
ON DUPLICATE KEY UPDATE parameter_value=VALUES(parameter_value);

-- 2. Users (Admin, Tutors, Clients, Centers)
INSERT INTO users (email, password_hash, status, created_at, updated_at) VALUES 
('thanhkiu0209@gmail.com', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('student@test.com', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
('tutor@test.com', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- Assign Platform Admin role to thanhkiu0209@gmail.com
INSERT INTO platform_admins (user_id, full_name)
SELECT user_id, 'Quản Trị Viên Hệ Thống' FROM users WHERE email = 'thanhkiu0209@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Quản Trị Viên Hệ Thống';

-- Assign Client role to student@test.com
INSERT INTO clients (user_id, full_name, phone)
SELECT user_id, 'Học Sinh Demo', '0912345678' FROM users WHERE email = 'student@test.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- Assign Tutor role to tutor@test.com
INSERT INTO tutors (user_id, full_name, bio, hourly_rate, rating, status)
SELECT user_id, 'Gia Sư Demo', 'Gia sư nhiều năm kinh nghiệm', 200000.00, 5.0, 'ACTIVE' FROM users WHERE email = 'tutor@test.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 3. FAQ Entries
INSERT INTO faq_entries (question, answer, category, sort_order, is_published, created_at) VALUES
('Làm sao để đăng ký làm gia sư?', 'Bạn cần tạo tài khoản, cập nhật hồ sơ và gửi yêu cầu xác thực.', 'GIA_SU', 1, 1, NOW()),
('Phí nền tảng là bao nhiêu?', 'Phí nền tảng hiện tại là 10% trên mỗi giao dịch thành công.', 'THANH_TOAN', 2, 1, NOW()),
('Làm sao để tìm gia sư?', 'Sử dụng công cụ tìm kiếm trên trang chủ để lọc gia sư theo môn học và khu vực.', 'PHU_HUYNH', 3, 1, NOW()),
('Tôi có thể hủy lớp không?', 'Có, bạn có thể hủy trước 24h mà không mất phí.', 'CHUNG', 4, 1, NOW()),
('Bao lâu thì nhận được tiền?', 'Tiền sẽ được cộng vào ví trong vòng 24h sau khi hoàn thành buổi học.', 'THANH_TOAN', 5, 1, NOW()),
('Làm sao để rút tiền?', 'Vào mục Ví điện tử, chọn Rút tiền và điền thông tin ngân hàng.', 'THANH_TOAN', 6, 1, NOW()),
('Gia sư có cần bằng cấp không?', 'Có, gia sư cần tải lên thẻ sinh viên hoặc bằng cấp liên quan để xác thực.', 'GIA_SU', 7, 1, NOW()),
('Tôi quên mật khẩu thì làm sao?', 'Nhấn vào nút Quên mật khẩu ở trang đăng nhập để khôi phục.', 'CHUNG', 8, 1, NOW()),
('Hệ thống thanh toán qua đâu?', 'Hệ thống hỗ trợ thanh toán qua chuyển khoản ngân hàng.', 'THANH_TOAN', 9, 1, NOW()),
('Lớp học diễn ra ở đâu?', 'Có thể học online qua Zoom/Google Meet hoặc học trực tiếp tại nhà.', 'CHUNG', 10, 1, NOW()),
('Làm sao để khiếu nại?', 'Vui lòng mở Support Ticket trong trang Hỗ trợ.', 'CHUNG', 11, 1, NOW()),
('Trung tâm gia sư có được đăng ký không?', 'Có, trung tâm gia sư có thể đăng ký tài khoản doanh nghiệp.', 'TRUNG_TAM', 12, 1, NOW())
ON DUPLICATE KEY UPDATE answer=VALUES(answer);

-- 4. Support Tickets
INSERT INTO support_tickets (user_id, subject, status, priority, category, description, created_at, updated_at) VALUES
((SELECT user_id FROM users WHERE email='student@test.com' LIMIT 1), 'Không thể thanh toán', 'OPEN', 'HIGH', 'PAYMENT_ISSUE', 'Tôi không thể thanh toán bằng thẻ visa', NOW(), NOW()),
((SELECT user_id FROM users WHERE email='tutor@test.com' LIMIT 1), 'Cập nhật CCCD', 'IN_PROGRESS', 'MEDIUM', 'ACCOUNT_ISSUE', 'Xin hãy duyệt CCCD mới của tôi', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at=NOW();

-- 5. Verification Requests
INSERT INTO verification_requests (user_id, verification_type, status, submitted_at)
SELECT user_id, 'TUTOR_PROFILE', 'PENDING', NOW() FROM users WHERE email = 'tutor@test.com'
ON DUPLICATE KEY UPDATE status=VALUES(status);
