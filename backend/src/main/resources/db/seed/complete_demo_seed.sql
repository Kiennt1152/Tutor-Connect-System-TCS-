-- ====================================================================
-- TCS COMPLETE PRODUCTION DEMO SEED SCRIPT
-- Schema Compatibility: V1 -> V27
-- Character Encoding: UTF-8 (utf8mb4)
-- ====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
USE tutorconnectsystem;

-- --------------------------------------------------------------------
-- 1. USERS
-- Password 'ducminh1011': $2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS
-- Password '12345678':   $2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW
-- --------------------------------------------------------------------
INSERT INTO users (email, phone, password_hash, status, created_at, updated_at) VALUES
('thanhkiu0209@gmail.com', '0909999999', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),
('admin@tcs.vn', '0908888888', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),
('haehuynh35@gmail.com', '0912345678', '$2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
('minhduc101dz@gmail.com', '0987654321', '$2a$10$NLRYt9H47Df/WP37AYWqPuLTBSEBqOauPCkVjOVF/fpVkucRweifS', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 45 DAY), NOW()),
('parent.nguyen@gmail.com', '0901234567', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 40 DAY), NOW()),
('parent.tran@gmail.com', '0902345678', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 35 DAY), NOW()),
('parent.tuan@tcs.vn', '0903334444', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
('tutor.le@gmail.com', '0903456789', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 50 DAY), NOW()),
('tutor.pham@gmail.com', '0904567890', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 45 DAY), NOW()),


-- --------------------------------------------------------------------
-- 2. PLATFORM ADMINS
-- --------------------------------------------------------------------
INSERT INTO platform_admins (user_id, full_name)
SELECT user_id, 'Quản Trị Viên Hệ Thống' FROM users WHERE email = 'thanhkiu0209@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Quản Trị Viên Hệ Thống';

INSERT INTO platform_admins (user_id, full_name)
SELECT user_id, 'Admin TCS' FROM users WHERE email = 'admin@tcs.vn'
ON DUPLICATE KEY UPDATE full_name = 'Admin TCS';

-- --------------------------------------------------------------------
-- 3. CLIENTS
-- --------------------------------------------------------------------
INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
SELECT user_id, 'Huỳnh Đức Minh (Phụ Huynh)', '0912345678', 'Quận Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW()
FROM users WHERE email = 'haehuynh35@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
SELECT user_id, 'Nguyễn Văn Hùng', '0901234567', 'Quận Ba Đình, Hà Nội', 'MALE', NOW(), NOW()
FROM users WHERE email = 'parent.nguyen@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
SELECT user_id, 'Trần Thị Mai', '0902345678', 'Quận Đống Đa, Hà Nội', 'FEMALE', NOW(), NOW()
FROM users WHERE email = 'parent.tran@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO clients (user_id, full_name, phone, address, gender, created_at, updated_at)
SELECT user_id, 'Phạm Anh Tuấn', '0903334444', 'Quận Cầu Giấy, Hà Nội', 'MALE', NOW(), NOW()
FROM users WHERE email = 'parent.tuan@tcs.vn'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- --------------------------------------------------------------------
-- 4. TUTORS (Verified)
-- --------------------------------------------------------------------
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT user_id, 'Minh Đức (Gia Sư Toán & Tin)', 'MALE', '0987654321', 'Quận Cầu Giấy, Hà Nội', 5, 'Gia sư chuyên Toán 12 và Luyện thi Đại học khối A, A1 khu vực Cầu Giấy.', 240000.00, 5.00, 'VERIFIED', NOW(), NOW()
FROM users WHERE email = 'minhduc101dz@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';

INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT user_id, 'Lê Hoàng Nam', 'MALE', '0903456789', 'Quận Cầu Giấy, Hà Nội', 5, 'Chuyên dạy Toán lớp 12 luyện thi đại học khu vực Cầu Giấy', 250000.00, 4.90, 'VERIFIED', NOW(), NOW()
FROM users WHERE email = 'tutor.le@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';

INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT user_id, 'Phạm Thu Thảo', 'FEMALE', '0904567890', 'Quận 1, TP.HCM', 4, 'Gia sư Tiếng Anh IELTS 8.0 chuyên kèm học sinh cấp 3', 300000.00, 5.00, 'VERIFIED', NOW(), NOW()
FROM users WHERE email = 'tutor.pham@gmail.com'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';

INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT user_id, 'Nguyễn Văn Toán', 'MALE', '0912111222', 'Quận Cầu Giấy, Hà Nội', 5, 'Gia sư Toán luyện thi học sinh giỏi cấp Quốc gia', 220000.00, 4.90, 'VERIFIED', NOW(), NOW()
FROM users WHERE email = 'tutor.math@tcs.vn'
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), verification_status = 'VERIFIED';



-- --------------------------------------------------------------------
-- 5. TUTOR CENTERS
-- --------------------------------------------------------------------
INSERT INTO tutor_centers (user_id, company_name, license_no, phone, address, description)
SELECT user_id, 'Trung Tâm Gia Sư Trí Việt', 'LICENSE-2026-TV', '02838999999', '123 Đường Cầu Giấy, Hà Nội', 'Trung tâm kết nối gia sư uy tín chất lượng cao hàng đầu Hà Nội'
FROM users WHERE email = 'center.triangviet@gmail.com'
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

-- --------------------------------------------------------------------
-- 6. WALLETS
-- --------------------------------------------------------------------
INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
SELECT user_id, 10000000.00, 0.00, 'ACTIVE', NOW(), NOW() 
FROM users WHERE email IN ('haehuynh35@gmail.com', 'parent.nguyen@gmail.com', 'parent.tran@gmail.com', 'parent.tuan@tcs.vn')
ON DUPLICATE KEY UPDATE available_balance = 10000000.00, status = 'ACTIVE';

INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at, updated_at)
SELECT user_id, 5000000.00, 0.00, 'ACTIVE', NOW(), NOW() 
FROM users WHERE email IN ('minhduc101dz@gmail.com', 'tutor.le@gmail.com', 'tutor.pham@gmail.com', 'tutor.math@tcs.vn', 'tutor.physics@tcs.vn')
ON DUPLICATE KEY UPDATE available_balance = 5000000.00, status = 'ACTIVE';

-- --------------------------------------------------------------------
-- 7. SYSTEM PARAMETERS & FAQS
-- --------------------------------------------------------------------
INSERT INTO system_parameters (param_key, param_value, description) VALUES 
('PLATFORM_FEE_RATE', '0.02', 'Phí nền tảng (2%)'),
('MAX_TUTOR_APPLICATIONS', '5', 'Số lượng ứng tuyển tối đa per class'),
('ESCROW_HOLD_DAYS', '7', 'Số ngày tạm giữ tiền ký quỹ'),
('AUTO_CLOSE_TICKET_DAYS', '3', 'Tự động đóng phiếu hỗ trợ sau 3 ngày không phản hồi')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value);

INSERT IGNORE INTO faq_entries (faq_id, question, answer, category, sort_order, is_published) VALUES
-- =========================================================================
-- 1. AUTH_PROFILE (1 to 20)
-- =========================================================================
(1, 'Làm sao để đăng ký tài khoản trên TCS?', 'Bạn bấm vào nút "Đăng ký" tại góc trên bên phải màn hình hoặc bấm nút "Đăng ký", chọn vai trò phù hợp (Học viên/Phụ huynh, Gia sư, hoặc Trung tâm gia sư), điền số điện thoại, email và mật khẩu để tạo tài khoản.', 'AUTH_PROFILE', 1, 1),
(2, 'Làm sao đăng nhập tài khoản TCS?', 'Truy cập màn hình Đăng nhập, nhập email hoặc số điện thoại kèm mật khẩu đã đăng ký. Bạn cũng có thể đăng nhập nhanh qua Google hoặc yêu cầu mã đăng nhập OTP.', 'AUTH_PROFILE', 2, 1),
(3, 'Tôi quên mật khẩu thì lấy lại thế nào?', 'Tại màn hình đăng nhập, bạn nhấn vào liên kết "Quên mật khẩu?" hoặc chọn tính năng Quên mật khẩu, nhập email đăng ký để nhận liên kết và mã xác thực đặt lại mật khẩu mới.', 'AUTH_PROFILE', 3, 1),
(4, 'Không nhận được mã OTP xác thực phải làm sao?', 'Vui lòng kiểm tra lại số điện thoại hoặc hộp thư rác (Spam) trong email. Nếu sau 60 giây vẫn chưa nhận được, bạn nhấn nút "Gửi lại mã OTP" trên giao diện.', 'AUTH_PROFILE', 4, 1),
(5, 'Tài khoản bị khóa vì lý do gì và mở lại ra sao?', 'Tài khoản có thể bị khóa tạm thời nếu nhập sai mật khẩu quá 5 lần liên tiếp hoặc có dấu hiệu vi phạm chính sách sàn. Bạn vui lòng tạo phiếu hỗ trợ tại mục Hỗ trợ & Khiếu nại để được mở khóa.', 'AUTH_PROFILE', 5, 1),
(6, 'Làm sao để đổi mật khẩu tài khoản?', 'Sau khi đăng nhập, vào mục Cài đặt tài khoản trong Hồ sơ cá nhân trong mục Hồ sơ cá nhân, chọn "Đổi mật khẩu", nhập mật khẩu hiện tại và mật khẩu mới để cập nhật.', 'AUTH_PROFILE', 6, 1),
(7, 'Cập nhật thông tin hồ sơ cá nhân ở đâu?', 'Bạn truy cập mục Hồ sơ cá nhân trong mục Hồ sơ cá nhân để chỉnh sửa họ tên, số điện thoại, địa chỉ, ảnh đại diện và thông tin liên hệ.', 'AUTH_PROFILE', 7, 1),
(8, 'Cách tải ảnh đại diện avatar chất lượng cao?', 'Trong trang Hồ sơ trong mục Hồ sơ cá nhân, nhấn vào biểu tượng máy ảnh trên khung ảnh đại diện, chọn tệp ảnh có định dạng PNG hoặc JPG dung lượng dưới 5MB để tải lên.', 'AUTH_PROFILE', 8, 1),
(9, 'Quét căn cước công dân CCCD tự động như thế nào?', 'Hệ thống TCS hỗ trợ công nghệ OCR tự động nhận diện thông tin từ ảnh chụp 2 mặt CCCD trong mục xác minh hồ sơ trong mục Hồ sơ cá nhân giúp bạn không cần nhập liệu thủ công.', 'AUTH_PROFILE', 9, 1),
(10, 'Tạo hồ sơ con học viên (Child Profile) để làm gì?', 'Phụ huynh có thể tạo nhiều hồ sơ con dưới một tài khoản quản lý trong mục Hồ sơ cá nhân để theo dõi lộ trình học tập, lịch học và điểm danh riêng biệt cho từng người con.', 'AUTH_PROFILE', 10, 1),
(11, 'Làm sao liên kết tài khoản Phụ huynh với Học viên?', 'Trong hồ sơ học viên, chọn tính năng "Liên kết Người giám hộ", nhập email hoặc số điện thoại của phụ huynh. Phụ huynh chỉ cần xác nhận qua mã liên kết là hoàn tất.', 'AUTH_PROFILE', 11, 1),
(12, 'Gia sư cập nhật kinh nghiệm giảng dạy ở đâu?', 'Gia sư vào mục Hồ sơ cá nhân, chọn tab "Kinh nghiệm & Chuyên môn" để thêm quá trình công tác, các trường đại học đã tốt nghiệp và giải thưởng thành tích.', 'AUTH_PROFILE', 12, 1),
(13, 'Cập nhật lịch rảnh của gia sư thế nào?', 'Gia sư vào mục Hồ sơ cá nhân hoặc mục Lịch dạy, tích chọn các khung giờ rảnh trong tuần để phụ huynh dễ dàng đối chiếu và đặt lịch học phù hợp.', 'AUTH_PROFILE', 13, 1),
(14, 'Viết phần giới thiệu Bio gia sư thế nào để thu hút?', 'Nêu rõ thế mạnh môn học, phương pháp sư phạm, thành tích đào tạo học sinh tiến bộ và thái độ tận tâm để tạo độ tin cậy cao với phụ huynh.', 'AUTH_PROFILE', 14, 1),
(15, 'Một tài khoản có thể vừa làm gia sư vừa làm phụ huynh không?', 'TCS khuyến nghị tạo tài khoản chuyên biệt theo từng vai trò chính để bảo đảm quyền lợi tài chính, tính hợp lệ của hợp đồng và giao diện tối ưu.', 'AUTH_PROFILE', 15, 1),
(16, 'Làm sao để xóa hoặc đóng tài khoản TCS?', 'Nếu không còn nhu cầu sử dụng, bạn có thể gửi yêu cầu vô hiệu hóa tài khoản trong mục Cài đặt quyền riêng tư hoặc liên hệ qua mục Hỗ trợ & Khiếu nại.', 'AUTH_PROFILE', 16, 1),
(17, 'Thông tin cá nhân trên TCS có được bảo mật không?', 'Tất cả dữ liệu cá nhân, số điện thoại và chứng từ CCCD được mã hóa an toàn theo tiêu chuẩn bảo mật và chỉ dùng cho mục đích xác minh danh tính.', 'AUTH_PROFILE', 17, 1),
(18, 'Phiên đăng nhập hết hạn thì phải làm sao?', 'Hệ thống tự động đăng xuất sau một khoảng thời gian không hoạt động để bảo vệ tài khoản. Bạn chỉ cần đăng nhập lại trên màn hình Đăng nhập để tiếp tục làm việc.', 'AUTH_PROFILE', 18, 1),
(19, 'Lỗi không có quyền truy cập trang này (Permission Denied)?', 'Lỗi này xuất hiện khi bạn cố truy cập trang dành cho vai trò khác (ví dụ: Học viên truy cập trang quản trị Admin). Hãy đăng nhập bằng tài khoản có vai trò tương ứng.', 'AUTH_PROFILE', 19, 1),
(20, 'Làm sao thay đổi số điện thoại nhận thông báo?', 'Bạn vào mục Hồ sơ cá nhân, chọn Chỉnh sửa thông tin liên hệ, nhập số điện thoại mới và xác thực bằng mã OTP được gửi về số đó.', 'AUTH_PROFILE', 20, 1),

