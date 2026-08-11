USE tutorconnectsystem;
SET NAMES 'utf8mb4';

DELETE FROM faq_entries;

INSERT INTO faq_entries (question, answer, category, sort_order, is_published, created_at, updated_at) VALUES 
('Làm thế nào để đăng ký làm gia sư?', 'Bạn có thể nhấn vào nút Đăng ký làm gia sư ở trang chủ, điền thông tin hồ sơ và chờ quản trị viên duyệt.', 'Gia sư', 1, 1, NOW(), NOW()),
('Tôi có thể thay đổi thông tin hồ sơ sau khi đăng ký không?', 'Có, bạn có thể truy cập trang Hồ sơ cá nhân để cập nhật thông tin bất kỳ lúc nào.', 'Chung', 2, 1, NOW(), NOW()),
('Hệ thống thanh toán bằng hình thức nào?', 'Hệ thống hỗ trợ thanh toán qua chuyển khoản ngân hàng và các ví điện tử phổ biến.', 'Thanh toán', 3, 1, NOW(), NOW()),
('Làm sao để tìm được gia sư phù hợp?', 'Bạn có thể sử dụng công cụ Tìm kiếm và Lọc trên hệ thống để tìm gia sư theo môn học, địa điểm, hoặc giá tiền.', 'Phụ huynh/Học viên', 4, 1, NOW(), NOW()),
('Khi nào tôi nhận được tiền lương gia sư?', 'Tiền lương sẽ được chuyển vào ví của bạn sau khi khóa học hoặc các buổi dạy hoàn tất và được phụ huynh xác nhận.', 'Thanh toán', 5, 1, NOW(), NOW()),
('Phí dịch vụ của hệ thống là bao nhiêu?', 'Hệ thống hiện tại miễn phí hoàn toàn đối với phụ huynh/học viên. Đối với gia sư, phí sẽ tùy thuộc vào chính sách từng hợp đồng.', 'Chung', 6, 1, NOW(), NOW()),
('Tôi có thể hủy lớp học không?', 'Bạn có thể gửi yêu cầu hủy lớp trong mục Quản lý lớp học. Tuy nhiên, việc hủy có thể đi kèm một số chế tài theo hợp đồng đã ký.', 'Chung', 7, 1, NOW(), NOW()),
('Quy trình ký hợp đồng như thế nào?', 'Sau khi gia sư được chọn, hệ thống sẽ tự động tạo hợp đồng điện tử. Cả hai bên chỉ cần xác nhận qua OTP email để hoàn tất.', 'Gia sư', 8, 1, NOW(), NOW()),
('Tôi quên mật khẩu, làm thế nào để khôi phục?', 'Vui lòng nhấn vào "Quên mật khẩu" ở trang Đăng nhập và làm theo hướng dẫn gửi về email của bạn.', 'Chung', 9, 1, NOW(), NOW()),
('Làm thế nào để liên hệ với bộ phận hỗ trợ?', 'Bạn có thể nhấn vào biểu tượng Trợ lý AI ở góc dưới bên phải hoặc vào phần "Liên hệ chúng tôi" để tạo ticket.', 'Chung', 10, 1, NOW(), NOW());