-- =========================================================================
-- 2. VERIFICATION (21 to 35)
-- =========================================================================
(21, 'Quy trình xác minh hồ sơ gia sư diễn ra thế nào?', 'Gia sư tải lên ảnh chụp 2 mặt CCCD, bằng cử nhân/thẻ sinh viên và chứng chỉ liên quan trong mục Hồ sơ cá nhân. Quản trị viên TCS sẽ đối soát và phê duyệt trong vòng 24–48 giờ.', 'VERIFICATION', 21, 1),
(22, 'Cần những giấy tờ gì để được duyệt hồ sơ gia sư?', 'Bao gồm: (1) Căn cước công dân hoặc Hộ chiếu còn hạn; (2) Thẻ sinh viên hoặc Bằng tốt nghiệp Đại học; (3) Chứng chỉ ngoại ngữ/chuyên môn (nếu có).', 'VERIFICATION', 22, 1),
(23, 'Vì sao hồ sơ xác minh gia sư bị từ chối?', 'Các lý do phổ biến: Ảnh giấy tờ bị mờ, lóa sáng, mất góc; thông tin họ tên/ngày sinh trên hồ sơ không khớp với CCCD; hoặc văn bằng không đủ tính pháp lý.', 'VERIFICATION', 23, 1),
(24, 'Thời gian xét duyệt hồ sơ xác minh là bao lâu?', 'Hệ thống xét duyệt trong vòng 24 giờ làm việc. Vào các đợt cao điểm đầu năm học, thời gian tối đa không quá 48 giờ.', 'VERIFICATION', 24, 1),
(25, 'Kiểm tra trạng thái duyệt hồ sơ ở đâu?', 'Bạn vào mục Hồ sơ cá nhân để xem huy hiệu trạng thái: "Chờ duyệt (PENDING)", "Đã xác minh (APPROVED)", hoặc "Từ chối (REJECTED)" kèm lý do chi tiết.', 'VERIFICATION', 25, 1),
(26, 'Xác minh hồ sơ trung tâm gia sư cần những gì?', 'Trung tâm gia sư cần cung cấp: Giấy phép đăng ký kinh doanh, Căn cước công dân của người đại diện pháp luật, địa chỉ trụ sở và hợp đồng mẫu.', 'VERIFICATION', 26, 1),
(27, 'Làm sao để nộp lại giấy tờ khi bị từ chối xác minh?', 'Tại mục Hồ sơ cá nhân, bấm vào nút "Cập nhật giấy tờ", chụp lại ảnh rõ nét theo đúng hướng dẫn và nhấn "Gửi duyệt lại".', 'VERIFICATION', 27, 1),
(28, 'Huy hiệu Đã xác minh (Verified Badge) mang lại lợi ích gì?', 'Gia sư có huy hiệu xác minh sẽ được ưu tiên hiển thị đầu trang tìm kiếm, tăng 80% tỷ lệ được phụ huynh chọn và đủ điều kiện nhận lớp có học phí cao.', 'VERIFICATION', 28, 1),
(29, 'Có cần công chứng bằng cấp khi tải lên không?', 'TCS chỉ yêu cầu ảnh chụp bản gốc hoặc bản sao công chứng rõ nét, thể hiện đầy đủ số hiệu văn bằng và con dấu để đội ngũ quản trị viên kiểm tra.', 'VERIFICATION', 29, 1),
(30, 'Xác minh chứng chỉ ngoại ngữ IELTS, TOEIC như thế nào?', 'Bạn tải lên bảng điểm hoặc chứng chỉ còn hiệu lực, hệ thống sẽ đối soát mã tra cứu chứng chỉ với cơ quan cấp phép quốc tế.', 'VERIFICATION', 30, 1),
(31, 'Học sinh chưa tốt nghiệp đại học có được làm gia sư không?', 'Có, sinh viên các trường đại học, cao đẳng chỉ cần tải Thẻ sinh viên còn hiệu lực và bảng điểm học tập để được duyệt làm gia sư sinh viên.', 'VERIFICATION', 31, 1),
(32, 'Duyệt hồ sơ gia sư có mất phí không?', 'Quy trình kiểm duyệt hồ sơ và cấp huy hiệu xác minh trên TCS là hoàn toàn miễn phí cho tất cả gia sư và trung tâm.', 'VERIFICATION', 32, 1),
(33, 'Thông tin CCCD sau khi duyệt có được lưu vĩnh viễn không?', 'Dữ liệu được lưu trữ an toàn trong kho lưu trữ mã hóa riêng biệt nhằm bảo vệ quyền lợi pháp lý cho các bên khi phát sinh ký kết hợp đồng.', 'VERIFICATION', 33, 1),
(34, 'Ai là người trực tiếp duyệt hồ sơ gia sư?', 'Đội ngũ Chuyên viên Pháp chế và Vận hành của TCS kiểm tra thủ công từng hồ sơ kết hợp công nghệ kiểm tra tự động OCR.', 'VERIFICATION', 34, 1),
(35, 'Gia sư chưa xác minh có được nộp hồ sơ nhận lớp không?', 'Gia sư chưa xác minh vẫn có thể xem thông tin lớp học nhưng chỉ được nộp hồ sơ ứng tuyển chính thức sau khi hồ sơ đạt trạng thái APPROVED.', 'VERIFICATION', 35, 1),

-- =========================================================================
-- 3. MARKETPLACE (36 to 65)
-- =========================================================================
(36, 'Làm sao để tìm gia sư phù hợp trên TCS?', 'Truy cập mục Tìm gia sư, sử dụng bộ lọc môn học, khối lớp, khu vực (quận/huyện), hình thức dạy (Online/Tại nhà) và mức học phí để chọn gia sư ưng ý.', 'MARKETPLACE', 36, 1),
(37, 'Làm sao để đăng bài tìm gia sư (Tạo lớp học)?', 'Học viên hoặc phụ huynh vào mục Tạo lớp học, điền đầy đủ môn học, lớp, địa chỉ học, thời gian rảnh và mức thù lao dự kiến rồi bấm "Đăng yêu cầu".', 'MARKETPLACE', 37, 1),
(38, 'Tìm lớp học đang tuyển gia sư ở đâu?', 'Gia sư truy cập mục Lớp học để xem danh sách toàn bộ các lớp đang ở trạng thái OPEN, xem yêu cầu học viên và nộp đơn ứng tuyển.', 'MARKETPLACE', 38, 1),
(39, 'Gia sư ứng tuyển lớp học như thế nào?', 'Tại trang chi tiết lớp học chi tiết lớp học, gia sư bấm nút "Ứng tuyển", nhập mức học phí đề xuất kèm lời giới thiệu bản thân rồi gửi phụ huynh xem xét.', 'MARKETPLACE', 39, 1),
(40, 'Phụ huynh chọn gia sư ứng tuyển ra sao?', 'Phụ huynh vào mục quản lý lớp học, bấm xem danh sách ứng viên, so sánh hồ sơ bằng cấp, đánh giá sao và bấm "Chấp nhận" gia sư phù hợp nhất.', 'MARKETPLACE', 40, 1),
(41, 'Trạng thái lớp học OPEN, ASSIGNED, COMPLETED, CANCELLED nghĩa là gì?', 'OPEN: Đang nhận ứng tuyển; ASSIGNED: Đã chọn gia sư và ký hợp đồng; COMPLETED: Đã học xong và tất toán; CANCELLED: Đã hủy lớp do phụ huynh yêu cầu.', 'MARKETPLACE', 41, 1),
(42, 'Học phí gia sư trên TCS dao động khoảng bao nhiêu?', 'Học phí thông thường từ 150.000 ₫ – 250.000 ₫/buổi đối với sinh viên và từ 300.000 ₫ – 500.000 ₫/buổi đối với giáo viên luyện thi chuyên sâu.', 'MARKETPLACE', 42, 1),
(43, 'Có thể thương lượng mức học phí với gia sư không?', 'Có, khi gia sư nộp đơn ứng tuyển có thể đề xuất mức giá khác với bài đăng gốc. Phụ huynh có thể trao đổi qua tin nhắn trước khi ký hợp đồng.', 'MARKETPLACE', 43, 1),
(44, 'Một lớp học có thể có bao nhiêu gia sư ứng tuyển?', 'Không giới hạn số lượng gia sư ứng tuyển cho đến khi phụ huynh lựa chọn được ứng viên ưng ý và tiến hành ký kết hợp đồng.', 'MARKETPLACE', 44, 1),
(45, 'Học phí dạy Online có rẻ hơn học tại nhà không?', 'Hình thức học Online thường tiết kiệm chi phí đi lại nên học phí có thể thấp hơn từ 15% – 30% so với hình thức gia sư đến tận nhà.', 'MARKETPLACE', 45, 1),
(46, 'Tìm gia sư luyện thi vào lớp 10 ở đâu?', 'Tại trang Tìm gia sư, chọn khối lớp "Lớp 9", môn Toán/Văn/Anh và chọn mục tiêu "Luyện thi vào 10" trong bộ lọc chuyên môn.', 'MARKETPLACE', 46, 1),
(47, 'Tìm gia sư luyện thi Đại học khối A, B, C, D1 thế nào?', 'Tại mục Tìm gia sư, chọn khối lớp "Lớp 12" kèm các môn tổ hợp như Toán - Lý - Hóa, Toán - Hóa - Sinh, hoặc Toán - Văn - Anh để tìm gia sư chuyên ban.', 'MARKETPLACE', 47, 1),
(48, 'Tìm gia sư dạy Tiếng Anh giao tiếp cho người đi làm?', 'Chọn môn "Tiếng Anh", cấp độ "Người đi làm / Giao tiếp" tại mục Tìm gia sư để lọc các gia sư có chứng chỉ IELTS và kinh nghiệm giảng dạy công sở.', 'MARKETPLACE', 48, 1),
(49, 'Tìm gia sư dạy kèm Tin học lập trình và Toán tư duy?', 'Hệ thống hỗ trợ danh mục môn học mở rộng bao gồm Lập trình Scratch, Python, C++, Tin học văn phòng và Toán tư duy Singapore.', 'MARKETPLACE', 49, 1),
(50, 'Sau khi đăng lớp bao lâu thì có gia sư ứng tuyển?', 'Thông thường chỉ sau 15–60 phút đăng lớp tại mục Tạo lớp học, hệ thống sẽ tự động thông báo đến các gia sư phù hợp trong khu vực để nộp hồ sơ.', 'MARKETPLACE', 50, 1),
(51, 'Phụ huynh có thể hủy bài đăng tìm gia sư không?', 'Có, nếu đã tìm được người dạy hoặc thay đổi kế hoạch, phụ huynh có thể vào danh sách lớp của mình và chọn "Đóng/Hủy bài đăng" bất kỳ lúc nào.', 'MARKETPLACE', 51, 1),
(52, 'Gia sư có thể rút lại đơn ứng tuyển đã nộp không?', 'Gia sư có thể hủy đơn ứng tuyển trước thời điểm phụ huynh bấm chấp nhận hợp tác trong mục Quản lý ứng tuyển của gia sư.', 'MARKETPLACE', 52, 1),
(53, 'Học thử buổi đầu tiên có mất phí không?', 'Học phí buổi học đầu tiên tùy thuộc vào thỏa thuận giữa hai bên trong hợp đồng, thông thường nếu không hài lòng có thể mở yêu cầu hoàn tiền.', 'MARKETPLACE', 53, 1),
(54, 'TCS hỗ trợ tìm gia sư ở những tỉnh thành nào?', 'TCS hỗ trợ toàn diện các khu vực tại Hà Nội, TP. Hồ Chí Minh, Đà Nẵng, Hải Phòng, Cần Thơ và học Online trên toàn quốc.', 'MARKETPLACE', 54, 1),
(55, 'Làm sao xem danh sách các gia sư được đánh giá cao nhất?', 'Tại mục Tìm gia sư, bạn chọn sắp xếp theo "Điểm đánh giá cao nhất" hoặc "Số lượng đánh giá nhiều nhất" để xem top gia sư uy tín.', 'MARKETPLACE', 55, 1),
(56, 'Có thể thuê gia sư dạy nhóm từ 2–5 học sinh không?', 'Có, phụ huynh có thể tạo bài đăng lớp nhóm tại mục Tạo lớp học và ghi rõ số lượng học sinh để gia sư chuẩn bị giáo án phù hợp.', 'MARKETPLACE', 56, 1),
(57, 'Học viên người nước ngoài có tìm được gia sư dạy Tiếng Việt không?', 'Có, TCS có đội ngũ gia sư sư phạm chuyên ngành Tiếng Việt cho người nước ngoài (Vietnamese for Expats).', 'MARKETPLACE', 57, 1),
(58, 'Xem chi tiết bằng cấp và video giới thiệu của gia sư ở đâu?', 'Bấm trực tiếp vào ảnh hoặc tên gia sư tại trang tìm kiếm /tim-gia-su để xem trang thông tin chi tiết cá nhân đầy đủ.', 'MARKETPLACE', 58, 1),
(59, 'Tìm gia sư dạy các môn năng khiếu Đàn Piano, Guitar, Vẽ?', 'Tại bộ lọc môn học, chọn nhóm Môn Năng khiếu: Piano, Organ, Guitar, Hội họa hoặc Cờ vua.', 'MARKETPLACE', 59, 1),
(60, 'Tìm gia sư can thiệp sớm và trẻ chậm nói?', 'TCS có chuyên mục Giáo dục đặc biệt với các gia sư tốt nghiệp chuyên ngành Giáo dục đặc biệt từ Đại học Sư phạm.', 'MARKETPLACE', 60, 1),
(61, 'Gia sư có thể dạy song song nhiều lớp không?', 'Có, gia sư có thể nhận nhiều lớp học khác nhau miễn là sắp xếp lịch dạy không bị trùng giờ và đảm bảo chất lượng giảng dạy.', 'MARKETPLACE', 61, 1),
(62, 'Làm sao để biết lớp học có khoảng cách gần nhà gia sư?', 'Hệ thống tự động hiển thị khoảng cách và bản đồ vị trí giữa địa chỉ lớp học với vị trí sinh sống của gia sư.', 'MARKETPLACE', 62, 1),
(63, 'Phụ huynh có thể mời trực tiếp 1 gia sư vào dạy lớp không?', 'Có, tại trang cá nhân của gia sư, bấm "Mời dạy lớp" và chọn bài đăng lớp học của bạn để gửi lời mời riêng.', 'MARKETPLACE', 63, 1),
(64, 'Lớp học có bắt buộc phải kết thúc đúng số buổi đăng ký không?', 'Hai bên có thể thỏa thuận gia hạn thêm buổi học bằng cách tạo phụ lục hợp đồng bổ sung trên hệ thống.', 'MARKETPLACE', 64, 1),
(65, 'Thông báo lớp học mới được gửi qua những kênh nào?', 'Gia sư sẽ nhận thông báo lớp phù hợp tức thì qua Email, chuông thông báo trên web và thông báo đẩy qua ứng dụng.', 'MARKETPLACE', 65, 1),

-- =========================================================================
-- 4. TUTOR_OPS (66 to 80)
-- =========================================================================
(66, 'Xem lịch dạy của gia sư ở đâu?', 'Gia sư truy cập mục Lịch dạy tại mục Lịch dạy để xem toàn bộ các ca dạy trong tuần, thời gian bắt đầu và thông tin học viên.', 'TUTOR_OPS', 66, 1),
(67, 'Điểm danh học viên sau mỗi buổi học thế nào?', 'Sau khi kết thúc buổi dạy, gia sư vào mục Lớp học của tôi, chọn buổi học tương ứng và bấm "Điểm danh" kèm nội dung bài đã học.', 'TUTOR_OPS', 67, 1),
(68, 'Xin dời hoặc đổi lịch buổi dạy như thế nào?', 'Tại /tutor/schedule, chọn ca học cần đổi, bấm "Yêu cầu dời lịch", chọn khung giờ mới và gửi lý do để phụ huynh/học viên phê duyệt.', 'TUTOR_OPS', 68, 1),
(69, 'Gia sư xin nghỉ một buổi dạy cần làm gì?', 'Gia sư cần gửi thông báo trước ít nhất 12 giờ trên hệ thống kèm lịch học bù đề xuất để phụ huynh chủ động sắp xếp thời gian.', 'TUTOR_OPS', 69, 1),
(70, 'Yêu cầu gia sư dạy thay (Substitute) hoạt động ra sao?', 'Nếu bận việc đột xuất dài ngày, gia sư có thể tạo yêu cầu người dạy thay tại mục Lớp học của tôi để trung tâm hoặc hệ thống hỗ trợ kết nối.', 'TUTOR_OPS', 70, 1),
(71, 'Ghi chú đánh giá buổi học (Lesson Notes) ở đâu?', 'Khi điểm danh, gia sư có thể nhập nhận xét về mức độ tiếp thu, bài tập về nhà và sự tập trung của học sinh để phụ huynh theo dõi.', 'TUTOR_OPS', 71, 1),
(72, 'Phụ huynh có nhận được thông báo khi gia sư điểm danh không?', 'Có, ngay khi gia sư bấm điểm danh, hệ thống tự động gửi thông báo đến tài khoản phụ huynh để xác nhận buổi học đã diễn ra.', 'TUTOR_OPS', 72, 1),
(73, 'Gia sư đến muộn hoặc về sớm thì tính buổi học thế nào?', 'Gia sư cần đảm bảo dạy đủ tổng thời lượng theo thỏa thuận (thông thường 90–120 phút/buổi), nếu muộn cần bù đủ giờ cho học viên.', 'TUTOR_OPS', 73, 1),
(74, 'Quản lý danh sách tài liệu học tập cho lớp ở đâu?', 'Trong chi tiết lớp học tại mục Lớp học của tôi, gia sư có thể tải lên tệp giáo án, đề kiểm tra PDF để học viên tải về học tập.', 'TUTOR_OPS', 74, 1),
(75, 'Học sinh vắng mặt không báo trước thì điểm danh thế nào?', 'Gia sư chọn trạng thái "Học sinh vắng mặt". Quy định tính phí buổi vắng được áp dụng theo điều khoản trong hợp đồng đã ký kết.', 'TUTOR_OPS', 75, 1),
(76, 'Làm sao đồng bộ lịch dạy với Google Calendar?', 'Tại trang /tutor/schedule, bấm nút "Đồng bộ Google Calendar" và cấp quyền để tự động hiển thị ca dạy trên ứng dụng lịch điện thoại.', 'TUTOR_OPS', 76, 1),
(77, 'Xem tổng số buổi đã dạy và số buổi còn lại ở đâu?', 'Trong bảng quản lý tiến độ lớp học tại mục Lớp học của tôi hiển thị thanh tiến độ trực quan: Số buổi hoàn thành / Tổng số buổi hợp đồng.', 'TUTOR_OPS', 77, 1),
(78, 'Làm gì khi phụ huynh không xác nhận điểm danh?', 'Nếu phụ huynh không khiếu nại trong vòng 48 giờ kể từ khi gia sư điểm danh, hệ thống sẽ tự động xác nhận buổi học hoàn tất.', 'TUTOR_OPS', 78, 1),
(79, 'Có thể thay đổi địa điểm dạy học sau khi nhận lớp không?', 'Địa điểm học chỉ có thể thay đổi khi có sự đồng thuận bằng văn bản hoặc tin nhắn xác nhận giữa phụ huynh và gia sư.', 'TUTOR_OPS', 79, 1),
(80, 'Gia sư có thể yêu cầu kết thúc lớp sớm không?', 'Nếu có lý do chính đáng không thể tiếp tục, gia sư gửi yêu cầu thanh lý hợp đồng sớm tại mục Lớp học của tôi để tiến hành tất toán số buổi đã dạy.', 'TUTOR_OPS', 80, 1),

-- =========================================================================
-- 5. CENTER_OPS (81 to 95)
-- =========================================================================
(81, 'Trung tâm gia sư quản lý danh sách giáo viên ở đâu?', 'Quản trị viên trung tâm truy cập trang Quản lý trung tâm, chọn tab "Gia sư trực thuộc" để xem danh sách, trạng thái và phân công lớp học.', 'CENTER_OPS', 81, 1),
(82, 'Thêm gia sư mới vào trung tâm gia sư như thế nào?', 'Tại /center, bấm nút "Thêm gia sư", nhập email hoặc mã gia sư trên hệ thống TCS để gửi lời mời gia nhập trung tâm.', 'CENTER_OPS', 82, 1),
(83, 'Đăng bài tuyển dụng gia sư cho trung tâm ở đâu?', 'Trung tâm vào mục Tuyển dụng tại trang Quản lý trung tâm/recruitment, điền yêu cầu tuyển dụng, mức lương và chế độ đãi ngộ để tiếp nhận hồ sơ.', 'CENTER_OPS', 83, 1),
(84, 'Duyệt gia sư ứng tuyển vào trung tâm ra sao?', 'Tại /center/recruitment, trung tâm xem danh sách ứng viên nộp hồ sơ, kiểm tra CV/bằng cấp và bấm "Phê duyệt" để thêm vào đội ngũ.', 'CENTER_OPS', 84, 1),
(85, 'Hợp đồng giữa trung tâm và gia sư trực thuộc quản lý ở đâu?', 'Mục Hợp đồng trung tâm lưu trữ toàn bộ hợp đồng hợp tác, tỷ lệ ăn chia hoa hồng và cam kết chất lượng giữa trung tâm với gia sư.', 'CENTER_OPS', 85, 1),
(86, 'Xem báo cáo doanh thu và dòng tiền của trung tâm ở đâu?', 'Tại mục Báo cáo doanh thu trung tâm, trung tâm có thể theo dõi biểu đồ doanh thu theo tuần/tháng, số lượng lớp đang chạy và tiền hoa hồng thực nhận.', 'CENTER_OPS', 86, 1),
(87, 'Tạo lớp học nhóm cho trung tâm gia sư như thế nào?', 'Trung tâm vào mục Lớp học của trung tâm, chọn "Tạo lớp nhóm", thiết lập sĩ số tối đa, mức học phí từng học viên và phân công giáo viên đứng lớp.', 'CENTER_OPS', 87, 1),
(88, 'Xóa hoặc gỡ gia sư khỏi trung tâm thế nào?', 'Trong danh sách gia sư tại trang Quản lý trung tâm, chọn gia sư cần gỡ, bấm "Rút khỏi trung tâm" sau khi đã tất toán toàn bộ các lớp phụ trách.', 'CENTER_OPS', 88, 1),
(89, 'Trung tâm có thể phân công gia sư dạy thay cho lớp không?', 'Có, trung tâm có quyền điều phối và đổi giáo viên phụ trách lớp học khi giáo viên chính có việc bận đột xuất.', 'CENTER_OPS', 89, 1),
(90, 'Thiết lập tỷ lệ hoa hồng trung tâm (Commission Rate) ở đâu?', 'Tại mục Cài đặt trung tâm Cài đặt trung tâm, quản trị viên có thể cấu hình tỷ lệ hoa hồng cố định hoặc linh hoạt theo từng môn học.', 'CENTER_OPS', 90, 1),
(91, 'Trung tâm có thể xuất báo cáo tài chính ra file Excel/CSV không?', 'Có, tại trang Quản lý trung tâm/analytics có nút "Xuất báo cáo CSV" để tải về dữ liệu thu chi, học phí và lương giáo viên chi tiết.', 'CENTER_OPS', 91, 1),
(92, 'Học viên thanh toán học phí cho trung tâm qua đâu?', 'Học viên nạp tiền và thanh toán qua tài khoản ký quỹ Escrow của TCS để đảm bảo an toàn, tiền sẽ tự động chia về ví trung tâm sau khi hoàn tất.', 'CENTER_OPS', 92, 1),
(93, 'Trung tâm gia sư có thể tạo nhiều chi nhánh không?', 'Có, trung tâm có thể quản lý nhiều cơ sở giảng dạy khác nhau trên cùng một bảng điều khiển trung tâm tại trang Quản lý trung tâm.', 'CENTER_OPS', 93, 1),
(94, 'Đánh giá uy tín của trung tâm gia sư được tính thế nào?', 'Dựa trên điểm trung bình sao của tất cả các lớp học do giáo viên trực thuộc trung tâm giảng dạy và tỷ lệ giải quyết khiếu nại thành công.', 'CENTER_OPS', 94, 1),
(95, 'Làm sao để đăng ký mở tài khoản Trung tâm gia sư?', 'Tại trang Đăng ký, chọn vai trò "Trung tâm gia sư (Tutor Center)", điền tên tổ chức, giấy phép kinh doanh và thông tin người đại diện.', 'CENTER_OPS', 95, 1),

-- =========================================================================
-- 6. FINANCE_ESCROW (96 to 125)
-- =========================================================================
(96, 'Cơ chế ký quỹ Escrow trên TCS hoạt động như thế nào?', 'Khi phụ huynh chọn gia sư, học phí được giữ an toàn tại tài khoản Escrow của TCS. Tiền chỉ được giải ngân cho gia sư sau khi các buổi học hoàn thành đúng cam kết.', 'FINANCE_ESCROW', 96, 1),
(97, 'Phí nền tảng (Platform Fee) của TCS là bao nhiêu?', 'TCS áp dụng mức phí sàn cố định 2% trên giá trị hợp đồng thành công để duy trì vận hành hệ thống, bảo vệ ký quỹ và chăm sóc khách hàng 24/7.', 'FINANCE_ESCROW', 97, 1),
(98, 'Làm sao nạp tiền vào ví bằng mã QR SePay tự động?', 'Vào mục Ví tiền & Tài chính, chọn "Nạp tiền", nhập số tiền cần nạp, hệ thống sẽ tạo mã VietQR SePay tự động. Bạn chỉ cần quét mã trên App ngân hàng để tiền vào ví tức thì.', 'FINANCE_ESCROW', 98, 1),
(99, 'Gia sư rút tiền về tài khoản ngân hàng như thế nào?', 'Vào mục Ví tiền & Tài chính, chọn "Rút tiền", nhập số tài khoản ngân hàng thụ hưởng, tên chủ tài khoản và số tiền cần rút (tối thiểu 50.000 ₫) rồi bấm Xác nhận.', 'FINANCE_ESCROW', 99, 1),
(100, 'Thời gian xử lý yêu cầu rút tiền mất bao lâu?', 'Hệ thống đối soát và chuyển tiền tự động trong vòng 1–4 giờ làm việc. Tối đa không quá 24 giờ kể từ khi lệnh rút được tạo.', 'FINANCE_ESCROW', 100, 1),
(101, 'Xem lịch sử giao dịch và biến động số dư ở đâu?', 'Tại mục Ví tiền & Tài chính/history hiển thị đầy đủ nhật ký nạp tiền, trừ tiền cọc Escrow, nhận học phí giải ngân và các khoản phí dịch vụ.', 'FINANCE_ESCROW', 101, 1),
(102, 'Chính sách hoàn tiền học phí (Refund Policy) như thế nào?', 'Nếu lớp học bị hủy trước khi bắt đầu, phụ huynh được hoàn 100% tiền Escrow. Nếu hủy giữa chừng, tiền hoàn được tính theo tỷ lệ các buổi chưa học.', 'FINANCE_ESCROW', 102, 1),
(103, 'Vì sao số dư trong ví bị tạm giữ (Held / Frozen)?', 'Số dư bị tạm giữ khi đang nằm trong hợp đồng lớp học đang diễn ra hoặc tài khoản đang có lệnh rút tiền chờ ngân hàng xử lý.', 'FINANCE_ESCROW', 103, 1),
(104, 'Gia sư xem tổng thu nhập tháng này ở đâu?', 'Tại mục Ví tiền & Tài chính hiển thị thẻ "Thu nhập tháng hiện tại", thống kê số tiền thực nhận sau khi đã trừ phí nền tảng 2%.', 'FINANCE_ESCROW', 104, 1),
(105, 'Nạp tiền bằng chuyển khoản ngân hàng có mất phí không?', 'TCS không thu bất kỳ khoản phí nạp tiền nào. Bạn được miễn phí nạp 100% qua cổng chuyển khoản VietQR SePay.', 'FINANCE_ESCROW', 105, 1),
(106, 'Rút tiền về ngân hàng có bị giới hạn số lần trong ngày không?', 'Mỗi tài khoản được thực hiện tối đa 3 lệnh rút tiền/ngày với tổng hạn mức rút không vượt quá 50.000.000 ₫/ngày.', 'FINANCE_ESCROW', 106, 1),
(107, 'Làm gì khi nạp tiền thành công mà số dư ví chưa cập nhật?', 'Hệ thống SePay tự động cộng tiền trong 30 giây. Nếu mạng ngân hàng chậm, bạn gửi ảnh biên lai chuyển tiền tại mục Hỗ trợ & Khiếu nại để nhân viên hỗ trợ cộng ngay.', 'FINANCE_ESCROW', 107, 1),
(108, 'Tiền ký quỹ Escrow được giải ngân theo từng buổi hay cả khóa?', 'Mặc định tiền được giải ngân định kỳ theo từng tháng hoặc sau khi hoàn tất toàn bộ số buổi học tùy theo cấu hình của hợp đồng.', 'FINANCE_ESCROW', 108, 1),
(109, 'Gia sư có phải trả phí trước khi nhận lớp không?', 'Không, TCS tuyệt đối KHÔNG thu phí nhận lớp trước của gia sư. Phí nền tảng chỉ được trừ tự động khi gia sư đã hoàn thành giảng dạy và nhận tiền.', 'FINANCE_ESCROW', 109, 1),
(110, 'Phụ huynh có bị mất phí nền tảng khi thanh toán không?', 'Không, phụ huynh chỉ thanh toán đúng số tiền học phí theo mức giá đã thỏa thuận với gia sư mà không mất thêm phụ phí nào.', 'FINANCE_ESCROW', 110, 1),
(111, 'Trạng thái lệnh rút tiền PENDING, APPROVED, REJECTED, TRANSFER_FAILED nghĩa là gì?', 'PENDING: Đang chờ duyệt; APPROVED: Đã duyệt và gửi lệnh chi; REJECTED: Bị từ chối do sai thông tin TK; TRANSFER_FAILED: Ngân hàng lỗi đường truyền.', 'FINANCE_ESCROW', 111, 1),
(112, 'Làm sao liên kết tài khoản ngân hàng nhận tiền?', 'Trong mục Ví tiền & Tài chính, chọn "Tài khoản ngân hàng", chọn tên ngân hàng, nhập số tài khoản và bấm Lưu để dùng cho các lần rút tiền sau.', 'FINANCE_ESCROW', 112, 1),
(113, 'Có thể rút tiền về ví điện tử MoMo, ZaloPay không?', 'Hiện tại TCS hỗ trợ chuyển tiền trực tiếp về hơn 40 ngân hàng nội địa Việt Nam thông qua hệ thống Napas 247.', 'FINANCE_ESCROW', 113, 1),
(114, 'Tiền hoàn trả (Refund) được chuyển về đâu?', 'Tiền hoàn trả được cộng trực tiếp vào Số dư khả dụng của ví TCS. Phụ huynh có thể dùng để thuê gia sư khác hoặc rút về tài khoản ngân hàng.', 'FINANCE_ESCROW', 114, 1),
(115, 'Hợp đồng bị tranh chấp thì tiền Escrow được xử lý thế nào?', 'Toàn bộ số tiền ký quỹ sẽ được đóng băng bảo vệ cho đến khi Quản trị viên đối soát bằng chứng và đưa ra phán quyết hòa giải thỏa đáng.', 'FINANCE_ESCROW', 115, 1),
(116, 'Có thể xuất hóa đơn giá trị gia tăng (VAT) cho học phí không?', 'Doanh nghiệp hoặc phụ huynh có nhu cầu xuất hóa đơn VAT phí dịch vụ có thể gửi thông tin mã số thuế công ty tại mục Hỗ trợ & Khiếu nại.', 'FINANCE_ESCROW', 116, 1),
(117, 'Số tiền rút tối thiểu và tối đa cho mỗi giao dịch là bao nhiêu?', 'Số tiền rút tối thiểu là 50.000 ₫/lệnh và tối đa là 20.000.000 ₫ cho một lần rút.', 'FINANCE_ESCROW', 117, 1),
(118, 'Làm sao kiểm tra mã tham chiếu giao dịch (Transaction Ref)?', 'Trong bảng lịch sử mục Lịch sử giao dịch, mỗi giao dịch đều có mã tham chiếu duy nhất dạng TX-XXXXXX để tra cứu khi cần đối soát.', 'FINANCE_ESCROW', 118, 1),
(119, 'Học viên có thể thanh toán học phí từng buổi một không?', 'Có, học viên có thể ký hợp đồng theo gói ngắn hạn (ví dụ gói 4 buổi/lần) để nạp ký quỹ linh hoạt thay vì nạp cả khóa dài hạn.', 'FINANCE_ESCROW', 119, 1),
(120, 'Gia sư có thể chuyển tiền từ ví sang tài khoản học viên khác không?', 'Hiện tại ví TCS chỉ hỗ trợ chức năng nạp tiền, nhận học phí lớp học và rút tiền về tài khoản ngân hàng chính chủ của người dùng.', 'FINANCE_ESCROW', 120, 1),
(121, 'Tài khoản ngân hàng rút tiền có bắt buộc trùng tên với CCCD không?', 'Để phòng chống gian lận và rửa tiền, tên chủ tài khoản ngân hàng nhận tiền phải trùng khớp với họ tên đã xác minh trên hồ sơ CCCD.', 'FINANCE_ESCROW', 121, 1),
(122, 'Phí phạt khi hủy lớp học sát giờ là bao nhiêu?', 'Nếu một bên tự ý hủy lớp trước giờ học dưới 2 giờ mà không có lý do bất khả kháng, bên đó có thể bị trừ 50% học phí của buổi học đó.', 'FINANCE_ESCROW', 122, 1),
(123, 'Làm sao để xem báo cáo tài chính tổng quan theo năm?', 'Tại mục Ví tiền & Tài chính, chọn bộ lọc khoảng thời gian từ ngày bắt đầu đến ngày kết thúc để xem tổng hợp dòng tiền ra vào trong năm.', 'FINANCE_ESCROW', 123, 1),
(124, 'Nạp tiền qua thẻ tín dụng quốc tế Visa/Mastercard có được không?', 'TCS hiện đang hỗ trợ thanh toán nội địa qua QR SePay/VNPAY và đang tích hợp cổng thanh toán thẻ quốc tế trong thời gian tới.', 'FINANCE_ESCROW', 124, 1),
(125, 'Tiền ký quỹ Escrow được bảo chứng bởi đơn vị nào?', 'Tất cả các khoản tiền ký quỹ được bảo chứng và phong tỏa tại tài khoản đối tác thanh toán ngân hàng thương mại được cấp phép.', 'FINANCE_ESCROW', 125, 1),

-- =========================================================================
-- 7. CONTRACT_REVIEW (126 to 140)
-- =========================================================================
(126, 'Hợp đồng điện tử trên TCS có giá trị pháp lý không?', 'Có, hợp đồng điện tử trên TCS được xác thực bằng mã OTP theo quy định của Luật Giao dịch điện tử Việt Nam, ràng buộc nghĩa vụ giữa phụ huynh và gia sư.', 'CONTRACT_REVIEW', 126, 1),
(127, 'Làm sao để ký hợp đồng lớp học bằng mã OTP?', 'Khi nhận được thông báo hợp đồng tại mục Quản lý hợp đồng, bạn đọc kỹ các điều khoản, bấm "Ký hợp đồng", nhập mã OTP 6 số gửi về điện thoại để hoàn tất.', 'CONTRACT_REVIEW', 127, 1),
(128, 'Xem danh sách các hợp đồng đã ký ở đâu?', 'Bạn truy cập mục Quản lý hợp đồng để xem toàn bộ danh sách hợp đồng: Đang chờ ký (PENDING), Đang hiệu lực (ACTIVE), hoặc Đã kết thúc (COMPLETED).', 'CONTRACT_REVIEW', 128, 1),
(129, 'Đánh giá gia sư sau khóa học như thế nào?', 'Khi lớp học kết thúc, phụ huynh vào mục Lớp học của tôi, chọn lớp học và bấm "Đánh giá gia sư" để chấm điểm sao (1–5 sao) kèm lời nhận xét chi tiết.', 'CONTRACT_REVIEW', 129, 1),
(130, 'Điểm uy tín (Reputation Score) của gia sư được tính ra sao?', 'Điểm uy tín được tính tự động dựa trên: (1) Điểm đánh giá trung bình từ học viên; (2) Tỷ lệ hoàn thành lớp học; (3) Tỷ lệ điểm danh đúng giờ.', 'CONTRACT_REVIEW', 130, 1),
(131, 'Có thể sửa hoặc xóa nhận xét đánh giá đã gửi không?', 'Đánh giá sau khi gửi sẽ hiển thị công khai để đảm bảo tính khách quan. Bạn có thể gửi yêu cầu chỉnh sửa trong vòng 7 ngày tại mục Hỗ trợ & Khiếu nại.', 'CONTRACT_REVIEW', 131, 1),
(132, 'Gia sư có thể từ chối ký hợp đồng không?', 'Có, nếu điều kiện lịch học hoặc mức học phí không còn phù hợp, gia sư có thể bấm "Từ chối hợp đồng" và nêu rõ lý do.', 'CONTRACT_REVIEW', 132, 1),
(133, 'Hợp đồng có thể gia hạn thêm buổi học không?', 'Có, phụ huynh và gia sư có thể tạo phụ lục gia hạn hợp đồng trực tiếp trên giao diện quản lý lớp tại mục Quản lý hợp đồng.', 'CONTRACT_REVIEW', 133, 1),
(134, 'Hợp đồng mẫu của TCS gồm những điều khoản chính nào?', 'Bao gồm: Môn học, số buổi học/tuần, mức học phí, cam kết chất lượng, quy định hoàn tiền Escrow và chế tài xử lý vi phạm hợp đồng.', 'CONTRACT_REVIEW', 134, 1),
(135, 'Đánh giá ẩn danh (Anonymous Review) có được hỗ trợ không?', 'Đánh giá sẽ hiển thị tên viết tắt của phụ huynh/học viên để đảm bảo tính chân thực và tránh các trường hợp đánh giá ảo.', 'CONTRACT_REVIEW', 135, 1),
(136, 'Vì sao đánh giá của tôi không hiển thị trên hồ sơ gia sư?', 'Các đánh giá chứa từ ngữ phản cảm, vi phạm thuần phong mỹ tục hoặc spam quảng cáo sẽ bị bộ lọc tự động ẩn để chờ quản trị viên kiểm duyệt.', 'CONTRACT_REVIEW', 136, 1),
(137, 'Gia sư có thể phản hồi lại nhận xét của phụ huynh không?', 'Có, gia sư có thể viết phản hồi lịch sự dưới mỗi bài đánh giá tại trang cá nhân để giải thích và trao đổi thông tin minh bạch.', 'CONTRACT_REVIEW', 137, 1),
(138, 'Tải bản in hợp đồng dạng PDF ở đâu?', 'Trong chi tiết hợp đồng tại mục Quản lý hợp đồng/{id}, bấm nút "Tải PDF" để lưu về máy bản hợp đồng có đóng dấu điện tử của nền tảng TCS.', 'CONTRACT_REVIEW', 138, 1),
(139, 'Nếu phụ huynh không ký hợp đồng thì lớp học có bắt đầu không?', 'Lớp học chỉ được kích hoạt chính thức và bắt đầu tính buổi học sau khi cả hai bên đã hoàn tất ký OTP và phụ huynh đã nạp cọc Escrow.', 'CONTRACT_REVIEW', 139, 1),
(140, 'Mức đánh giá bao nhiêu sao thì gia sư bị cảnh cáo?', 'Gia sư có điểm đánh giá trung bình dưới 3.0 sao trong 3 lớp học liên tiếp sẽ nhận cảnh báo chất lượng và tạm ngưng quyền nhận lớp mới.', 'CONTRACT_REVIEW', 140, 1),

-- =========================================================================
-- 8. TRUST_SAFETY (141 to 170)
-- =========================================================================
(141, 'Làm sao báo cáo gia sư hoặc trung tâm lách sàn?', 'Bạn truy cập mục /support/tickets, chọn loại yêu cầu "Báo cáo vi phạm / Lách sàn", cung cấp số điện thoại hoặc liên kết lớp học kèm bằng chứng tin nhắn.', 'TRUST_SAFETY', 141, 1),
(142, 'Hành vi lách sàn (Circumvention) bị xử lý như thế nào?', 'TCS xử phạt nghiêm khắc: Khóa tài khoản vĩnh viễn, tịch thu số dư vi phạm và từ chối cung cấp dịch vụ cho tất cả các bên liên quan.', 'TRUST_SAFETY', 142, 1),
(143, 'Vì sao không nên giao dịch và chuyển tiền ngoài sàn?', 'Giao dịch ngoài sàn sẽ mất hoàn toàn sự bảo vệ của quỹ Escrow; khi gia sư bùng lịch hoặc phụ huynh quỵt tiền, TCS không thể can thiệp đòi lại học phí.', 'TRUST_SAFETY', 143, 1),
(144, 'Khi nào nên mở tranh chấp (Dispute) lớp học?', 'Nên mở tranh chấp khi gia sư bỏ dạy giữa chừng, dạy sai kiến thức nghiêm trọng, học sinh không thanh toán học phí hoặc có hành vi quấy rối.', 'TRUST_SAFETY', 144, 1),
(145, 'Quy trình giải quyết tranh chấp của TCS gồm các bước nào?', '(1) Tiếp nhận đơn tranh chấp; (2) Tạm khóa tiền ký quỹ; (3) Yêu cầu hai bên nộp bằng chứng trong 48h; (4) Hòa giải và đưa ra quyết định phân chia tiền hoàn.', 'TRUST_SAFETY', 145, 1),
(146, 'Tải bằng chứng tranh chấp lên hệ thống như thế nào?', 'Trong giao diện tranh chấp tại mục Hỗ trợ & Khiếu nại, bạn có thể tải lên ảnh chụp màn hình tin nhắn Zalo/SMS, bản ghi âm, hoặc video buổi học (tối đa 10 tệp).', 'TRUST_SAFETY', 146, 1),
(147, 'Thời gian xử lý một vụ tranh chấp là bao lâu?', 'Hội đồng Hòa giải TCS xử lý và đưa ra phán quyết chính thức trong vòng 3–5 ngày làm việc kể từ khi nhận đủ bằng chứng từ cả hai phía.', 'TRUST_SAFETY', 147, 1),
(148, 'Bị phạt cảnh cáo hoặc trừ điểm uy tín vì những lỗi gì?', 'Các lỗi: Thường xuyên đến muộn, tự ý hủy ca dạy sát giờ, có thái độ không chuẩn mực, hoặc bị học viên khiếu nại nhiều lần.', 'TRUST_SAFETY', 148, 1),
(149, 'Chế tài xử phạt khi vi phạm quy tắc cộng đồng TCS?', 'Các mức độ: (1) Cảnh cáo bằng văn bản; (2) Trừ 10–50 điểm uy tín; (3) Tạm ngưng quyền nhận lớp 14 ngày; (4) Khóa tài khoản vĩnh viễn.', 'TRUST_SAFETY', 149, 1),
(150, 'Làm sao tố cáo gia sư thu tiền học phí trực tiếp từ học sinh?', 'Gửi phiếu tố cáo tại mục Hỗ trợ & Khiếu nại kèm biên lai chuyển khoản hoặc tin nhắn yêu cầu thu tiền riêng để nhận thưởng bảo vệ cộng đồng từ TCS.', 'TRUST_SAFETY', 150, 1),
(151, 'Học sinh bị quấy rối hoặc đối xử thiếu văn minh phải làm sao?', 'Hãy lập tức dừng buổi học, liên hệ hotline khẩn cấp của TCS và tạo phiếu báo cáo khẩn tại mục Hỗ trợ & Khiếu nại để được can thiệp pháp lý.', 'TRUST_SAFETY', 151, 1),
(152, 'Gia sư bị phụ huynh đe dọa hoặc quỵt tiền học phí?', 'Nếu giao dịch được thực hiện qua hợp đồng Escrow trên TCS, học phí của gia sư được bảo vệ 100% và sẽ được giải ngân theo đúng cam kết.', 'TRUST_SAFETY', 152, 1),
(153, 'Tài khoản bị khóa do nghi vấn gian lận có khiếu nại được không?', 'Bạn có quyền gửi đơn giải trình tại mục Hỗ trợ & Khiếu nại kèm chứng từ minh bạch để Ban Kiểm soát TCS xem xét mở lại tài khoản.', 'TRUST_SAFETY', 153, 1),
(154, 'Chính sách bảo vệ trẻ em và người học vị thành niên?', 'Tất cả gia sư đều phải qua kiểm tra lý lịch CCCD và ký cam kết bảo vệ an toàn cho học sinh trước khi được phép nhận lớp tại nhà.', 'TRUST_SAFETY', 154, 1),
(155, 'Người dùng có bị lộ danh tính khi gửi tố cáo không?', 'Mọi thông tin người tố cáo và người gửi phản ánh vi phạm đều được bảo mật tuyệt đối theo chính sách bảo vệ nhân chứng của TCS.', 'TRUST_SAFETY', 155, 1),
(156, 'Làm sao nhận biết các hành vi lừa đảo giả mạo gia sư?', 'Cảnh giác với các yêu cầu chuyển cọc trước qua tài khoản cá nhân ngoài nền tảng hoặc gia sư từ chối cung cấp thẻ sinh viên/CCCD.', 'TRUST_SAFETY', 156, 1),
(157, 'Hủy lớp học sát giờ thi có bị phạt không?', 'Hủy lớp trong vòng 7 ngày trước kỳ thi quan trọng mà không có lý do bất khả kháng sẽ bị trừ 100 điểm uy tín và bồi thường theo hợp đồng.', 'TRUST_SAFETY', 157, 1),
(158, 'Tranh chấp hợp đồng không đạt được thỏa thuận thì xử lý ra sao?', 'Nếu không đồng ý với kết quả hòa giải nội bộ, các bên có quyền đưa vụ việc ra cơ quan Trọng tài thương mại hoặc Tòa án nhân dân có thẩm quyền.', 'TRUST_SAFETY', 158, 1),
(159, 'Làm sao bảo vệ thông tin số điện thoại của học viên?', 'TCS chỉ hiển thị số điện thoại liên lạc sau khi phụ huynh và gia sư đã hoàn tất ký hợp đồng lớp học chính thức.', 'TRUST_SAFETY', 159, 1),
(160, 'Trung tâm gia sư có được thu tiền môi giới của sinh viên không?', 'Trung tâm hoạt động trên TCS phải tuân thủ mức phí minh bạch và không được thu các khoản tiền đặt cọc giữ chỗ trái quy định pháp luật.', 'TRUST_SAFETY', 160, 1),
(161, 'Tài khoản bị đánh giá tiêu cực ác ý có được gỡ bỏ không?', 'Nếu chứng minh được đánh giá xuất phát từ đối thủ cạnh tranh hoặc không phản ánh đúng thực tế, quản trị viên sẽ gỡ bỏ đánh giá đó.', 'TRUST_SAFETY', 161, 1),
(162, 'Làm sao để biết một lớp học có dấu hiệu bất thường?', 'Các bài đăng có học phí cao bất thường nhưng nội dung mơ hồ hoặc yêu cầu gặp mặt ở địa điểm nhạy cảm sẽ bị hệ thống gắn cờ cảnh báo.', 'TRUST_SAFETY', 162, 1),
(163, 'Có được chia sẻ tài khoản TCS cho người khác dùng chung không?', 'Mỗi tài khoản gắn liền với danh tính và bằng cấp của một cá nhân duy nhất, việc cho mượn tài khoản sẽ dẫn đến việc bị khóa tài khoản vĩnh viễn.', 'TRUST_SAFETY', 163, 1),
(164, 'Quy định về văn hóa giao tiếp và ứng xử trên TCS?', 'Thành viên phải luôn giữ thái độ tôn trọng, lịch sự, không sử dụng ngôn từ tục tĩu, xúc phạm hoặc phân biệt đối xử.', 'TRUST_SAFETY', 164, 1),
(165, 'Báo cáo sai sự thật để hãm hại người khác bị xử lý thế nào?', 'Hành vi vu khống và báo cáo sai sự thật có chủ đích sẽ bị xử phạt khóa tài khoản người báo cáo và trừ toàn bộ điểm uy tín.', 'TRUST_SAFETY', 165, 1),
(166, 'Trường hợp bất khả kháng do thiên tai, dịch bệnh được xử lý ra sao?', 'Lớp học được tạm dừng và bảo lưu học phí Escrow vô thời hạn cho đến khi các bên có thể tiếp tục việc dạy học an toàn.', 'TRUST_SAFETY', 166, 1),
(167, 'Gia sư có được dạy kèm cùng lúc học sinh của trường mình đang công tác không?', 'Gia sư là giáo viên trường công lập cần tuân thủ các quy định hiện hành của Bộ Giáo dục và Đào tạo về việc dạy thêm ngoài nhà trường.', 'TRUST_SAFETY', 167, 1),
(168, 'Hệ thống tự động phát hiện vi phạm lách sàn bằng công nghệ gì?', 'TCS tích hợp thuật toán AI quét tự động các tin nhắn chứa số điện thoại, số tài khoản ngân hàng hoặc liên kết giao dịch ngoài sàn.', 'TRUST_SAFETY', 168, 1),
(169, 'Khóa tài khoản vĩnh viễn có được tạo lại tài khoản mới không?', 'Người dùng bị khóa vĩnh viễn do vi phạm nghiêm trọng sẽ bị chặn số điện thoại, email và số CCCD trên toàn hệ thống TCS.', 'TRUST_SAFETY', 169, 1),
(170, 'Mức thưởng cho người dùng phát hiện và báo cáo lách sàn?', 'Người dùng báo cáo chính xác hành vi gian lận sẽ được tặng voucher giảm giá 50% phí dịch vụ cho lần thuê gia sư tiếp theo.', 'TRUST_SAFETY', 170, 1),

-- =========================================================================
-- 9. PLATFORM_ADMIN (171 to 195)
-- =========================================================================
(171, 'Bảng điều khiển quản trị Admin Dashboard nằm ở đâu?', 'Quản trị viên nền tảng truy cập bảng điều khiển Quản trị viên để theo dõi tổng quan các chỉ số người dùng, lớp học, doanh thu và các tác vụ cần xử lý.', 'PLATFORM_ADMIN', 171, 1),
(172, 'Xem báo cáo doanh thu và phân tích dòng tiền ở đâu?', 'Báo cáo doanh thu và tài chính chi tiết được hiển thị tại bảng điều khiển Quản trị viên/analytics với biểu đồ dòng tiền vào (Money In) và dòng tiền ra (Money Out).', 'PLATFORM_ADMIN', 172, 1),
(173, 'Hàng đợi duyệt xác minh hồ sơ (Verification Queue) ở đâu?', 'Admin vào trang Quản trị Xác minh để xem danh sách gia sư và trung tâm đang chờ xét duyệt bằng cấp và căn cước công dân.', 'PLATFORM_ADMIN', 173, 1),
(174, 'Quản lý và phê duyệt yêu cầu rút tiền ở đâu?', 'Admin truy cập trang Quản trị Rút tiền để kiểm tra, duyệt lệnh chuyển tiền ngân hàng hoặc từ chối các yêu cầu rút tiền không hợp lệ.', 'PLATFORM_ADMIN', 174, 1),
(175, 'Quản lý các vụ tranh chấp lớp học ở đâu?', 'Admin truy cập trang Quản trị Tranh chấp để xem các khiếu nại, đối soát bằng chứng từ hai bên và thực hiện phân chia tiền hoàn Escrow.', 'PLATFORM_ADMIN', 175, 1),
(176, 'Xem nhật ký hoạt động hệ thống (Audit Log) ở đâu?', 'Toàn bộ nhật ký thao tác đăng nhập, chỉnh sửa dữ liệu, phân quyền và giao dịch được ghi lại minh bạch tại bảng điều khiển Quản trị viên.', 'PLATFORM_ADMIN', 176, 1),
(177, 'Lọc các tác vụ quá hạn cam kết SLA như thế nào?', 'Tại bảng điều khiển /platform, Admin có thể lọc các ticket hỗ trợ hoặc hồ sơ xác minh vượt quá 24h để ưu tiên xử lý khẩn cấp.', 'PLATFORM_ADMIN', 177, 1),
(178, 'Tính năng Reindex cơ sở tri thức AI hoạt động thế nào?', 'Admin vào trang Báo cáo thống kê quản trị, bấm "Reindex AI Knowledge Base" để đồng bộ dữ liệu FAQ và tài liệu mới vào vector database của AI Assistant.', 'PLATFORM_ADMIN', 178, 1),
(179, 'Cấu hình tỷ lệ phí nền tảng (Platform Fee Rate) ở đâu?', 'Admin có thể điều chỉnh tỷ lệ phí sàn tại mục Cấu hình hệ thống trong trang quản trị /platform.', 'PLATFORM_ADMIN', 179, 1),
(180, 'Xuất báo cáo dữ liệu dạng file CSV ở đâu?', 'Tại các bảng quản lý người dùng, giao dịch và lớp học trên bảng điều khiển Quản trị viên đều có nút "Export CSV" để tải về báo cáo đối soát.', 'PLATFORM_ADMIN', 180, 1),
(181, 'Phân quyền tài khoản quản trị viên phụ (Moderator) thế nào?', 'Super Admin có thể cấp quyền kiểm duyệt hồ sơ, xử lý ticket hoặc xem báo cáo tài chính cho từng nhân sự tại bảng điều khiển Quản trị viên.', 'PLATFORM_ADMIN', 181, 1),
(182, 'Quản lý danh mục môn học và khối lớp tại đâu?', 'Admin có thể thêm mới hoặc ẩn các môn học, chuyên đề đào tạo trong mục Quản lý Danh mục tại bảng điều khiển Quản trị viên.', 'PLATFORM_ADMIN', 182, 1),
(183, 'Xử lý khi hệ thống ngân hàng bị lỗi cổng thanh toán?', 'Admin có thể tạm ngưng tính năng nạp rút tự động và chuyển sang chế độ đối soát thủ công để đảm bảo không thất thoát dòng tiền.', 'PLATFORM_ADMIN', 183, 1),
(184, 'Xem số lượng người dùng đang trực tuyến (Real-time Active Users)?', 'Biểu đồ người dùng thời gian thực được hiển thị trên bảng thống kê giám sát vận hành tại bảng điều khiển Quản trị viên/analytics.', 'PLATFORM_ADMIN', 184, 1),
(185, 'Khóa tài khoản vi phạm từ trang quản trị như thế nào?', 'Trong danh sách người dùng tại bảng điều khiển Quản trị viên, tìm kiếm tài khoản, chọn "Khóa tài khoản", nhập lý do và thời gian khóa.', 'PLATFORM_ADMIN', 185, 1),
(186, 'Xem tổng giá trị học phí đang ký quỹ (Total Escrow Balance)?', 'Tổng tiền học phí đang được bảo lưu an toàn trong hệ thống được cập nhật thời gian thực tại chỉ số "Escrow Balance" trên bảng điều khiển Quản trị viên.', 'PLATFORM_ADMIN', 186, 1),
(187, 'Theo dõi tỷ lệ hoàn thành lớp học (Completion Rate)?', 'Tỷ lệ lớp học hoàn tất thành công trên tổng số lớp mở được biểu diễn dạng biểu đồ hình tròn tại bảng điều khiển Quản trị viên/analytics.', 'PLATFORM_ADMIN', 187, 1),
(188, 'Cấu hình ngưỡng cảnh báo gian lận tự động?', 'Admin có thể đặt các ngưỡng cảnh báo như: Số lần nhập sai OTP, số tin nhắn chứa số điện thoại hoặc tần suất tạo lớp bất thường.', 'PLATFORM_ADMIN', 188, 1),
(189, 'Kiểm tra độ chính xác và phản hồi của AI Assistant?', 'Admin theo dõi tỷ lệ phân loại Intent đúng, số lượt người dùng đánh giá câu trả lời hữu ích tại tab Giám sát AI trên bảng điều khiển Quản trị viên/analytics.', 'PLATFORM_ADMIN', 189, 1),
(190, 'Sao lưu cơ sở dữ liệu hệ thống định kỳ thế nào?', 'Hệ thống tự động thực hiện snapshot sao lưu dữ liệu mỗi ngày vào 02:00 sáng và lưu trữ an toàn tại kho lưu trữ đám mây.', 'PLATFORM_ADMIN', 190, 1),
(191, 'Xem danh sách các gia sư có điểm uy tín thấp cần kiểm tra?', 'Bộ lọc tại bảng điều khiển Quản trị viên/verifications cho phép lọc các gia sư có điểm đánh giá dưới 3.5 sao để bộ phận CSKH liên hệ hỗ trợ.', 'PLATFORM_ADMIN', 191, 1),
(192, 'Quản lý và cập nhật nội dung các bài viết FAQ ở đâu?', 'Admin có thể thêm mới, sửa đổi hoặc xóa các bài hỏi đáp tri thức tại mục Quản lý FAQ trên bảng điều khiển Quản trị viên.', 'PLATFORM_ADMIN', 192, 1),
(193, 'Quy trình xử lý hoàn tiền thủ công cho học viên?', 'Trong trường hợp đặc biệt, Admin có thể phê duyệt lệnh hoàn tiền trực tiếp từ tài khoản Escrow về ví học viên tại bảng điều khiển Quản trị viên/disputes.', 'PLATFORM_ADMIN', 193, 1),
(194, 'Gửi thông báo toàn hệ thống (Broadcast Notification)?', 'Admin có thể soạn thảo và gửi thông báo chung đến toàn bộ Học viên, Gia sư hoặc Trung tâm trong mục Thông báo hệ thống.', 'PLATFORM_ADMIN', 194, 1),
(195, 'Theo dõi thời gian phản hồi trung bình của máy chủ (Server Latency)?', 'Chỉ số sức khỏe hạ tầng hệ thống, thời gian phản hồi API và mức tải CPU được giám sát liên tục tại bảng điều khiển Quản trị viên.', 'PLATFORM_ADMIN', 195, 1),

-- =========================================================================
-- 10. GENERAL (196 to 205)
-- =========================================================================
(196, 'TCS là gì và sứ mệnh kết nối của nền tảng?', 'Tutor Connect System (TCS) là nền tảng công nghệ giáo dục hàng đầu kết nối học viên, phụ huynh với gia sư chất lượng cao và trung tâm uy tín tại Việt Nam.', 'GENERAL', 196, 1),
(197, 'Hệ thống TCS hỗ trợ những môn học và cấp học nào?', 'TCS hỗ trợ đầy đủ các môn từ Lớp 1 đến Lớp 12, Luyện thi Chuyên, Luyện thi Đại học, Ngoại ngữ (IELTS/TOEIC/Tiếng Nhật/Hàn/Trung), Lập trình và Năng khiếu.', 'GENERAL', 197, 1),
(198, 'Trung tâm trợ giúp và các kênh liên hệ hỗ trợ TCS?', 'Bạn có thể tìm câu trả lời tại /help, tạo phiếu hỗ trợ tại mục Hỗ trợ & Khiếu nại, gửi email về support@tcs.edu.vn hoặc gọi hotline 1900-XXXX.', 'GENERAL', 198, 1),
(199, 'Tại sao nên chọn thuê gia sư qua nền tảng TCS?', 'TCS mang lại 3 giá trị vượt trội: (1) 100% gia sư được xác minh bằng cấp/CCCD; (2) Học phí được bảo vệ an toàn qua Escrow; (3) Hợp đồng điện tử minh bạch.', 'GENERAL', 199, 1),
(200, 'Trợ lý AI của TCS có thể giúp gì cho người dùng?', 'AI Assistant của TCS hỗ trợ gợi ý gia sư phù hợp, tìm kiếm lớp học, giải đáp bài tập Toán/Anh, hướng dẫn quy trình hệ thống và phục vụ 24/7.', 'GENERAL', 200, 1),
(201, 'Ứng dụng TCS có dùng được trên điện thoại không?', 'Giao diện web của TCS được thiết kế chuẩn Responsive, hoạt động mượt mà trên tất cả các trình duyệt di động iOS, Android và máy tính bảng.', 'GENERAL', 201, 1),
(202, 'Chính sách bảo mật quyền riêng tư của TCS thế nào?', 'TCS cam kết tuân thủ nghiêm ngặt các quy định về bảo vệ dữ liệu cá nhân theo Nghị định 13/2023/NĐ-CP của Chính phủ.', 'GENERAL', 202, 1),
(203, 'Làm thế nào để đóng góp ý kiến cải tiến tính năng?', 'Bạn có thể gửi phản hồi và đề xuất cải tiến tại mục "Góp ý sản phẩm" trong trang /support/tickets để nhận quà tri ân từ TCS.', 'GENERAL', 203, 1),
(204, 'TCS có cung cấp tài liệu ôn tập và đề thi thử miễn phí không?', 'Có, học viên và gia sư có thể truy cập kho tài nguyên học tập phong phú hoàn toàn miễn phí tại mục Thư viện đề thi.', 'GENERAL', 204, 1),
(205, 'Thời gian làm việc của đội ngũ hỗ trợ khách hàng TCS?', 'Bộ phận Hỗ trợ khách hàng TCS làm việc từ 08:00 đến 22:00 tất cả các ngày trong tuần (kể cả Thứ Bảy, Chủ Nhật và ngày Lễ).', 'GENERAL', 205, 1),

-- =========================================================================
-- 11. EXPANDED FAQS (206 to 260)
-- =========================================================================
(206, 'Học phí gia sư dạy kèm chương trình Quốc tế (IB, AP, IGCSE, Cambridge) dao động bao nhiêu?', 'Học phí dạy kèm chương trình Quốc tế thường dao động từ 350.000 ₫ – 700.000 ₫/buổi (90–120 phút), tùy thuộc vào cấp độ môn học (SL/HL) và gia sư là sinh viên trường quốc tế hay giáo viên có chứng chỉ sư phạm quốc tế.', 'MARKETPLACE', 206, 1),
(207, 'Học phí gia sư luyện thi chứng chỉ SAT, ACT, GMAT là bao nhiêu?', 'Học phí luyện thi chứng chỉ du học chuẩn hóa (SAT, ACT, GMAT, GRE) dao động từ 350.000 ₫ – 600.000 ₫/buổi với các gia sư đạt điểm SAT >= 1500 hoặc có kinh nghiệm du học.', 'MARKETPLACE', 207, 1),
(208, 'Học phí dạy kèm tiếng Nhật (N5-N1), tiếng Hàn (TOPIK), tiếng Trung (HSK) khoảng bao nhiêu?', 'Học phí ngoại ngữ châu Á dao động từ 200.000 ₫ – 350.000 ₫/buổi cho các cấp độ sơ cấp (N5/TOPIK 1-2/HSK 1-3) và từ 350.000 ₫ – 500.000 ₫/buổi cho luyện thi trung - cao cấp.', 'MARKETPLACE', 208, 1),
(209, 'Tìm gia sư dạy kèm tiền tiểu học (hành trang vào lớp 1, tập đọc, tập viết, làm quen số)?', 'Bạn vào mục /tim-gia-su, chọn khối lớp "Tiền tiểu học (Hành trang lớp 1)" để tìm các gia sư Sư phạm Giáo dục Tiểu học có chuyên môn rèn chữ, dạy phát âm chuẩn và toán tư duy mầm non.', 'MARKETPLACE', 209, 1),
(210, 'Thuê gia sư theo gói dài hạn 3 tháng hoặc 6 tháng có được ưu đãi gì không?', 'Học viên ký hợp đồng dài hạn có thể thương lượng mức học phí trọn gói ưu đãi hơn từ 5% – 15% với gia sư và được hỗ trợ đổi gia sư miễn phí nếu không phù hợp.', 'MARKETPLACE', 210, 1),
(211, 'Thuê gia sư học nhóm (3-5 bạn) thì cách tính học phí cho từng học viên thế nào?', 'Lớp học nhóm chia sẻ chi phí giúp mỗi học viên tiết kiệm từ 40% – 60% học phí so với học 1-1, trong khi gia sư vẫn nhận được tổng thù lao buổi dạy cao hơn.', 'MARKETPLACE', 211, 1),
(212, 'Gia sư có được nhận dạy kèm cho học sinh là người thân trong gia đình trên TCS không?', 'Có, gia sư hoàn toàn có thể tạo lớp và nhận dạy cho người quen trên TCS để được bảo vệ quyền lợi tài chính qua quỹ ký quỹ Escrow và lưu trữ nhật ký học tập.', 'MARKETPLACE', 212, 1),
(213, 'Sau khi đăng lớp, phụ huynh có thể sửa đổi mức học phí đề xuất hoặc địa chỉ học không?', 'Có, phụ huynh có thể vào mục Quản lý lớp học, chọn lớp đang OPEN và bấm "Chỉnh sửa bài đăng" để cập nhật lại thông tin trước khi chấp nhận gia sư.', 'MARKETPLACE', 213, 1),
(214, 'Lớp học cấp tốc 1 tháng ôn thi học kỳ / thi chuyển cấp hoạt động như thế nào?', 'Phụ huynh tạo lớp học với tần suất 4–6 buổi/tuần tại mục /tao-lop, ghi rõ mục tiêu "Ôn thi cấp tốc 1 tháng" để hệ thống ưu tiên kết nối với các gia sư có sẵn lịch trống.', 'MARKETPLACE', 214, 1),
(215, 'Xử lý thế nào khi xảy ra sự cố mất điện hoặc mất kết nối Internet giữa buổi học online?', 'Hai bên chụp ảnh màn hình thông báo sự cố, trao đổi qua tin nhắn và ghi nhận thời lượng đã học. Buổi học có thể được dạy bù phần thời gian còn lại vào buổi tiếp theo.', 'TUTOR_OPS', 215, 1),
(216, 'Học sinh muốn xin nghỉ một buổi học thì cần thông báo cho gia sư trước bao lâu?', 'Học sinh/phụ huynh cần thông báo cho gia sư trước ít nhất 4 giờ. Nếu báo nghỉ sát giờ dưới 2 giờ mà không có lý do bất khả kháng, buổi học vẫn có thể bị tính 50% học phí theo hợp đồng.', 'TUTOR_OPS', 216, 1),
(217, 'Thời hạn để tổ chức buổi học bù sau khi gia sư hoặc học sinh xin dời lịch là bao lâu?', 'Buổi học bù cần được sắp xếp và hoàn thành trong vòng 14 ngày kể từ ngày xin dời lịch để đảm bảo tiến độ và khối lượng kiến thức của khóa học.', 'TUTOR_OPS', 217, 1),
(218, 'Gia sư xin nghỉ quá 3 buổi trong 1 tháng thì có bị xử lý vi phạm không?', 'Gia sư tự ý nghỉ quá 3 buổi/tháng không có sự đồng ý của phụ huynh sẽ bị trừ 30 điểm uy tín và phụ huynh có quyền yêu cầu đơn phương chấm dứt hợp đồng hoàn tiền cọc.', 'TUTOR_OPS', 218, 1),
(219, 'Lớp học nhóm nếu có 1 bạn học sinh vắng mặt thì tính điểm danh buổi đó ra sao?', 'Gia sư vẫn tiến hành giảng dạy bình thường cho các học sinh có mặt và ghi nhận học sinh vắng mặt. Học sinh vắng có thể xem lại tài liệu bài giảng được gia sư tải lên sau buổi học.', 'TUTOR_OPS', 219, 1),
(220, 'Gia sư có thể chia sẻ đề cương ôn thi và bài tập về nhà trực tiếp qua cửa sổ chat không?', 'Có, cửa sổ chat trên TCS hỗ trợ gửi tệp đính kèm tài liệu học tập định dạng PDF, Word, hình ảnh dung lượng tối đa 25MB cho mỗi tệp.', 'TUTOR_OPS', 220, 1),
(221, 'Phụ huynh có thể phản hồi nhận xét trực tiếp dưới phiếu điểm danh của gia sư không?', 'Có, trong chi tiết buổi học tại /parent/classes, phụ huynh có thể bấm "Góp ý buổi học" để gửi phản hồi trực tiếp cho gia sư về chất lượng bài giảng.', 'TUTOR_OPS', 221, 1),
(222, 'Quy định thời lượng tối thiểu của một buổi học gia sư tiêu chuẩn trên TCS là bao lâu?', 'Thời lượng tiêu chuẩn là 90 phút (1.5 giờ) đối với học sinh Tiểu học và 120 phút (2.0 giờ) đối với học sinh THCS, THPT và luyện thi chứng chỉ.', 'TUTOR_OPS', 222, 1),
(223, 'Trung tâm gia sư trả lương cho giáo viên trực thuộc vào ngày nào trong tháng?', 'Thời gian thanh toán thù lao cho giáo viên trực thuộc do Trung tâm quy định trong hợp đồng nội bộ, thông thường từ ngày 05 đến ngày 10 của tháng kế tiếp.', 'CENTER_OPS', 223, 1),
(224, 'Trung tâm có thể tạo bài kiểm tra năng lực đầu vào (Placement Test) cho học sinh trên TCS không?', 'Có, trung tâm có thể đính kèm đề kiểm tra đánh giá năng lực trong phần mô tả tuyển sinh lớp nhóm tại trang /center để phân loại trình độ học viên.', 'CENTER_OPS', 224, 1),
(225, 'Hợp đồng hợp tác giữa Trung tâm và Gia sư trực thuộc có điểm gì khác với gia sư tự do?', 'Hợp đồng trung tâm có thêm các điều khoản về: Tỷ lệ phân chia hoa hồng, cam kết giờ dạy tối thiểu/tháng, bảo mật giáo trình và quyền đại diện của trung tâm.', 'CENTER_OPS', 225, 1),
(226, 'Trung tâm có thể theo dõi tỷ lệ đi dạy và chuyên cần của từng giáo viên ở đâu?', 'Tại bảng điều khiển /center, quản trị viên chọn tab "Báo cáo giảng dạy" để xem biểu đồ tỷ lệ điểm danh đúng giờ, số ca dạy hoàn thành và phản hồi học viên.', 'CENTER_OPS', 226, 1),
(227, 'Trung tâm có thể phát hành mã giảm giá học phí (Voucher/Coupon) cho lớp nhóm không?', 'Có, tính năng Khuyến mãi tại /center cho phép trung tâm tạo mã giảm giá theo phần trăm hoặc số tiền cố định để thu hút học viên đăng ký lớp nhóm.', 'CENTER_OPS', 227, 1),
(228, 'Quy trình chuyển nhượng quyền quản trị hoặc thay đổi người đại diện pháp luật của Trung tâm?', 'Trung tâm gửi giấy tờ đăng ký kinh doanh điều chỉnh mới nhất tại /support/tickets để Ban Pháp chế TCS đối soát và cập nhật thông tin trong 24 giờ.', 'CENTER_OPS', 228, 1),
(229, 'Hạn mức nạp tiền tối thiểu và tối đa qua mã VietQR SePay là bao nhiêu?', 'Hạn mức nạp tiền tối thiểu là 10.000 ₫/lần và tối đa là 50.000.000 ₫/giao dịch. Số lần nạp tiền trong ngày không bị giới hạn.', 'FINANCE_ESCROW', 229, 1),
(230, 'Nếu quét mã VietQR SePay nhưng quên nhập hoặc nhập sai nội dung chuyển khoản thì làm sao?', 'Bạn vào mục /support/tickets, chọn "Sự cố nạp tiền", đính kèm ảnh biên lai ngân hàng và mã giao dịch. Kế toán TCS sẽ đối soát và ghi nhận số dư vào ví trong 15 phút.', 'FINANCE_ESCROW', 230, 1),
(231, 'Sau buổi học cuối cùng, nếu phụ huynh không bấm xác nhận thì sau bao lâu tiền Escrow tự động giải ngân?', 'Nếu không có tranh chấp hoặc khiếu nại phát sinh, hệ thống sẽ tự động giải ngân 100% học phí Escrow cho gia sư sau 72 giờ kể từ khi gia sư điểm danh buổi cuối.', 'FINANCE_ESCROW', 231, 1),
(232, 'Lệnh rút tiền tạo vào tối muộn hoặc ngày nghỉ lễ cuối tuần có được xử lý không?', 'Lệnh rút tiền qua hệ thống Napas 247 được đối soát và giải ngân tự động 24/7 (kể cả ban đêm, Thứ Bảy, Chủ Nhật và ngày Lễ).', 'FINANCE_ESCROW', 232, 1),
(233, 'Một tài khoản người dùng có thể liên kết tối đa bao nhiêu tài khoản ngân hàng để rút tiền?', 'Người dùng có thể lưu tối đa 3 tài khoản ngân hàng chính chủ tại mục /finance để thuận tiện lựa chọn khi tạo lệnh rút tiền.', 'FINANCE_ESCROW', 233, 1),
(234, 'Tiền hoàn cọc (Refund) khi kết thúc lớp sớm có bị trừ phí nền tảng không?', 'Không, phần học phí hoàn trả từ các buổi chưa học trong quỹ Escrow sẽ được hoàn trả nguyên vẹn 100% về ví phụ huynh mà không bị trừ phí sàn.', 'FINANCE_ESCROW', 234, 1),
(235, 'Thu nhập của gia sư trên nền tảng TCS có phải chịu thuế thu nhập cá nhân (TNCN) không?', 'Gia sư có tổng thu nhập chịu thuế theo quy định pháp luật sẽ tự kê khai hoặc được TCS hỗ trợ trích xuất chứng từ khấu trừ thuế TNCN khi có yêu cầu.', 'FINANCE_ESCROW', 235, 1),
(236, 'Phụ huynh có thể nạp tiền vào ví trước để giữ chỗ nhiều lớp học cùng lúc không?', 'Có, số dư khả dụng trong ví TCS có thể dùng để thanh toán cọc Escrow cho nhiều lớp học khác nhau của các con mà không cần chuyển khoản nhiều lần.', 'FINANCE_ESCROW', 236, 1),
(237, 'Căn cứ pháp lý nào quy định giá trị của Hợp đồng điện tử xác thực qua OTP trên TCS?', 'Hợp đồng điện tử trên TCS được xây dựng tuân thủ theo Luật Giao dịch điện tử số 20/2023/QH15 và Bộ luật Dân sự 2015, có đầy đủ giá trị chứng cứ pháp lý.', 'CONTRACT_REVIEW', 237, 1),
(238, 'Sau khi phụ huynh bấm chấp nhận ứng viên, ai là người khởi tạo bản hợp đồng đầu tiên?', 'Hệ thống TCS tự động sinh dự thảo Hợp đồng điện tử dựa trên thông tin bài đăng lớp học và các điều khoản thù lao mà hai bên đã thống nhất.', 'CONTRACT_REVIEW', 238, 1),
(239, 'Nếu gia sư hoặc phụ huynh không ký mã OTP trong vòng 48h thì hợp đồng xử lý thế nào?', 'Sau 48 giờ kể từ khi tạo hợp đồng nếu một trong hai bên chưa hoàn tất ký OTP, yêu cầu hợp đồng sẽ tự động hết hiệu lực và lớp học quay lại trạng thái OPEN.', 'CONTRACT_REVIEW', 239, 1),
(240, 'Ký phụ lục hợp đồng gia hạn thêm buổi học có yêu cầu xác thực lại mã OTP không?', 'Có, mỗi phụ lục gia hạn thời gian hoặc thay đổi mức phí đều yêu cầu hai bên nhập mã xác thực OTP gửi về số điện thoại/email để đảm bảo tính pháp lý.', 'CONTRACT_REVIEW', 240, 1),
(241, 'Gia sư bị trừ điểm uy tín xuống dưới 50 điểm thì sẽ bị những hạn chế gì?', 'Gia sư có điểm uy tín < 50 điểm sẽ bị ẩn khỏi trang tìm kiếm công khai, bị giới hạn chỉ được nhận tối đa 1 lớp học và phải qua xét duyệt thủ công của Admin.', 'CONTRACT_REVIEW', 241, 1),
(242, 'Làm sao để gia sư phục hồi lại điểm uy tín sau khi đã bị trừ điểm?', 'Gia sư có thể phục hồi điểm bằng cách hoàn thành các lớp học tiếp theo đúng cam kết (+5 điểm/lớp), nhận đánh giá 5 sao (+2 điểm) và không vi phạm quy chế trong 3 tháng.', 'CONTRACT_REVIEW', 242, 1),
(243, 'Phụ huynh có thể đánh giá gia sư theo từng tiêu chí (kiến thức, đúng giờ, sư phạm) không?', 'Có, biểu mẫu đánh giá sau khóa học cho phép chấm điểm chi tiết theo 3 tiêu chí: (1) Trình độ chuyên môn; (2) Kỹ năng sư phạm; (3) Tác phong đúng giờ và nhiệt tình.', 'CONTRACT_REVIEW', 243, 1),
(244, 'Hệ thống tự động cảnh báo như thế nào khi người dùng nhắn tin trao đổi thông tin liên lạc ngoài sàn?', 'Bộ lọc AI sẽ tự động che các chuỗi số điện thoại, số tài khoản hoặc từ khóa nhạy cảm và gửi thông báo cảnh báo bảo vệ an toàn cho cả hai bên trong hộp chat.', 'TRUST_SAFETY', 244, 1),
(245, 'Những loại bằng chứng nào được Hội đồng Hòa giải TCS chấp nhận khi xử lý tranh chấp?', 'Chấp nhận: Ảnh chụp màn hình tin nhắn, biên bản điểm danh, bản ghi âm cuộc gọi, bài tập của học sinh và video trích xuất từ camera buổi học.', 'TRUST_SAFETY', 245, 1),
(246, 'Gia sư đến muộn quá 30 phút mà không báo trước thì phụ huynh có quyền xử lý thế nào?', 'Phụ huynh có quyền hủy buổi học đó, yêu cầu gia sư dạy bù không tính phí hoặc ghi nhận vi phạm để làm căn cứ chấm dứt hợp đồng nếu tái diễn.', 'TRUST_SAFETY', 246, 1),
(247, 'Nền tảng TCS có chính sách bảo vệ an toàn đặc biệt nào cho học sinh dưới 16 tuổi?', 'Tất cả lớp học tại nhà dành cho học sinh dưới 16 tuổi bắt buộc phải có sự giám sát của phụ huynh/người giám hộ và gia sư phải có lý lịch tư pháp rõ ràng.', 'TRUST_SAFETY', 247, 1),
(248, 'Chính sách khen thưởng cho người dùng phát hiện lỗi bảo mật hệ thống (Bug Bounty Program)?', 'TCS trao thưởng từ 500.000 ₫ đến 5.000.000 ₫ cùng thư tri ân cho các thành viên phát hiện và gửi báo cáo lỗ hổng kỹ thuật có tính xây dựng.', 'TRUST_SAFETY', 248, 1),
(249, 'Gia sư có được yêu cầu phụ huynh chụp ảnh bài kiểm tra ở trường của học sinh gửi qua hệ thống không?', 'Có, việc gửi đề kiểm tra và bài thi định kỳ giúp gia sư đánh giá chính xác học lực hiện tại để điều chỉnh giáo án bồi dưỡng sát với chương trình trên lớp.', 'TRUST_SAFETY', 249, 1),
(250, 'Tài khoản bị kẻ xấu đánh cắp hoặc đổi mật khẩu thì quy trình hỗ trợ khẩn cấp ra sao?', 'Liên hệ ngay hotline 1900-XXXX hoặc gửi email khẩn cấp tới security@tcs.edu.vn kèm ảnh chụp CCCD để kỹ thuật viên khóa tạm thời và cấp lại quyền truy cập.', 'TRUST_SAFETY', 250, 1),
(251, 'Trợ lý AI có thể phân tích tiến độ học tập và gợi ý bài tập nâng cao cho học sinh không?', 'Có, AI Assistant dựa trên nhật ký điểm danh và nhận xét của gia sư để tổng hợp biểu đồ tiến độ học tập và đề xuất các chủ đề kiến thức cần củng cố thêm.', 'GENERAL', 251, 1),
(252, 'Trợ lý AI có thể tìm gia sư theo khung thời gian rảnh cụ thể trong tuần không?', 'Có, bạn chỉ cần nhắn tin cho AI (ví dụ: "Tìm gia sư Toán lớp 9 rảnh tối thứ 3 và tối thứ 7 tại Cầu Giấy"), AI sẽ tự động đối soát lịch rảnh và lọc gia sư phù hợp.', 'GENERAL', 252, 1),
(253, 'Trợ lý AI có hỗ trợ giải đáp bài tập Toán, Lý, Hóa hoặc ngữ pháp Tiếng Anh không?', 'Có, Trợ lý AI được tích hợp mô hình sư phạm chuyên sâu, có khả năng hướng dẫn phương pháp giải từng bước cho các bài tập từ Lớp 1 đến Lớp 12.', 'GENERAL', 253, 1),
(254, 'AI Assistant phân tích và tóm tắt đánh giá của các phụ huynh trước về gia sư như thế nào?', 'AI tự động tổng hợp hàng trăm nhận xét thực tế để đưa ra bản tóm tắt điểm mạnh nổi bật (ví dụ: "Rất kiên nhẫn, chuyên luyện thi chuyên Toán, đúng giờ 100%").', 'GENERAL', 254, 1),
(255, 'Dữ liệu trò chuyện với AI Assistant có được bảo mật và xóa định kỳ không?', 'Toàn bộ nội dung hội thoại được mã hóa đầu cuối. Bạn có thể xóa lịch sử chat bất kỳ lúc nào bằng nút "Xóa cuộc trò chuyện" trên giao diện chat AI.', 'GENERAL', 255, 1),
(256, 'Làm thế nào để phản hồi câu trả lời của AI Assistant chưa chính xác để cải thiện hệ thống?', 'Dưới mỗi câu trả lời của AI có nút Thích (Thumbs up) và Không thích (Thumbs down). Bấm "Không thích" để gửi góp ý trực tiếp cho đội ngũ kỹ sư AI của TCS.', 'GENERAL', 256, 1),
(257, 'Học viên khiếm thị hoặc người khuyết tật có thể sử dụng Trợ lý AI bằng giọng nói không?', 'Giao diện web của TCS tương thích tốt với các công cụ đọc màn hình tiêu chuẩn (Screen Reader) và đang thử nghiệm tính năng nhận diện giọng nói Voice-to-Text.', 'GENERAL', 257, 1),
(258, 'AI Assistant có thể cảnh báo phụ huynh khi mức học phí gia sư đề xuất cao bất thường không?', 'Có, AI sẽ so sánh mức học phí đề xuất với mặt bằng chung thị trường tại cùng khu vực/môn học và đưa ra khuyến nghị mức giá hợp lý cho phụ huynh.', 'GENERAL', 258, 1),
(259, 'Phụ huynh có thể hỏi AI về lộ trình ôn thi vào lớp 10 các trường chuyên tại Hà Nội/TP.HCM không?', 'Có, AI Assistant có cơ sở dữ liệu về cấu trúc đề thi, điểm chuẩn 3 năm gần nhất của các trường chuyên (Ams, Chuyên Sư Phạm, Chuyên KHTN, Lê Hồng Phong...).', 'GENERAL', 259, 1),
(260, 'Làm sao để bật/tắt gợi ý thông minh từ Trợ lý AI trong quá trình tìm kiếm gia sư?', 'Tại mục Cài đặt tài khoản /profile, bạn có thể tùy chọn Bật hoặc Tắt tính năng "Gợi ý thông minh từ AI" theo nhu cầu cá nhân.', 'GENERAL', 260, 1);


-- --------------------------------------------------------------------
-- 8. VERIFICATION REQUESTS
-- --------------------------------------------------------------------
INSERT INTO verification_requests (user_id, verification_type, status, admin_notes, submitted_at, created_at, updated_at)
SELECT user_id, 'TUTOR_PROFILE', 'SUBMITTED', 'Gia sư gửi hồ sơ xác minh bằng tốt nghiệp ĐH Sư Phạm Hà Nội', DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW(), NOW()
FROM users WHERE email = 'tutor.le@gmail.com'
ON DUPLICATE KEY UPDATE status = 'SUBMITTED';

INSERT INTO verification_requests (user_id, verification_type, status, admin_notes, submitted_at, created_at, updated_at)
SELECT user_id, 'TUTOR_CENTER_LICENSE', 'SUBMITTED', 'Trung tâm nộp giấy phép đăng ký kinh doanh 2026', DATE_SUB(NOW(), INTERVAL 4 HOUR), NOW(), NOW()
FROM users WHERE email = 'center.triangviet@gmail.com'
ON DUPLICATE KEY UPDATE status = 'SUBMITTED';

-- --------------------------------------------------------------------
-- 9. SUPPORT TICKETS (Includes SLA breached ticket)
-- --------------------------------------------------------------------
INSERT INTO support_tickets (user_id, category, subject, description, priority, status, due_at, sla_breached, created_at)
SELECT user_id, 'DISPUTE', 'Lỗi thanh toán VNPay qua cổng quét QR', 'Sau khi quét mã QR thanh toán thành công, hệ thống chưa cộng tiền vào ví ký quỹ.', 'URGENT', 'OPEN', DATE_SUB(NOW(), INTERVAL 2 HOUR), 1, DATE_SUB(NOW(), INTERVAL 14 HOUR)
FROM users WHERE email = 'haehuynh35@gmail.com' LIMIT 1
ON DUPLICATE KEY UPDATE status = 'OPEN';

INSERT INTO support_tickets (user_id, category, subject, description, priority, status, due_at, sla_breached, created_at)
SELECT user_id, 'INQUIRY', 'Yêu cầu kiểm duyệt bổ sung chứng chỉ IELTS', 'Tôi đã tải lên bằng IELTS mới 8.5, nhờ Admin kiểm duyệt lại hồ sơ gia sư.', 'HIGH', 'IN_PROGRESS', DATE_ADD(NOW(), INTERVAL 8 HOUR), 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)
FROM users WHERE email = 'minhduc101dz@gmail.com' LIMIT 1
ON DUPLICATE KEY UPDATE status = 'IN_PROGRESS';

INSERT INTO support_tickets (user_id, category, subject, description, priority, status, due_at, sla_breached, created_at)
SELECT user_id, 'BUG_REPORT', 'Báo cáo vi phạm thái độ gia sư', 'Gia sư nghỉ học không báo trước 3 buổi liên tiếp.', 'URGENT', 'OPEN', DATE_ADD(NOW(), INTERVAL 4 HOUR), 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)
FROM users WHERE email = 'parent.tran@gmail.com' LIMIT 1
ON DUPLICATE KEY UPDATE status = 'OPEN';

-- --------------------------------------------------------------------
-- 10. TUTORING CLASSES, APPLICATIONS & ASSIGNMENTS
-- --------------------------------------------------------------------
SET @client_user_id = (SELECT user_id FROM users WHERE email = 'haehuynh35@gmail.com' LIMIT 1);
SET @tutor_user_id = (SELECT user_id FROM users WHERE email = 'minhduc101dz@gmail.com' LIMIT 1);
SET @tutor_profile_id = (SELECT tutor_id FROM tutors WHERE user_id = @tutor_user_id LIMIT 1);

INSERT INTO tutoring_classes (
    creator_id, class_type, learning_goal, tutor_requirement, title, description,
    lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, budget, recurring_type, status, created_at, updated_at
)
VALUES (
    @client_user_id, 'PRIVATE', 'Luyện thi Đại học khối A môn Toán', 'Gia sư có kinh nghiệm dạy Toán 12 Cầu Giấy',
    'Lớp Toán 12 Luyện thi Đại học Cầu Giấy', 'Cần tìm gia sư dạy kèm 2 buổi/tuần tại nhà.',
    'OFFLINE', 10, 240000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY),
    2400000.00, 'WEEKLY', 'OPEN', NOW(), NOW()
)
ON DUPLICATE KEY UPDATE title = 'Lớp Toán 12 Luyện thi Đại học Cầu Giấy';

SET @demo_class_id = (SELECT class_id FROM tutoring_classes WHERE title = 'Lớp Toán 12 Luyện thi Đại học Cầu Giấy' LIMIT 1);

INSERT INTO tutor_applications (class_id, tutor_id, proposed_rate, cover_letter, status, applied_at)
VALUES (@demo_class_id, @tutor_profile_id, 240000.00, 'Tôi có 5 năm kinh nghiệm luyện thi ĐH Toán khối A.', 'ACCEPTED', NOW())
ON DUPLICATE KEY UPDATE status = 'ACCEPTED';

SET @demo_app_id = (SELECT application_id FROM tutor_applications WHERE class_id = @demo_class_id AND tutor_id = @tutor_profile_id LIMIT 1);

INSERT INTO class_assignments (tutor_id, application_id, assigned_date, status)
VALUES (@tutor_profile_id, @demo_app_id, NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

SET @demo_assignment_id = (SELECT assignment_id FROM class_assignments WHERE application_id = @demo_app_id LIMIT 1);

-- --------------------------------------------------------------------
-- 11. FINANCIAL TRANSACTIONS & ESCROW
-- --------------------------------------------------------------------
INSERT INTO payment_transactions (wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (@client_user_id, 'DEP-VNPAY-001', 'DEPOSIT', 'SUCCESS', 5000000.00, 'Nạp tiền vào ví qua cổng VNPAY', 'DEP-VNPAY-001', NOW(), DATE_SUB(NOW(), INTERVAL 15 DAY))
ON DUPLICATE KEY UPDATE status = 'SUCCESS';

SET @dep_tx_id = (SELECT transaction_id FROM payment_transactions WHERE reference_code = 'DEP-VNPAY-001' LIMIT 1);

INSERT INTO payment_transactions (wallet_id, external_transaction_id, type, status, amount, description, reference_code, processed_at, created_at)
VALUES (@client_user_id, 'ESC-DEP-001', 'ESCROW_DEPOSIT', 'SUCCESS', 2400000.00, 'Đặt cọc Escrow Lớp Toán 12 Minh Đức', 'ESC-DEP-001', NOW(), DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE status = 'SUCCESS';

SET @escrow_tx_id = (SELECT transaction_id FROM payment_transactions WHERE reference_code = 'ESC-DEP-001' LIMIT 1);

INSERT INTO escrow_transactions (payment_id, assignment_id, amount, status, deposited_at, created_at)
VALUES (@escrow_tx_id, @demo_assignment_id, 2400000.00, 'DISPUTED', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE status = 'DISPUTED';

SET @demo_escrow_id = (SELECT escrow_id FROM escrow_transactions WHERE payment_id = @escrow_tx_id LIMIT 1);

-- --------------------------------------------------------------------
-- 12. REPORTS (Circumvention & Abuse)
-- --------------------------------------------------------------------
INSERT INTO reports (reporter_id, target_type, target_id, category, description, status, created_at)
VALUES (@client_user_id, 'USER', @tutor_user_id, 'FRAUD', 'Gia sư đề nghị chuyển khoản ngoài để né tránh phí dịch vụ nền tảng 2%', 'PENDING', DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE status = 'PENDING';

SET @demo_report_id = (SELECT report_id FROM reports WHERE reporter_id = @client_user_id AND target_id = @tutor_user_id LIMIT 1);

-- --------------------------------------------------------------------
-- 13. DISPUTES & REFUND REQUESTS
-- --------------------------------------------------------------------
INSERT INTO disputes (report_id, escrow_id, status, created_at)
VALUES (@demo_report_id, @demo_escrow_id, 'OPEN', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = 'OPEN';

INSERT INTO refund_requests (escrow_id, requested_by, reason, amount, status, requested_at)
VALUES (@demo_escrow_id, @client_user_id, 'Lớp học bị gián đoạn do sự cố phát sinh từ phía gia sư', 1200000.00, 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = 'PENDING';

-- --------------------------------------------------------------------
-- 14. PAYMENT METHODS & WITHDRAWAL REQUESTS
-- --------------------------------------------------------------------
INSERT INTO payment_methods (wallet_id, type, account_no, bank_name, status)
VALUES (@tutor_user_id, 'BANK_TRANSFER', '0987654321', 'MBBank', 'ACTIVE')
ON DUPLICATE KEY UPDATE status = 'ACTIVE';

SET @tutor_pm_id = (SELECT payment_method_id FROM payment_methods WHERE wallet_id = @tutor_user_id LIMIT 1);

INSERT INTO withdrawal_requests (wallet_id, payment_method_id, amount, status, requested_at)
VALUES (@tutor_user_id, @tutor_pm_id, 1000000.00, 'PENDING', DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE status = 'PENDING';
