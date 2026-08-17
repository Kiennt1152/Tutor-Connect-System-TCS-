-- =====================================================================
-- MASTER SEED SCRIPT FOR TCS (TUTOR CONNECT SYSTEM)
-- Bao gồm:
--   1. Đầy đủ danh mục Môn học & Khối lớp
--   2. 205 Câu hỏi thường gặp (FAQ Entries) theo 10 chuyên mục chuẩn
--   3. 20 Hồ sơ Gia sư thực chiến đầy đủ các môn (Toán, Lý, Hóa, Văn, Anh, Hàn, Trung, Tin...)
--   4. 30 Lớp học đang mở (OPEN) trải dài khắp các tỉnh thành
--   5. Tài khoản Quản trị viên (Admin) & Tài khoản kiểm thử Onboarding
--
-- Cách dùng:
--   docker compose exec -T mysql mysql -u root -p12345 tutorconnectsystem < scripts/seed-master-data.sql
-- =====================================================================

USE tutorconnectsystem;
SET NAMES 'utf8mb4';
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- =====================================================================
-- PHẦN 1: MÔN HỌC & KHỐI LỚP
-- =====================================================================
INSERT IGNORE INTO subjects (subject_name, description) VALUES
    ('Toán', 'Môn Toán các cấp, luyện thi vào 10 và THPT Quốc gia'),
    ('Vật lý', 'Môn Vật lý THCS, THPT và luyện thi Đại học'),
    ('Hóa học', 'Môn Hóa học THCS, THPT và bồi dưỡng HSG'),
    ('Sinh học', 'Môn Sinh học ôn thi THPTQG và khối B Y Dược'),
    ('Ngữ văn', 'Môn Ngữ văn, rèn kỹ năng viết và cảm thụ văn học'),
    ('Tiếng Việt', 'Môn Tiếng Việt tiểu học, rèn chữ đẹp, tập đọc'),
    ('Tiếng Anh', 'Tiếng Anh phổ thông, giao tiếp và chứng chỉ quốc tế'),
    ('Tiếng Hàn', 'Tiếng Hàn giao tiếp, du học và luyện thi TOPIK'),
    ('Tiếng Trung', 'Tiếng Trung giao tiếp thương mại và luyện thi HSK/TOCFL'),
    ('Tin học', 'Tin học văn phòng, lập trình Scratch, Python, C++, Web'),
    ('Lịch sử', 'Môn Lịch sử ôn thi vào 10 và THPTQG'),
    ('Địa lý', 'Môn Địa lý ôn thi vào 10 và THPTQG'),
    ('Thi chứng chỉ (IELTS, TOEIC...)', 'Luyện thi IELTS, TOEIC, TOEFL quốc tế');

INSERT IGNORE INTO grades (grade_name) VALUES
    ('Lớp 1'), ('Lớp 2'), ('Lớp 3'), ('Lớp 4'), ('Lớp 5'), ('Lớp 6'),
    ('Lớp 7'), ('Lớp 8'), ('Lớp 9'), ('Lớp 10'), ('Lớp 11'), ('Lớp 12'),
    ('Luyện thi chứng chỉ (IELTS, TOEIC...)'),
    ('Luyện thi Đại học');

-- =====================================================================
-- PHẦN 2: 205 CÂU HỎI THƯỜNG GẶP FAQ (CHO VECTOR CHATBOT AI)
-- =====================================================================
DELETE FROM faq_entries WHERE faq_id > 0;

INSERT INTO faq_entries (faq_id, question, answer, category, sort_order, is_published, created_at, updated_at) VALUES 
-- 1. AUTH_PROFILE (1 to 20)
(1, 'Làm sao để đăng ký tài khoản trên TCS?', 'Bạn bấm vào nút "Đăng ký" tại góc trên bên phải màn hình hoặc truy cập /register, chọn vai trò phù hợp (Học viên/Phụ huynh, Gia sư, hoặc Trung tâm gia sư), điền số điện thoại, email và mật khẩu để tạo tài khoản.', 'AUTH_PROFILE', 1, 1, NOW(), NOW()),
(2, 'Làm sao đăng nhập tài khoản TCS?', 'Truy cập trang /login, nhập email hoặc số điện thoại kèm mật khẩu đã đăng ký. Bạn cũng có thể đăng nhập nhanh qua Google hoặc yêu cầu mã đăng nhập OTP.', 'AUTH_PROFILE', 2, 1, NOW(), NOW()),
(3, 'Tôi quên mật khẩu thì lấy lại thế nào?', 'Tại màn hình đăng nhập /login, bạn nhấn vào liên kết "Quên mật khẩu?" hoặc truy cập /forgot-password, nhập email đăng ký để nhận liên kết và mã xác thực đặt lại mật khẩu mới.', 'AUTH_PROFILE', 3, 1, NOW(), NOW()),
(4, 'Không nhận được mã OTP xác thực phải làm sao?', 'Vui lòng kiểm tra lại số điện thoại hoặc hộp thư rác (Spam) trong email. Nếu sau 60 giây vẫn chưa nhận được, bạn nhấn nút "Gửi lại mã OTP" trên giao diện.', 'AUTH_PROFILE', 4, 1, NOW(), NOW()),
(5, 'Tài khoản bị khóa vì lý do gì và mở lại ra sao?', 'Tài khoản có thể bị khóa tạm thời nếu nhập sai mật khẩu quá 5 lần liên tiếp hoặc có dấu hiệu vi phạm chính sách sàn. Bạn vui lòng tạo phiếu hỗ trợ tại /support/tickets để được mở khóa.', 'AUTH_PROFILE', 5, 1, NOW(), NOW()),
(6, 'Làm sao để đổi mật khẩu tài khoản?', 'Sau khi đăng nhập, vào mục Cài đặt tài khoản trong Hồ sơ cá nhân (/profile), chọn "Đổi mật khẩu", nhập mật khẩu hiện tại và mật khẩu mới để cập nhật.', 'AUTH_PROFILE', 6, 1, NOW(), NOW()),
(7, 'Cập nhật thông tin hồ sơ cá nhân ở đâu?', 'Bạn truy cập mục Hồ sơ cá nhân tại /profile để chỉnh sửa họ tên, số điện thoại, địa chỉ, ảnh đại diện và thông tin liên hệ.', 'AUTH_PROFILE', 7, 1, NOW(), NOW()),
(8, 'Cách tải ảnh đại diện avatar chất lượng cao?', 'Trong trang Hồ sơ (/profile), nhấn vào biểu tượng máy ảnh trên khung ảnh đại diện, chọn tệp ảnh có định dạng PNG hoặc JPG dung lượng dưới 5MB để tải lên.', 'AUTH_PROFILE', 8, 1, NOW(), NOW()),
(9, 'Quét căn cước công dân CCCD tự động như thế nào?', 'Hệ thống TCS hỗ trợ công nghệ OCR tự động nhận diện thông tin từ ảnh chụp 2 mặt CCCD trong mục xác minh hồ sơ tại /profile giúp bạn không cần nhập liệu thủ công.', 'AUTH_PROFILE', 9, 1, NOW(), NOW()),
(10, 'Tạo hồ sơ con học viên (Child Profile) để làm gì?', 'Phụ huynh có thể tạo nhiều hồ sơ con dưới một tài khoản quản lý tại /profile để theo dõi lộ trình học tập, lịch học và điểm danh riêng biệt cho từng người con.', 'AUTH_PROFILE', 10, 1, NOW(), NOW()),
(11, 'Làm sao liên kết tài khoản Phụ huynh với Học viên?', 'Trong hồ sơ học viên, chọn tính năng "Liên kết Người giám hộ", nhập email hoặc số điện thoại của phụ huynh. Phụ huynh chỉ cần xác nhận qua mã liên kết là hoàn tất.', 'AUTH_PROFILE', 11, 1, NOW(), NOW()),
(12, 'Gia sư cập nhật kinh nghiệm giảng dạy ở đâu?', 'Gia sư vào mục /profile, chọn tab "Kinh nghiệm & Chuyên môn" để thêm quá trình công tác, các trường đại học đã tốt nghiệp và giải thưởng thành tích.', 'AUTH_PROFILE', 12, 1, NOW(), NOW()),
(13, 'Cập nhật lịch rảnh của gia sư thế nào?', 'Gia sư vào mục /profile hoặc /tutor/schedule, tích chọn các khung giờ rảnh trong tuần để phụ huynh dễ dàng đối chiếu và đặt lịch học phù hợp.', 'AUTH_PROFILE', 13, 1, NOW(), NOW()),
(14, 'Viết phần giới thiệu Bio gia sư thế nào để thu hút?', 'Nêu rõ thế mạnh môn học, phương pháp sư phạm, thành tích đào tạo học sinh tiến bộ và thái độ tận tâm để tạo độ tin cậy cao với phụ huynh.', 'AUTH_PROFILE', 14, 1, NOW(), NOW()),
(15, 'Một tài khoản có thể vừa làm gia sư vừa làm phụ huynh không?', 'TCS khuyến nghị tạo tài khoản chuyên biệt theo từng vai trò chính để bảo đảm quyền lợi tài chính, tính hợp lệ của hợp đồng và giao diện tối ưu.', 'AUTH_PROFILE', 15, 1, NOW(), NOW()),
(16, 'Làm sao để xóa hoặc đóng tài khoản TCS?', 'Nếu không còn nhu cầu sử dụng, bạn có thể gửi yêu cầu vô hiệu hóa tài khoản trong mục Cài đặt quyền riêng tư hoặc liên hệ /support/tickets.', 'AUTH_PROFILE', 16, 1, NOW(), NOW()),
(17, 'Thông tin cá nhân trên TCS có được bảo mật không?', 'Tất cả dữ liệu cá nhân, số điện thoại và chứng từ CCCD được mã hóa an toàn theo tiêu chuẩn bảo mật và chỉ dùng cho mục đích xác minh danh tính.', 'AUTH_PROFILE', 17, 1, NOW(), NOW()),
(18, 'Phiên đăng nhập hết hạn thì phải làm sao?', 'Hệ thống tự động đăng xuất sau một khoảng thời gian không hoạt động để bảo vệ tài khoản. Bạn chỉ cần đăng nhập lại tại /login để tiếp tục làm việc.', 'AUTH_PROFILE', 18, 1, NOW(), NOW()),
(19, 'Lỗi không có quyền truy cập trang này (Permission Denied)?', 'Lỗi này xuất hiện khi bạn cố truy cập trang dành cho vai trò khác (ví dụ: Học viên truy cập trang quản trị Admin). Hãy đăng nhập bằng tài khoản có vai trò tương ứng.', 'AUTH_PROFILE', 19, 1, NOW(), NOW()),
(20, 'Làm sao thay đổi số điện thoại nhận thông báo?', 'Bạn vào mục /profile, chọn Chỉnh sửa thông tin liên hệ, nhập số điện thoại mới và xác thực bằng mã OTP được gửi về số đó.', 'AUTH_PROFILE', 20, 1, NOW(), NOW()),

-- 2. VERIFICATION (21 to 35)
(21, 'Quy trình xác minh hồ sơ gia sư diễn ra thế nào?', 'Gia sư tải lên ảnh chụp 2 mặt CCCD, bằng cử nhân/thẻ sinh viên và chứng chỉ liên quan tại /profile. Quản trị viên TCS sẽ đối soát và phê duyệt trong vòng 24–48 giờ.', 'VERIFICATION', 21, 1, NOW(), NOW()),
(22, 'Cần những giấy tờ gì để được duyệt hồ sơ gia sư?', 'Bao gồm: (1) Căn cước công dân hoặc Hộ chiếu còn hạn; (2) Thẻ sinh viên hoặc Bằng tốt nghiệp Đại học; (3) Chứng chỉ ngoại ngữ/chuyên môn (nếu có).', 'VERIFICATION', 22, 1, NOW(), NOW()),
(23, 'Vì sao hồ sơ xác minh gia sư bị từ chối?', 'Các lý do phổ biến: Ảnh giấy tờ bị mờ, lóa sáng, mất góc; thông tin họ tên/ngày sinh trên hồ sơ không khớp với CCCD; hoặc văn bằng không đủ tính pháp lý.', 'VERIFICATION', 23, 1, NOW(), NOW()),
(24, 'Thời gian xét duyệt hồ sơ xác minh là bao lâu?', 'Hệ thống xét duyệt trong vòng 24 giờ làm việc. Vào các đợt cao điểm đầu năm học, thời gian tối đa không quá 48 giờ.', 'VERIFICATION', 24, 1, NOW(), NOW()),
(25, 'Kiểm tra trạng thái duyệt hồ sơ ở đâu?', 'Bạn vào mục /profile để xem huy hiệu trạng thái: "Chờ duyệt (PENDING)", "Đã xác minh (APPROVED)", hoặc "Từ chối (REJECTED)" kèm lý do chi tiết.', 'VERIFICATION', 25, 1, NOW(), NOW()),
(26, 'Xác minh hồ sơ trung tâm gia sư cần những gì?', 'Trung tâm gia sư cần cung cấp: Giấy phép đăng ký kinh doanh, Căn cước công dân của người đại diện pháp luật, địa chỉ trụ sở và hợp đồng mẫu.', 'VERIFICATION', 26, 1, NOW(), NOW()),
(27, 'Làm sao để nộp lại giấy tờ khi bị từ chối xác minh?', 'Tại mục /profile, bấm vào nút "Cập nhật giấy tờ", chụp lại ảnh rõ nét theo đúng hướng dẫn và nhấn "Gửi duyệt lại".', 'VERIFICATION', 27, 1, NOW(), NOW()),
(28, 'Huy hiệu Đã xác minh (Verified Badge) mang lại lợi ích gì?', 'Gia sư có huy hiệu xác minh sẽ được ưu tiên hiển thị đầu trang tìm kiếm, tăng 80% tỷ lệ được phụ huynh chọn và đủ điều kiện nhận lớp có học phí cao.', 'VERIFICATION', 28, 1, NOW(), NOW()),
(29, 'Có cần công chứng bằng cấp khi tải lên không?', 'TCS chỉ yêu cầu ảnh chụp bản gốc hoặc bản sao công chứng rõ nét, thể hiện đầy đủ số hiệu văn bằng và con dấu để đội ngũ quản trị viên kiểm tra.', 'VERIFICATION', 29, 1, NOW(), NOW()),
(30, 'Xác minh chứng chỉ ngoại ngữ IELTS, TOEIC như thế nào?', 'Bạn tải lên bảng điểm hoặc chứng chỉ còn hiệu lực, hệ thống sẽ đối soát mã tra cứu chứng chỉ với cơ quan cấp phép quốc tế.', 'VERIFICATION', 30, 1, NOW(), NOW()),
(31, 'Học sinh chưa tốt nghiệp đại học có được làm gia sư không?', 'Có, sinh viên các trường đại học, cao đẳng chỉ cần tải Thẻ sinh viên còn hiệu lực và bảng điểm học tập để được duyệt làm gia sư sinh viên.', 'VERIFICATION', 31, 1, NOW(), NOW()),
(32, 'Duyệt hồ sơ gia sư có mất phí không?', 'Quy trình kiểm duyệt hồ sơ và cấp huy hiệu xác minh trên TCS là hoàn toàn miễn phí cho tất cả gia sư và trung tâm.', 'VERIFICATION', 32, 1, NOW(), NOW()),
(33, 'Thông tin CCCD sau khi duyệt có được lưu vĩnh viễn không?', 'Dữ liệu được lưu trữ an toàn trong kho lưu trữ mã hóa riêng biệt nhằm bảo vệ quyền lợi pháp lý cho các bên khi phát sinh ký kết hợp đồng.', 'VERIFICATION', 33, 1, NOW(), NOW()),
(34, 'Ai là người trực tiếp duyệt hồ sơ gia sư?', 'Đội ngũ Chuyên viên Pháp chế và Vận hành của TCS kiểm tra thủ công từng hồ sơ kết hợp công nghệ kiểm tra tự động OCR.', 'VERIFICATION', 34, 1, NOW(), NOW()),
(35, 'Gia sư chưa xác minh có được nộp hồ sơ nhận lớp không?', 'Gia sư chưa xác minh vẫn có thể xem thông tin lớp học nhưng chỉ được nộp hồ sơ ứng tuyển chính thức sau khi hồ sơ đạt trạng thái APPROVED.', 'VERIFICATION', 35, 1, NOW(), NOW()),

-- 3. MARKETPLACE (36 to 65)
(36, 'Làm sao để tìm gia sư phù hợp trên TCS?', 'Truy cập mục /tim-gia-su, sử dụng bộ lọc môn học, khối lớp, khu vực (quận/huyện), hình thức dạy (Online/Tại nhà) và mức học phí để chọn gia sư ưng ý.', 'MARKETPLACE', 36, 1, NOW(), NOW()),
(37, 'Làm sao để đăng bài tìm gia sư (Tạo lớp học)?', 'Học viên hoặc phụ huynh vào mục /tao-lop, điền đầy đủ môn học, lớp, địa chỉ học, thời gian rảnh và mức thù lao dự kiến rồi bấm "Đăng yêu cầu".', 'MARKETPLACE', 37, 1, NOW(), NOW()),
(38, 'Tìm lớp học đang tuyển gia sư ở đâu?', 'Gia sư truy cập mục /lop-hoc để xem danh sách toàn bộ các lớp đang ở trạng thái OPEN, xem yêu cầu học viên và nộp đơn ứng tuyển.', 'MARKETPLACE', 38, 1, NOW(), NOW()),
(39, 'Gia sư ứng tuyển lớp học như thế nào?', 'Tại trang chi tiết lớp học /lop-hoc/{id}, gia sư bấm nút "Ứng tuyển", nhập mức học phí đề xuất kèm lời giới thiệu bản thân rồi gửi phụ huynh xem xét.', 'MARKETPLACE', 39, 1, NOW(), NOW()),
(40, 'Phụ huynh chọn gia sư ứng tuyển ra sao?', 'Phụ huynh vào mục quản lý lớp học, bấm xem danh sách ứng viên, so sánh hồ sơ bằng cấp, đánh giá sao và bấm "Chấp nhận" gia sư phù hợp nhất.', 'MARKETPLACE', 40, 1, NOW(), NOW()),
(41, 'Trạng thái lớp học OPEN, ASSIGNED, COMPLETED, CANCELLED nghĩa là gì?', 'OPEN: Đang nhận ứng tuyển; ASSIGNED: Đã chọn gia sư và ký hợp đồng; COMPLETED: Đã học xong và tất toán; CANCELLED: Đã hủy lớp do phụ huynh yêu cầu.', 'MARKETPLACE', 41, 1, NOW(), NOW()),
(42, 'Học phí gia sư trên TCS dao động khoảng bao nhiêu?', 'Học phí thông thường từ 150.000 ₫ – 250.000 ₫/buổi đối với sinh viên và từ 300.000 ₫ – 500.000 ₫/buổi đối với giáo viên luyện thi chuyên sâu.', 'MARKETPLACE', 42, 1, NOW(), NOW()),
(43, 'Có thể thương lượng mức học phí với gia sư không?', 'Có, khi gia sư nộp đơn ứng tuyển có thể đề xuất mức giá khác với bài đăng gốc. Phụ huynh có thể trao đổi qua tin nhắn trước khi ký hợp đồng.', 'MARKETPLACE', 43, 1, NOW(), NOW()),
(44, 'Một lớp học có thể có bao nhiêu gia sư ứng tuyển?', 'Không giới hạn số lượng gia sư ứng tuyển cho đến khi phụ huynh lựa chọn được ứng viên ưng ý và tiến hành ký kết hợp đồng.', 'MARKETPLACE', 44, 1, NOW(), NOW()),
(45, 'Học phí dạy Online có rẻ hơn học tại nhà không?', 'Hình thức học Online thường tiết kiệm chi phí đi lại nên học phí có thể thấp hơn từ 15% – 30% so với hình thức gia sư đến tận nhà.', 'MARKETPLACE', 45, 1, NOW(), NOW()),
(46, 'Tìm gia sư luyện thi vào lớp 10 ở đâu?', 'Tại trang /tim-gia-su, chọn khối lớp "Lớp 9", môn Toán/Văn/Anh và chọn mục tiêu "Luyện thi vào 10" trong bộ lọc chuyên môn.', 'MARKETPLACE', 46, 1, NOW(), NOW()),
(47, 'Tìm gia sư luyện thi Đại học khối A, B, C, D1 thế nào?', 'Tại /tim-gia-su, chọn khối lớp "Lớp 12" kèm các môn tổ hợp như Toán - Lý - Hóa, Toán - Hóa - Sinh, hoặc Toán - Văn - Anh để tìm gia sư chuyên ban.', 'MARKETPLACE', 47, 1, NOW(), NOW()),
(48, 'Tìm gia sư dạy Tiếng Anh giao tiếp cho người đi làm?', 'Chọn môn "Tiếng Anh", cấp độ "Người đi làm / Giao tiếp" tại /tim-gia-su để lọc các gia sư có chứng chỉ IELTS và kinh nghiệm giảng dạy công sở.', 'MARKETPLACE', 48, 1, NOW(), NOW()),
(49, 'Tìm gia sư dạy kèm Tin học lập trình và Toán tư duy?', 'Hệ thống hỗ trợ danh mục môn học mở rộng bao gồm Lập trình Scratch, Python, C++, Tin học văn phòng và Toán tư duy Singapore.', 'MARKETPLACE', 49, 1, NOW(), NOW()),
(50, 'Sau khi đăng lớp bao lâu thì có gia sư ứng tuyển?', 'Thông thường chỉ sau 15–60 phút đăng lớp tại /tao-lop, hệ thống sẽ tự động thông báo đến các gia sư phù hợp trong khu vực để nộp hồ sơ.', 'MARKETPLACE', 50, 1, NOW(), NOW()),
(51, 'Phụ huynh có thể hủy bài đăng tìm gia sư không?', 'Có, nếu đã tìm được người dạy hoặc thay đổi kế hoạch, phụ huynh có thể vào danh sách lớp của mình và chọn "Đóng/Hủy bài đăng" bất kỳ lúc nào.', 'MARKETPLACE', 51, 1, NOW(), NOW()),
(52, 'Gia sư có thể rút lại đơn ứng tuyển đã nộp không?', 'Gia sư có thể hủy đơn ứng tuyển trước thời điểm phụ huynh bấm chấp nhận hợp tác trong mục Quản lý ứng tuyển của gia sư.', 'MARKETPLACE', 52, 1, NOW(), NOW()),
(53, 'Học thử buổi đầu tiên có mất phí không?', 'Học phí buổi học đầu tiên tùy thuộc vào thỏa thuận giữa hai bên trong hợp đồng, thông thường nếu không hài lòng có thể mở yêu cầu hoàn tiền.', 'MARKETPLACE', 53, 1, NOW(), NOW()),
(54, 'TCS hỗ trợ tìm gia sư ở những tỉnh thành nào?', 'TCS hỗ trợ toàn diện các khu vực tại Hà Nội, TP. Hồ Chí Minh, Đà Nẵng, Hải Phòng, Cần Thơ và học Online trên toàn quốc.', 'MARKETPLACE', 54, 1, NOW(), NOW()),
(55, 'Làm sao xem danh sách các gia sư được đánh giá cao nhất?', 'Tại /tim-gia-su, bạn chọn sắp xếp theo "Điểm đánh giá cao nhất" hoặc "Số lượng đánh giá nhiều nhất" để xem top gia sư uy tín.', 'MARKETPLACE', 55, 1, NOW(), NOW()),
(56, 'Có thể thuê gia sư dạy nhóm từ 2–5 học sinh không?', 'Có, phụ huynh có thể tạo bài đăng lớp nhóm tại /tao-lop và ghi rõ số lượng học sinh để gia sư chuẩn bị giáo án phù hợp.', 'MARKETPLACE', 56, 1, NOW(), NOW()),
(57, 'Học viên người nước ngoài có tìm được gia sư dạy Tiếng Việt không?', 'Có, TCS có đội ngũ gia sư sư phạm chuyên ngành Tiếng Việt cho người nước ngoài (Vietnamese for Expats).', 'MARKETPLACE', 57, 1, NOW(), NOW()),
(58, 'Xem chi tiết bằng cấp và video giới thiệu của gia sư ở đâu?', 'Bấm trực tiếp vào ảnh hoặc tên gia sư tại trang tìm kiếm /tim-gia-su để xem trang thông tin chi tiết cá nhân đầy đủ.', 'MARKETPLACE', 58, 1, NOW(), NOW()),
(59, 'Tìm gia sư dạy các môn năng khiếu Đàn Piano, Guitar, Vẽ?', 'Tại bộ lọc môn học, chọn nhóm Môn Năng khiếu: Piano, Organ, Guitar, Hội họa hoặc Cờ vua.', 'MARKETPLACE', 59, 1, NOW(), NOW()),
(60, 'Tìm gia sư can thiệp sớm và trẻ chậm nói?', 'TCS có chuyên mục Giáo dục đặc biệt với các gia sư tốt nghiệp chuyên ngành Giáo dục đặc biệt từ Đại học Sư phạm.', 'MARKETPLACE', 60, 1, NOW(), NOW()),
(61, 'Gia sư có thể dạy song song nhiều lớp không?', 'Có, gia sư có thể nhận nhiều lớp học khác nhau miễn là sắp xếp lịch dạy không bị trùng giờ và đảm bảo chất lượng giảng dạy.', 'MARKETPLACE', 61, 1, NOW(), NOW()),
(62, 'Làm sao để biết lớp học có khoảng cách gần nhà gia sư?', 'Hệ thống tự động hiển thị khoảng cách và bản đồ vị trí giữa địa chỉ lớp học với vị trí sinh sống của gia sư.', 'MARKETPLACE', 62, 1, NOW(), NOW()),
(63, 'Phụ huynh có thể mời trực tiếp 1 gia sư vào dạy lớp không?', 'Có, tại trang cá nhân của gia sư, bấm "Mời dạy lớp" và chọn bài đăng lớp học của bạn để gửi lời mời riêng.', 'MARKETPLACE', 63, 1, NOW(), NOW()),
(64, 'Lớp học có bắt buộc phải kết thúc đúng số buổi đăng ký không?', 'Hai bên có thể thỏa thuận gia hạn thêm buổi học bằng cách tạo phụ lục hợp đồng bổ sung trên hệ thống.', 'MARKETPLACE', 64, 1, NOW(), NOW()),
(65, 'Thông báo lớp học mới được gửi qua những kênh nào?', 'Gia sư sẽ nhận thông báo lớp phù hợp tức thì qua Email, chuông thông báo trên web và thông báo đẩy qua ứng dụng.', 'MARKETPLACE', 65, 1, NOW(), NOW()),

-- 4. TUTOR_OPS (66 to 80)
(66, 'Xem lịch dạy của gia sư ở đâu?', 'Gia sư truy cập mục Lịch dạy tại /tutor/schedule để xem toàn bộ các ca dạy trong tuần, thời gian bắt đầu và thông tin học viên.', 'TUTOR_OPS', 66, 1, NOW(), NOW()),
(67, 'Điểm danh học viên sau mỗi buổi học thế nào?', 'Sau khi kết thúc buổi dạy, gia sư vào /tutor/classes, chọn buổi học tương ứng và bấm "Điểm danh" kèm nội dung bài đã học.', 'TUTOR_OPS', 67, 1, NOW(), NOW()),
(68, 'Xin dời hoặc đổi lịch buổi dạy như thế nào?', 'Tại /tutor/schedule, chọn ca học cần đổi, bấm "Yêu cầu dời lịch", chọn khung giờ mới và gửi lý do để phụ huynh/học viên phê duyệt.', 'TUTOR_OPS', 68, 1, NOW(), NOW()),
(69, 'Gia sư xin nghỉ một buổi dạy cần làm gì?', 'Gia sư cần gửi thông báo trước ít nhất 12 giờ trên hệ thống kèm lịch học bù đề xuất để phụ huynh chủ động sắp xếp thời gian.', 'TUTOR_OPS', 69, 1, NOW(), NOW()),
(70, 'Yêu cầu gia sư dạy thay (Substitute) hoạt động ra sao?', 'Nếu bận việc đột xuất dài ngày, gia sư có thể tạo yêu cầu người dạy thay tại /tutor/classes để trung tâm hoặc hệ thống hỗ trợ kết nối.', 'TUTOR_OPS', 70, 1, NOW(), NOW()),
(71, 'Ghi chú đánh giá buổi học (Lesson Notes) ở đâu?', 'Khi điểm danh, gia sư có thể nhập nhận xét về mức độ tiếp thu, bài tập về nhà và sự tập trung của học sinh để phụ huynh theo dõi.', 'TUTOR_OPS', 71, 1, NOW(), NOW()),
(72, 'Phụ huynh có nhận được thông báo khi gia sư điểm danh không?', 'Có, ngay khi gia sư bấm điểm danh, hệ thống tự động gửi thông báo đến tài khoản phụ huynh để xác nhận buổi học đã diễn ra.', 'TUTOR_OPS', 72, 1, NOW(), NOW()),
(73, 'Gia sư đến muộn hoặc về sớm thì tính buổi học thế nào?', 'Gia sư cần đảm bảo dạy đủ tổng thời lượng theo thỏa thuận (thông thường 90–120 phút/buổi), nếu muộn cần bù đủ giờ cho học viên.', 'TUTOR_OPS', 73, 1, NOW(), NOW()),
(74, 'Quản lý danh sách tài liệu học tập cho lớp ở đâu?', 'Trong chi tiết lớp học tại /tutor/classes, gia sư có thể tải lên tệp giáo án, đề kiểm tra PDF để học viên tải về học tập.', 'TUTOR_OPS', 74, 1, NOW(), NOW()),
(75, 'Học sinh vắng mặt không báo trước thì điểm danh thế nào?', 'Gia sư chọn trạng thái "Học sinh vắng mặt". Quy định tính phí buổi vắng được áp dụng theo điều khoản trong hợp đồng đã ký kết.', 'TUTOR_OPS', 75, 1, NOW(), NOW()),
(76, 'Làm sao đồng bộ lịch dạy với Google Calendar?', 'Tại trang /tutor/schedule, bấm nút "Đồng bộ Google Calendar" và cấp quyền để tự động hiển thị ca dạy trên ứng dụng lịch điện thoại.', 'TUTOR_OPS', 76, 1, NOW(), NOW()),
(77, 'Xem tổng số buổi đã dạy và số buổi còn lại ở đâu?', 'Trong bảng quản lý tiến độ lớp học tại /tutor/classes hiển thị thanh tiến độ trực quan: Số buổi hoàn thành / Tổng số buổi hợp đồng.', 'TUTOR_OPS', 77, 1, NOW(), NOW()),
(78, 'Làm gì khi phụ huynh không xác nhận điểm danh?', 'Nếu phụ huynh không khiếu nại trong vòng 48 giờ kể từ khi gia sư điểm danh, hệ thống sẽ tự động xác nhận buổi học hoàn tất.', 'TUTOR_OPS', 78, 1, NOW(), NOW()),
(79, 'Có thể thay đổi địa điểm dạy học sau khi nhận lớp không?', 'Địa điểm học chỉ có thể thay đổi khi có sự đồng thuận bằng văn bản hoặc tin nhắn xác nhận giữa phụ huynh và gia sư.', 'TUTOR_OPS', 79, 1, NOW(), NOW()),
(80, 'Gia sư có thể yêu cầu kết thúc lớp sớm không?', 'Nếu có lý do chính đáng không thể tiếp tục, gia sư gửi yêu cầu thanh lý hợp đồng sớm tại /tutor/classes để tiến hành tất toán số buổi đã dạy.', 'TUTOR_OPS', 80, 1, NOW(), NOW()),

-- 5. CENTER_OPS (81 to 95)
(81, 'Trung tâm gia sư quản lý danh sách giáo viên ở đâu?', 'Quản trị viên trung tâm truy cập trang /center, chọn tab "Gia sư trực thuộc" để xem danh sách, trạng thái và phân công lớp học.', 'CENTER_OPS', 81, 1, NOW(), NOW()),
(82, 'Thêm gia sư mới vào trung tâm gia sư như thế nào?', 'Tại /center, bấm nút "Thêm gia sư", nhập email hoặc mã gia sư trên hệ thống TCS để gửi lời mời gia nhập trung tâm.', 'CENTER_OPS', 82, 1, NOW(), NOW()),
(83, 'Đăng bài tuyển dụng gia sư cho trung tâm ở đâu?', 'Trung tâm vào mục Tuyển dụng tại /center/recruitment, điền yêu cầu tuyển dụng, mức lương và chế độ đãi ngộ để tiếp nhận hồ sơ.', 'CENTER_OPS', 83, 1, NOW(), NOW()),
(84, 'Duyệt gia sư ứng tuyển vào trung tâm ra sao?', 'Tại /center/recruitment, trung tâm xem danh sách ứng viên nộp hồ sơ, kiểm tra CV/bằng cấp và bấm "Phê duyệt" để thêm vào đội ngũ.', 'CENTER_OPS', 84, 1, NOW(), NOW()),
(85, 'Hợp đồng giữa trung tâm và gia sư trực thuộc quản lý ở đâu?', 'Mục /center/contracts lưu trữ toàn bộ hợp đồng hợp tác, tỷ lệ ăn chia hoa hồng và cam kết chất lượng giữa trung tâm với gia sư.', 'CENTER_OPS', 85, 1, NOW(), NOW()),
(86, 'Xem báo cáo doanh thu và dòng tiền của trung tâm ở đâu?', 'Tại /center/analytics, trung tâm có thể theo dõi biểu đồ doanh thu theo tuần/tháng, số lượng lớp đang chạy và tiền hoa hồng thực nhận.', 'CENTER_OPS', 86, 1, NOW(), NOW()),
(87, 'Tạo lớp học nhóm cho trung tâm gia sư như thế nào?', 'Trung tâm vào /center/classes, chọn "Tạo lớp nhóm", thiết lập sĩ số tối đa, mức học phí từng học viên và phân công giáo viên đứng lớp.', 'CENTER_OPS', 87, 1, NOW(), NOW()),
(88, 'Xóa hoặc gỡ gia sư khỏi trung tâm thế nào?', 'Trong danh sách gia sư tại /center, chọn gia sư cần gỡ, bấm "Rút khỏi trung tâm" sau khi đã tất toán toàn bộ các lớp phụ trách.', 'CENTER_OPS', 88, 1, NOW(), NOW()),
(89, 'Trung tâm có thể phân công gia sư dạy thay cho lớp không?', 'Có, trung tâm có quyền điều phối và đổi giáo viên phụ trách lớp học khi giáo viên chính có việc bận đột xuất.', 'CENTER_OPS', 89, 1, NOW(), NOW()),
(90, 'Thiết lập tỷ lệ hoa hồng trung tâm (Commission Rate) ở đâu?', 'Tại mục Cài đặt trung tâm /center/settings, quản trị viên có thể cấu hình tỷ lệ hoa hồng cố định hoặc linh hoạt theo từng môn học.', 'CENTER_OPS', 90, 1, NOW(), NOW()),
(91, 'Trung tâm có thể xuất báo cáo tài chính ra file Excel/CSV không?', 'Có, tại trang /center/analytics có nút "Xuất báo cáo CSV" để tải về dữ liệu thu chi, học phí và lương giáo viên chi tiết.', 'CENTER_OPS', 91, 1, NOW(), NOW()),
(92, 'Học viên thanh toán học phí cho trung tâm qua đâu?', 'Học viên nạp tiền và thanh toán qua tài khoản ký quỹ Escrow của TCS để đảm bảo an toàn, tiền sẽ tự động chia về ví trung tâm sau khi hoàn tất.', 'CENTER_OPS', 92, 1, NOW(), NOW()),
(93, 'Trung tâm gia sư có thể tạo nhiều chi nhánh không?', 'Có, trung tâm có thể quản lý nhiều cơ sở giảng dạy khác nhau trên cùng một bảng điều khiển trung tâm tại /center.', 'CENTER_OPS', 93, 1, NOW(), NOW()),
(94, 'Đánh giá uy tín của trung tâm gia sư được tính thế nào?', 'Dựa trên điểm trung bình sao của tất cả các lớp học do giáo viên trực thuộc trung tâm giảng dạy và tỷ lệ giải quyết khiếu nại thành công.', 'CENTER_OPS', 94, 1, NOW(), NOW()),
(95, 'Làm sao để đăng ký mở tài khoản Trung tâm gia sư?', 'Tại trang /register, chọn vai trò "Trung tâm gia sư (Tutor Center)", điền tên tổ chức, giấy phép kinh doanh và thông tin người đại diện.', 'CENTER_OPS', 95, 1, NOW(), NOW()),

-- 6. FINANCE_ESCROW (96 to 125)
(96, 'Cơ chế ký quỹ Escrow trên TCS hoạt động như thế nào?', 'Khi phụ huynh chọn gia sư, học phí được giữ an toàn tại tài khoản Escrow của TCS. Tiền chỉ được giải ngân cho gia sư sau khi các buổi học hoàn thành đúng cam kết.', 'FINANCE_ESCROW', 96, 1, NOW(), NOW()),
(97, 'Phí nền tảng (Platform Fee) của TCS là bao nhiêu?', 'TCS áp dụng mức phí sàn cố định 10% trên giá trị hợp đồng thành công để duy trì vận hành hệ thống, bảo vệ ký quỹ và chăm sóc khách hàng 24/7.', 'FINANCE_ESCROW', 97, 1, NOW(), NOW()),
(98, 'Làm sao nạp tiền vào ví bằng mã QR SePay tự động?', 'Vào /finance, chọn "Nạp tiền", nhập số tiền cần nạp, hệ thống sẽ tạo mã VietQR SePay tự động. Bạn chỉ cần quét mã trên App ngân hàng để tiền vào ví tức thì.', 'FINANCE_ESCROW', 98, 1, NOW(), NOW()),
(99, 'Gia sư rút tiền về tài khoản ngân hàng như thế nào?', 'Vào mục /finance, chọn "Rút tiền", nhập số tài khoản ngân hàng thụ hưởng, tên chủ tài khoản và số tiền cần rút (tối thiểu 50.000 ₫) rồi bấm Xác nhận.', 'FINANCE_ESCROW', 99, 1, NOW(), NOW()),
(100, 'Thời gian xử lý yêu cầu rút tiền mất bao lâu?', 'Hệ thống đối soát và chuyển tiền tự động trong vòng 1–4 giờ làm việc. Tối đa không quá 24 giờ kể từ khi lệnh rút được tạo.', 'FINANCE_ESCROW', 100, 1, NOW(), NOW()),
(101, 'Xem lịch sử giao dịch và biến động số dư ở đâu?', 'Tại mục /finance/history hiển thị đầy đủ nhật ký nạp tiền, trừ tiền cọc Escrow, nhận học phí giải ngân và các khoản phí dịch vụ.', 'FINANCE_ESCROW', 101, 1, NOW(), NOW()),
(102, 'Chính sách hoàn tiền học phí (Refund Policy) như thế nào?', 'Nếu lớp học bị hủy trước khi bắt đầu, phụ huynh được hoàn 100% tiền Escrow. Nếu hủy giữa chừng, tiền hoàn được tính theo tỷ lệ các buổi chưa học.', 'FINANCE_ESCROW', 102, 1, NOW(), NOW()),
(103, 'Vì sao số dư trong ví bị tạm giữ (Held / Frozen)?', 'Số dư bị tạm giữ khi đang nằm trong hợp đồng lớp học đang diễn ra hoặc tài khoản đang có lệnh rút tiền chờ ngân hàng xử lý.', 'FINANCE_ESCROW', 103, 1, NOW(), NOW()),
(104, 'Gia sư xem tổng thu nhập tháng này ở đâu?', 'Tại /finance hiển thị thẻ "Thu nhập tháng hiện tại", thống kê số tiền thực nhận sau khi đã trừ phí nền tảng 10%.', 'FINANCE_ESCROW', 104, 1, NOW(), NOW()),
(105, 'Nạp tiền bằng chuyển khoản ngân hàng có mất phí không?', 'TCS không thu bất kỳ khoản phí nạp tiền nào. Bạn được miễn phí nạp 100% qua cổng chuyển khoản VietQR SePay.', 'FINANCE_ESCROW', 105, 1, NOW(), NOW()),
(106, 'Rút tiền về ngân hàng có bị giới hạn số lần trong ngày không?', 'Mỗi tài khoản được thực hiện tối đa 3 lệnh rút tiền/ngày với tổng hạn mức rút không vượt quá 50.000.000 ₫/ngày.', 'FINANCE_ESCROW', 106, 1, NOW(), NOW()),
(107, 'Làm gì khi nạp tiền thành công mà số dư ví chưa cập nhật?', 'Hệ thống SePay tự động cộng tiền trong 30 giây. Nếu mạng ngân hàng chậm, bạn gửi ảnh biên lai chuyển tiền tại /support/tickets để nhân viên hỗ trợ cộng ngay.', 'FINANCE_ESCROW', 107, 1, NOW(), NOW()),
(108, 'Tiền ký quỹ Escrow được giải ngân theo từng buổi hay cả khóa?', 'Mặc định tiền được giải ngân định kỳ theo từng tháng hoặc sau khi hoàn tất toàn bộ số buổi học tùy theo cấu hình của hợp đồng.', 'FINANCE_ESCROW', 108, 1, NOW(), NOW()),
(109, 'Gia sư có phải trả phí trước khi nhận lớp không?', 'Không, TCS tuyệt đối KHÔNG thu phí nhận lớp trước của gia sư. Phí nền tảng chỉ được trừ tự động khi gia sư đã hoàn thành giảng dạy và nhận tiền.', 'FINANCE_ESCROW', 109, 1, NOW(), NOW()),
(110, 'Phụ huynh có bị mất phí nền tảng khi thanh toán không?', 'Không, phụ huynh chỉ thanh toán đúng số tiền học phí theo mức giá đã thỏa thuận với gia sư mà không mất thêm phụ phí nào.', 'FINANCE_ESCROW', 110, 1, NOW(), NOW()),
(111, 'Trạng thái lệnh rút tiền PENDING, APPROVED, REJECTED, TRANSFER_FAILED nghĩa là gì?', 'PENDING: Đang chờ duyệt; APPROVED: Đã duyệt và gửi lệnh chi; REJECTED: Bị từ chối do sai thông tin TK; TRANSFER_FAILED: Ngân hàng lỗi đường truyền.', 'FINANCE_ESCROW', 111, 1, NOW(), NOW()),
(112, 'Làm sao liên kết tài khoản ngân hàng nhận tiền?', 'Trong mục /finance, chọn "Tài khoản ngân hàng", chọn tên ngân hàng, nhập số tài khoản và bấm Lưu để dùng cho các lần rút tiền sau.', 'FINANCE_ESCROW', 112, 1, NOW(), NOW()),
(113, 'Có thể rút tiền về ví điện tử MoMo, ZaloPay không?', 'Hiện tại TCS hỗ trợ chuyển tiền trực tiếp về hơn 40 ngân hàng nội địa Việt Nam thông qua hệ thống Napas 247.', 'FINANCE_ESCROW', 113, 1, NOW(), NOW()),
(114, 'Tiền hoàn trả (Refund) được chuyển về đâu?', 'Tiền hoàn trả được cộng trực tiếp vào Số dư khả dụng của ví TCS. Phụ huynh có thể dùng để thuê gia sư khác hoặc rút về tài khoản ngân hàng.', 'FINANCE_ESCROW', 114, 1, NOW(), NOW()),
(115, 'Hợp đồng bị tranh chấp thì tiền Escrow được xử lý thế nào?', 'Toàn bộ số tiền ký quỹ sẽ được đóng băng bảo vệ cho đến khi Quản trị viên đối soát bằng chứng và đưa ra phán quyết hòa giải thỏa đáng.', 'FINANCE_ESCROW', 115, 1, NOW(), NOW()),
(116, 'Có thể xuất hóa đơn giá trị gia tăng (VAT) cho học phí không?', 'Doanh nghiệp hoặc phụ huynh có nhu cầu xuất hóa đơn VAT phí dịch vụ có thể gửi thông tin mã số thuế công ty tại /support/tickets.', 'FINANCE_ESCROW', 116, 1, NOW(), NOW()),
(117, 'Số tiền rút tối thiểu và tối đa cho mỗi giao dịch là bao nhiêu?', 'Số tiền rút tối thiểu là 50.000 ₫/lệnh và tối đa là 20.000.000 ₫ cho một lần rút.', 'FINANCE_ESCROW', 117, 1, NOW(), NOW()),
(118, 'Làm sao kiểm tra mã tham chiếu giao dịch (Transaction Ref)?', 'Trong bảng lịch sử /finance/history, mỗi giao dịch đều có mã tham chiếu duy nhất dạng TX-XXXXXX để tra cứu khi cần đối soát.', 'FINANCE_ESCROW', 118, 1, NOW(), NOW()),
(119, 'Học viên có thể thanh toán học phí từng buổi một không?', 'Có, học viên có thể ký hợp đồng theo gói ngắn hạn (ví dụ gói 4 buổi/lần) để nạp ký quỹ linh hoạt thay vì nạp cả khóa dài hạn.', 'FINANCE_ESCROW', 119, 1, NOW(), NOW()),
(120, 'Gia sư có thể chuyển tiền từ ví sang tài khoản học viên khác không?', 'Hiện tại ví TCS chỉ hỗ trợ chức năng nạp tiền, nhận học phí lớp học và rút tiền về tài khoản ngân hàng chính chủ của người dùng.', 'FINANCE_ESCROW', 120, 1, NOW(), NOW()),
(121, 'Tài khoản ngân hàng rút tiền có bắt buộc trùng tên với CCCD không?', 'Để phòng chống gian lận và rửa tiền, tên chủ tài khoản ngân hàng nhận tiền phải trùng khớp với họ tên đã xác minh trên hồ sơ CCCD.', 'FINANCE_ESCROW', 121, 1, NOW(), NOW()),
(122, 'Phí phạt khi hủy lớp học sát giờ là bao nhiêu?', 'Nếu một bên tự ý hủy lớp trước giờ học dưới 2 giờ mà không có lý do bất khả kháng, bên đó có thể bị trừ 50% học phí của buổi học đó.', 'FINANCE_ESCROW', 122, 1, NOW(), NOW()),
(123, 'Làm sao để xem báo cáo tài chính tổng quan theo năm?', 'Tại mục /finance, chọn bộ lọc khoảng thời gian từ ngày bắt đầu đến ngày kết thúc để xem tổng hợp dòng tiền ra vào trong năm.', 'FINANCE_ESCROW', 123, 1, NOW(), NOW()),
(124, 'Nạp tiền qua thẻ tín dụng quốc tế Visa/Mastercard có được không?', 'TCS hiện đang hỗ trợ thanh toán nội địa qua QR SePay/VNPAY và đang tích hợp cổng thanh toán thẻ quốc tế trong thời gian tới.', 'FINANCE_ESCROW', 124, 1, NOW(), NOW()),
(125, 'Tiền ký quỹ Escrow được bảo chứng bởi đơn vị nào?', 'Tất cả các khoản tiền ký quỹ được bảo chứng và phong tỏa tại tài khoản đối tác thanh toán ngân hàng thương mại được cấp phép.', 'FINANCE_ESCROW', 125, 1, NOW(), NOW()),

-- 7. CONTRACT_REVIEW (126 to 140)
(126, 'Hợp đồng điện tử trên TCS có giá trị pháp lý không?', 'Có, hợp đồng điện tử trên TCS được xác thực bằng mã OTP theo quy định của Luật Giao dịch điện tử Việt Nam, ràng buộc nghĩa vụ giữa phụ huynh và gia sư.', 'CONTRACT_REVIEW', 126, 1, NOW(), NOW()),
(127, 'Làm sao để ký hợp đồng lớp học bằng mã OTP?', 'Khi nhận được thông báo hợp đồng tại /contracts, bạn đọc kỹ các điều khoản, bấm "Ký hợp đồng", nhập mã OTP 6 số gửi về điện thoại để hoàn tất.', 'CONTRACT_REVIEW', 127, 1, NOW(), NOW()),
(128, 'Xem danh sách các hợp đồng đã ký ở đâu?', 'Bạn truy cập mục /contracts để xem toàn bộ danh sách hợp đồng: Đang chờ ký (PENDING), Đang hiệu lực (ACTIVE), hoặc Đã kết thúc (COMPLETED).', 'CONTRACT_REVIEW', 128, 1, NOW(), NOW()),
(129, 'Đánh giá gia sư sau khóa học như thế nào?', 'Khi lớp học kết thúc, phụ huynh vào /classes, chọn lớp học và bấm "Đánh giá gia sư" để chấm điểm sao (1–5 sao) kèm lời nhận xét chi tiết.', 'CONTRACT_REVIEW', 129, 1, NOW(), NOW()),
(130, 'Điểm uy tín (Reputation Score) của gia sư được tính ra sao?', 'Điểm uy tín được tính tự động dựa trên: (1) Điểm đánh giá trung bình từ học viên; (2) Tỷ lệ hoàn thành lớp học; (3) Tỷ lệ điểm danh đúng giờ.', 'CONTRACT_REVIEW', 130, 1, NOW(), NOW()),
(131, 'Có thể sửa hoặc xóa nhận xét đánh giá đã gửi không?', 'Đánh giá sau khi gửi sẽ hiển thị công khai để đảm bảo tính khách quan. Bạn có thể gửi yêu cầu chỉnh sửa trong vòng 7 ngày tại /support/tickets.', 'CONTRACT_REVIEW', 131, 1, NOW(), NOW()),
(132, 'Gia sư có thể từ chối ký hợp đồng không?', 'Có, nếu điều kiện lịch học hoặc mức học phí không còn phù hợp, gia sư có thể bấm "Từ chối hợp đồng" và nêu rõ lý do.', 'CONTRACT_REVIEW', 132, 1, NOW(), NOW()),
(133, 'Hợp đồng có thể gia hạn thêm buổi học không?', 'Có, phụ huynh và gia sư có thể tạo phụ lục gia hạn hợp đồng trực tiếp trên giao diện quản lý lớp tại /contracts.', 'CONTRACT_REVIEW', 133, 1, NOW(), NOW()),
(134, 'Hợp đồng mẫu của TCS gồm những điều khoản chính nào?', 'Bao gồm: Môn học, số buổi học/tuần, mức học phí, cam kết chất lượng, quy định hoàn tiền Escrow và chế tài xử lý vi phạm hợp đồng.', 'CONTRACT_REVIEW', 134, 1, NOW(), NOW()),
(135, 'Đánh giá ẩn danh (Anonymous Review) có được hỗ trợ không?', 'Đánh giá sẽ hiển thị tên viết tắt của phụ huynh/học viên để đảm bảo tính chân thực và tránh các trường hợp đánh giá ảo.', 'CONTRACT_REVIEW', 135, 1, NOW(), NOW()),
(136, 'Vì sao đánh giá của tôi không hiển thị trên hồ sơ gia sư?', 'Các đánh giá chứa từ ngữ phản cảm, vi phạm thuần phong mỹ tục hoặc spam quảng cáo sẽ bị bộ lọc tự động ẩn để chờ quản trị viên kiểm duyệt.', 'CONTRACT_REVIEW', 136, 1, NOW(), NOW()),
(137, 'Gia sư có thể phản hồi lại nhận xét của phụ huynh không?', 'Có, gia sư có thể viết phản hồi lịch sự dưới mỗi bài đánh giá tại trang cá nhân để giải thích và trao đổi thông tin minh bạch.', 'CONTRACT_REVIEW', 137, 1, NOW(), NOW()),
(138, 'Tải bản in hợp đồng dạng PDF ở đâu?', 'Trong chi tiết hợp đồng tại /contracts/{id}, bấm nút "Tải PDF" để lưu về máy bản hợp đồng có đóng dấu điện tử của nền tảng TCS.', 'CONTRACT_REVIEW', 138, 1, NOW(), NOW()),
(139, 'Nếu phụ huynh không ký hợp đồng thì lớp học có bắt đầu không?', 'Lớp học chỉ được kích hoạt chính thức và bắt đầu tính buổi học sau khi cả hai bên đã hoàn tất ký OTP và phụ huynh đã nạp cọc Escrow.', 'CONTRACT_REVIEW', 139, 1, NOW(), NOW()),
(140, 'Mức đánh giá bao nhiêu sao thì gia sư bị cảnh cáo?', 'Gia sư có điểm đánh giá trung bình dưới 3.0 sao trong 3 lớp học liên tiếp sẽ nhận cảnh báo chất lượng và tạm ngưng quyền nhận lớp mới.', 'CONTRACT_REVIEW', 140, 1, NOW(), NOW()),

-- 8. TRUST_SAFETY (141 to 170)
(141, 'Làm sao báo cáo gia sư hoặc trung tâm lách sàn?', 'Bạn truy cập mục /support/tickets, chọn loại yêu cầu "Báo cáo vi phạm / Lách sàn", cung cấp số điện thoại hoặc liên kết lớp học kèm bằng chứng tin nhắn.', 'TRUST_SAFETY', 141, 1, NOW(), NOW()),
(142, 'Hành vi lách sàn (Circumvention) bị xử lý như thế nào?', 'TCS xử phạt nghiêm khắc: Khóa tài khoản vĩnh viễn, tịch thu số dư vi phạm và từ chối cung cấp dịch vụ cho tất cả các bên liên quan.', 'TRUST_SAFETY', 142, 1, NOW(), NOW()),
(143, 'Vì sao không nên giao dịch và chuyển tiền ngoài sàn?', 'Giao dịch ngoài sàn sẽ mất hoàn toàn sự bảo vệ của quỹ Escrow; khi gia sư bùng lịch hoặc phụ huynh quỵt tiền, TCS không thể can thiệp đòi lại học phí.', 'TRUST_SAFETY', 143, 1, NOW(), NOW()),
(144, 'Khi nào nên mở tranh chấp (Dispute) lớp học?', 'Nên mở tranh chấp khi gia sư bỏ dạy giữa chừng, dạy sai kiến thức nghiêm trọng, học sinh không thanh toán học phí hoặc có hành vi quấy rối.', 'TRUST_SAFETY', 144, 1, NOW(), NOW()),
(145, 'Quy trình giải quyết tranh chấp của TCS gồm các bước nào?', '(1) Tiếp nhận đơn tranh chấp; (2) Tạm khóa tiền ký quỹ; (3) Yêu cầu hai bên nộp bằng chứng trong 48h; (4) Hòa giải và đưa ra quyết định phân chia tiền hoàn.', 'TRUST_SAFETY', 145, 1, NOW(), NOW()),
(146, 'Tải bằng chứng tranh chấp lên hệ thống như thế nào?', 'Trong giao diện tranh chấp tại /support/tickets, bạn có thể tải lên ảnh chụp màn hình tin nhắn Zalo/SMS, bản ghi âm, hoặc video buổi học (tối đa 10 tệp).', 'TRUST_SAFETY', 146, 1, NOW(), NOW()),
(147, 'Thời gian xử lý một vụ tranh chấp là bao lâu?', 'Hội đồng Hòa giải TCS xử lý và đưa ra phán quyết chính thức trong vòng 3–5 ngày làm việc kể từ khi nhận đủ bằng chứng từ cả hai phía.', 'TRUST_SAFETY', 147, 1, NOW(), NOW()),
(148, 'Bị phạt cảnh cáo hoặc trừ điểm uy tín vì những lỗi gì?', 'Các lỗi: Thường xuyên đến muộn, tự ý hủy ca dạy sát giờ, có thái độ không chuẩn mực, hoặc bị học viên khiếu nại nhiều lần.', 'TRUST_SAFETY', 148, 1, NOW(), NOW()),
(149, 'Chế tài xử phạt khi vi phạm quy tắc cộng đồng TCS?', 'Các mức độ: (1) Cảnh cáo bằng văn bản; (2) Trừ 10–50 điểm uy tín; (3) Tạm ngưng quyền nhận lớp 14 ngày; (4) Khóa tài khoản vĩnh viễn.', 'TRUST_SAFETY', 149, 1, NOW(), NOW()),
(150, 'Làm sao tố cáo gia sư thu tiền học phí trực tiếp từ học sinh?', 'Gửi phiếu tố cáo tại /support/tickets kèm biên lai chuyển khoản hoặc tin nhắn yêu cầu thu tiền riêng để nhận thưởng bảo vệ cộng đồng từ TCS.', 'TRUST_SAFETY', 150, 1, NOW(), NOW()),
(151, 'Học sinh bị quấy rối hoặc đối xử thiếu văn minh phải làm sao?', 'Hãy lập tức dừng buổi học, liên hệ hotline khẩn cấp của TCS và tạo phiếu báo cáo khẩn tại /support/tickets để được can thiệp pháp lý.', 'TRUST_SAFETY', 151, 1, NOW(), NOW()),
(152, 'Gia sư bị phụ huynh đe dọa hoặc quỵt tiền học phí?', 'Nếu giao dịch được thực hiện qua hợp đồng Escrow trên TCS, học phí của gia sư được bảo vệ 100% và sẽ được giải ngân theo đúng cam kết.', 'TRUST_SAFETY', 152, 1, NOW(), NOW()),
(153, 'Tài khoản bị khóa do nghi vấn gian lận có khiếu nại được không?', 'Bạn có quyền gửi đơn giải trình tại /support/tickets kèm chứng từ minh bạch để Ban Kiểm soát TCS xem xét mở lại tài khoản.', 'TRUST_SAFETY', 153, 1, NOW(), NOW()),
(154, 'Chính sách bảo vệ trẻ em và người học vị thành niên?', 'Tất cả gia sư đều phải qua kiểm tra lý lịch CCCD và ký cam kết bảo vệ an toàn cho học sinh trước khi được phép nhận lớp tại nhà.', 'TRUST_SAFETY', 154, 1, NOW(), NOW()),
(155, 'Người dùng có bị lộ danh tính khi gửi tố cáo không?', 'Mọi thông tin người tố cáo và người gửi phản ánh vi phạm đều được bảo mật tuyệt đối theo chính sách bảo vệ nhân chứng của TCS.', 'TRUST_SAFETY', 155, 1, NOW(), NOW()),
(156, 'Làm sao nhận biết các hành vi lừa đảo giả mạo gia sư?', 'Cảnh giác với các yêu cầu chuyển cọc trước qua tài khoản cá nhân ngoài nền tảng hoặc gia sư từ chối cung cấp thẻ sinh viên/CCCD.', 'TRUST_SAFETY', 156, 1, NOW(), NOW()),
(157, 'Hủy lớp học sát giờ thi có bị phạt không?', 'Hủy lớp trong vòng 7 ngày trước kỳ thi quan trọng mà không có lý do bất khả kháng sẽ bị trừ 100 điểm uy tín và bồi thường theo hợp đồng.', 'TRUST_SAFETY', 157, 1, NOW(), NOW()),
(158, 'Tranh chấp hợp đồng không đạt được thỏa thuận thì xử lý ra sao?', 'Nếu không đồng ý với kết quả hòa giải nội bộ, các bên có quyền đưa vụ việc ra cơ quan Trọng tài thương mại hoặc Tòa án nhân dân có thẩm quyền.', 'TRUST_SAFETY', 158, 1, NOW(), NOW()),
(159, 'Làm sao bảo vệ thông tin số điện thoại của học viên?', 'TCS chỉ hiển thị số điện thoại liên lạc sau khi phụ huynh và gia sư đã hoàn tất ký hợp đồng lớp học chính thức.', 'TRUST_SAFETY', 159, 1, NOW(), NOW()),
(160, 'Trung tâm gia sư có được thu tiền môi giới của sinh viên không?', 'Trung tâm hoạt động trên TCS phải tuân thủ mức phí minh bạch và không được thu các khoản tiền đặt cọc giữ chỗ trái quy định pháp luật.', 'TRUST_SAFETY', 160, 1, NOW(), NOW()),
(161, 'Tài khoản bị đánh giá tiêu cực ác ý có được gỡ bỏ không?', 'Nếu chứng minh được đánh giá xuất phát từ đối thủ cạnh tranh hoặc không phản ánh đúng thực tế, quản trị viên sẽ gỡ bỏ đánh giá đó.', 'TRUST_SAFETY', 161, 1, NOW(), NOW()),
(162, 'Làm sao để biết một lớp học có dấu hiệu bất thường?', 'Các bài đăng có học phí cao bất thường nhưng nội dung mơ hồ hoặc yêu cầu gặp mặt ở địa điểm nhạy cảm sẽ bị hệ thống gắn cờ cảnh báo.', 'TRUST_SAFETY', 162, 1, NOW(), NOW()),
(163, 'Có được chia sẻ tài khoản TCS cho người khác dùng chung không?', 'Mỗi tài khoản gắn liền với danh tính và bằng cấp của một cá nhân duy nhất, việc cho mượn tài khoản sẽ dẫn đến việc bị khóa tài khoản vĩnh viễn.', 'TRUST_SAFETY', 163, 1, NOW(), NOW()),
(164, 'Quy định về văn hóa giao tiếp và ứng xử trên TCS?', 'Thành viên phải luôn giữ thái độ tôn trọng, lịch sự, không sử dụng ngôn từ tục tĩu, xúc phạm hoặc phân biệt đối xử.', 'TRUST_SAFETY', 164, 1, NOW(), NOW()),
(165, 'Báo cáo sai sự thật để hãm hại người khác bị xử lý thế nào?', 'Hành vi vu khống và báo cáo sai sự thật có chủ đích sẽ bị xử phạt khóa tài khoản người báo cáo và trừ toàn bộ điểm uy tín.', 'TRUST_SAFETY', 165, 1, NOW(), NOW()),
(166, 'Trường hợp bất khả kháng do thiên tai, dịch bệnh được xử lý ra sao?', 'Lớp học được tạm dừng và bảo lưu học phí Escrow vô thời hạn cho đến khi các bên có thể tiếp tục việc dạy học an toàn.', 'TRUST_SAFETY', 166, 1, NOW(), NOW()),
(167, 'Gia sư có được dạy kèm cùng lúc học sinh của trường mình đang công tác không?', 'Gia sư là giáo viên trường công lập cần tuân thủ các quy định hiện hành của Bộ Giáo dục và Đào tạo về việc dạy thêm ngoài nhà trường.', 'TRUST_SAFETY', 167, 1, NOW(), NOW()),
(168, 'Hệ thống tự động phát hiện vi phạm lách sàn bằng công nghệ gì?', 'TCS tích hợp thuật toán AI quét tự động các tin nhắn chứa số điện thoại, số tài khoản ngân hàng hoặc liên kết giao dịch ngoài sàn.', 'TRUST_SAFETY', 168, 1, NOW(), NOW()),
(169, 'Khóa tài khoản vĩnh viễn có được tạo lại tài khoản mới không?', 'Người dùng bị khóa vĩnh viễn do vi phạm nghiêm trọng sẽ bị chặn số điện thoại, email và số CCCD trên toàn hệ thống TCS.', 'TRUST_SAFETY', 169, 1, NOW(), NOW()),
(170, 'Mức thưởng cho người dùng phát hiện và báo cáo lách sàn?', 'Người dùng báo cáo chính xác hành vi gian lận sẽ được tặng voucher giảm giá 50% phí dịch vụ cho lần thuê gia sư tiếp theo.', 'TRUST_SAFETY', 170, 1, NOW(), NOW()),

-- 9. PLATFORM_ADMIN (171 to 195)
(171, 'Bảng điều khiển quản trị Admin Dashboard nằm ở đâu?', 'Quản trị viên nền tảng truy cập trang /platform để theo dõi tổng quan các chỉ số người dùng, lớp học, doanh thu và các tác vụ cần xử lý.', 'PLATFORM_ADMIN', 171, 1, NOW(), NOW()),
(172, 'Xem báo cáo doanh thu và phân tích dòng tiền ở đâu?', 'Báo cáo doanh thu và tài chính chi tiết được hiển thị tại trang /platform/analytics với biểu đồ dòng tiền vào (Money In) và dòng tiền ra (Money Out).', 'PLATFORM_ADMIN', 172, 1, NOW(), NOW()),
(173, 'Hàng đợi duyệt xác minh hồ sơ (Verification Queue) ở đâu?', 'Admin vào mục /platform/verifications để xem danh sách gia sư và trung tâm đang chờ xét duyệt bằng cấp và căn cước công dân.', 'PLATFORM_ADMIN', 173, 1, NOW(), NOW()),
(174, 'Quản lý và phê duyệt yêu cầu rút tiền ở đâu?', 'Admin truy cập mục /platform/withdrawals để kiểm tra, duyệt lệnh chuyển tiền ngân hàng hoặc từ chối các yêu cầu rút tiền không hợp lệ.', 'PLATFORM_ADMIN', 174, 1, NOW(), NOW()),
(175, 'Quản lý các vụ tranh chấp lớp học ở đâu?', 'Admin truy cập mục /platform/disputes để xem các khiếu nại, đối soát bằng chứng từ hai bên và thực hiện phân chia tiền hoàn Escrow.', 'PLATFORM_ADMIN', 175, 1, NOW(), NOW()),
(176, 'Xem nhật ký hoạt động hệ thống (Audit Log) ở đâu?', 'Toàn bộ nhật ký thao tác đăng nhập, chỉnh sửa dữ liệu, phân quyền và giao dịch được ghi lại minh bạch tại /platform.', 'PLATFORM_ADMIN', 176, 1, NOW(), NOW()),
(177, 'Lọc các tác vụ quá hạn cam kết SLA như thế nào?', 'Tại bảng điều khiển /platform, Admin có thể lọc các ticket hỗ trợ hoặc hồ sơ xác minh vượt quá 24h để ưu tiên xử lý khẩn cấp.', 'PLATFORM_ADMIN', 177, 1, NOW(), NOW()),
(178, 'Tính năng Reindex cơ sở tri thức AI hoạt động thế nào?', 'Admin vào /platform/analytics, bấm "Reindex AI Knowledge Base" để đồng bộ dữ liệu FAQ và tài liệu mới vào vector database của AI Assistant.', 'PLATFORM_ADMIN', 178, 1, NOW(), NOW()),
(179, 'Cấu hình tỷ lệ phí nền tảng (Platform Fee Rate) ở đâu?', 'Admin có thể điều chỉnh tỷ lệ phí sàn tại mục Cấu hình hệ thống trong trang quản trị /platform.', 'PLATFORM_ADMIN', 179, 1, NOW(), NOW()),
(180, 'Xuất báo cáo dữ liệu dạng file CSV ở đâu?', 'Tại các bảng quản lý người dùng, giao dịch và lớp học trên /platform đều có nút "Export CSV" để tải về báo cáo đối soát.', 'PLATFORM_ADMIN', 180, 1, NOW(), NOW()),
(181, 'Phân quyền tài khoản quản trị viên phụ (Moderator) thế nào?', 'Super Admin có thể cấp quyền kiểm duyệt hồ sơ, xử lý ticket hoặc xem báo cáo tài chính cho từng nhân sự tại /platform.', 'PLATFORM_ADMIN', 181, 1, NOW(), NOW()),
(182, 'Quản lý danh mục môn học và khối lớp tại đâu?', 'Admin có thể thêm mới hoặc ẩn các môn học, chuyên đề đào tạo trong mục Quản lý Danh mục tại trang /platform.', 'PLATFORM_ADMIN', 182, 1, NOW(), NOW()),
(183, 'Xử lý khi hệ thống ngân hàng bị lỗi cổng thanh toán?', 'Admin có thể tạm ngưng tính năng nạp rút tự động và chuyển sang chế độ đối soát thủ công để đảm bảo không thất thoát dòng tiền.', 'PLATFORM_ADMIN', 183, 1, NOW(), NOW()),
(184, 'Xem số lượng người dùng đang trực tuyến (Real-time Active Users)?', 'Biểu đồ người dùng thời gian thực được hiển thị trên bảng thống kê giám sát vận hành tại /platform/analytics.', 'PLATFORM_ADMIN', 184, 1, NOW(), NOW()),
(185, 'Khóa tài khoản vi phạm từ trang quản trị như thế nào?', 'Trong danh sách người dùng tại /platform, tìm kiếm tài khoản, chọn "Khóa tài khoản", nhập lý do và thời gian khóa.', 'PLATFORM_ADMIN', 185, 1, NOW(), NOW()),
(186, 'Xem tổng giá trị học phí đang ký quỹ (Total Escrow Balance)?', 'Tổng tiền học phí đang được bảo lưu an toàn trong hệ thống được cập nhật thời gian thực tại chỉ số "Escrow Balance" trên /platform.', 'PLATFORM_ADMIN', 186, 1, NOW(), NOW()),
(187, 'Theo dõi tỷ lệ hoàn thành lớp học (Completion Rate)?', 'Tỷ lệ lớp học hoàn tất thành công trên tổng số lớp mở được biểu diễn dạng biểu đồ hình tròn tại /platform/analytics.', 'PLATFORM_ADMIN', 187, 1, NOW(), NOW()),
(188, 'Cấu hình ngưỡng cảnh báo gian lận tự động?', 'Admin có thể đặt các ngưỡng cảnh báo như: Số lần nhập sai OTP, số tin nhắn chứa số điện thoại hoặc tần suất tạo lớp bất thường.', 'PLATFORM_ADMIN', 188, 1, NOW(), NOW()),
(189, 'Kiểm tra độ chính xác và phản hồi của AI Assistant?', 'Admin theo dõi tỷ lệ phân loại Intent đúng, số lượt người dùng đánh giá câu trả lời hữu ích tại tab Giám sát AI trên /platform/analytics.', 'PLATFORM_ADMIN', 189, 1, NOW(), NOW()),
(190, 'Sao lưu cơ sở dữ liệu hệ thống định kỳ thế nào?', 'Hệ thống tự động thực hiện snapshot sao lưu dữ liệu mỗi ngày vào 02:00 sáng và lưu trữ an toàn tại kho lưu trữ đám mây.', 'PLATFORM_ADMIN', 190, 1, NOW(), NOW()),
(191, 'Xem danh sách các gia sư có điểm uy tín thấp cần kiểm tra?', 'Bộ lọc tại /platform/verifications cho phép lọc các gia sư có điểm đánh giá dưới 3.5 sao để bộ phận CSKH liên hệ hỗ trợ.', 'PLATFORM_ADMIN', 191, 1, NOW(), NOW()),
(192, 'Quản lý và cập nhật nội dung các bài viết FAQ ở đâu?', 'Admin có thể thêm mới, sửa đổi hoặc xóa các bài hỏi đáp tri thức tại mục Quản lý FAQ trên trang /platform.', 'PLATFORM_ADMIN', 192, 1, NOW(), NOW()),
(193, 'Quy trình xử lý hoàn tiền thủ công cho học viên?', 'Trong trường hợp đặc biệt, Admin có thể phê duyệt lệnh hoàn tiền trực tiếp từ tài khoản Escrow về ví học viên tại /platform/disputes.', 'PLATFORM_ADMIN', 193, 1, NOW(), NOW()),
(194, 'Gửi thông báo toàn hệ thống (Broadcast Notification)?', 'Admin có thể soạn thảo và gửi thông báo chung đến toàn bộ Học viên, Gia sư hoặc Trung tâm trong mục Thông báo hệ thống.', 'PLATFORM_ADMIN', 194, 1, NOW(), NOW()),
(195, 'Theo dõi thời gian phản hồi trung bình của máy chủ (Server Latency)?', 'Chỉ số sức khỏe hạ tầng hệ thống, thời gian phản hồi API và mức tải CPU được giám sát liên tục tại /platform.', 'PLATFORM_ADMIN', 195, 1, NOW(), NOW()),

-- 10. GENERAL (196 to 205)
(196, 'TCS là gì và sứ mệnh kết nối của nền tảng?', 'Tutor Connect System (TCS) là nền tảng công nghệ giáo dục hàng đầu kết nối học viên, phụ huynh với gia sư chất lượng cao và trung tâm uy tín tại Việt Nam.', 'GENERAL', 196, 1, NOW(), NOW()),
(197, 'Hệ thống TCS hỗ trợ những môn học và cấp học nào?', 'TCS hỗ trợ đầy đủ các môn từ Lớp 1 đến Lớp 12, Luyện thi Chuyên, Luyện thi Đại học, Ngoại ngữ (IELTS/TOEIC/Tiếng Nhật/Hàn/Trung), Lập trình và Năng khiếu.', 'GENERAL', 197, 1, NOW(), NOW()),
(198, 'Trung tâm trợ giúp và các kênh liên hệ hỗ trợ TCS?', 'Bạn có thể tìm câu trả lời tại /help, tạo phiếu hỗ trợ tại /support/tickets, gửi email về support@tcs.edu.vn hoặc gọi hotline 1900-XXXX.', 'GENERAL', 198, 1, NOW(), NOW()),
(199, 'Tại sao nên chọn thuê gia sư qua nền tảng TCS?', 'TCS mang lại 3 giá trị vượt trội: (1) 100% gia sư được xác minh bằng cấp/CCCD; (2) Học phí được bảo vệ an toàn qua Escrow; (3) Hợp đồng điện tử minh bạch.', 'GENERAL', 199, 1, NOW(), NOW()),
(200, 'Trợ lý AI của TCS có thể giúp gì cho người dùng?', 'AI Assistant của TCS hỗ trợ gợi ý gia sư phù hợp, tìm kiếm lớp học, giải đáp bài tập Toán/Anh, hướng dẫn quy trình hệ thống và phục vụ 24/7.', 'GENERAL', 200, 1, NOW(), NOW()),
(201, 'Ứng dụng TCS có dùng được trên điện thoại không?', 'Giao diện web của TCS được thiết kế chuẩn Responsive, hoạt động mượt mà trên tất cả các trình duyệt di động iOS, Android và máy tính bảng.', 'GENERAL', 201, 1, NOW(), NOW()),
(202, 'Chính sách bảo mật quyền riêng tư của TCS thế nào?', 'TCS cam kết tuân thủ nghiêm ngặt các quy định về bảo vệ dữ liệu cá nhân theo Nghị định 13/2023/NĐ-CP của Chính phủ.', 'GENERAL', 202, 1, NOW(), NOW()),
(203, 'Làm thế nào để đóng góp ý kiến cải tiến tính năng?', 'Bạn có thể gửi phản hồi và đề xuất cải tiến tại mục "Góp ý sản phẩm" trong trang /support/tickets để nhận quà tri ân từ TCS.', 'GENERAL', 203, 1, NOW(), NOW()),
(204, 'TCS có cung cấp tài liệu ôn tập và đề thi thử miễn phí không?', 'Có, học viên và gia sư có thể truy cập kho tài nguyên học tập phong phú hoàn toàn miễn phí tại mục Thư viện đề thi.', 'GENERAL', 204, 1, NOW(), NOW()),
(205, 'Thời gian làm việc của đội ngũ hỗ trợ khách hàng TCS?', 'Bộ phận Hỗ trợ khách hàng TCS làm việc từ 08:00 đến 22:00 tất cả các ngày trong tuần (kể cả Thứ Bảy, Chủ Nhật và ngày Lễ).', 'GENERAL', 205, 1, NOW(), NOW());

-- =====================================================================
-- PHẦN 3: TÀI KHOẢN ADMIN HỆ THỐNG (thanhkiu0209@gmail.com / 12345678)
-- =====================================================================
INSERT INTO users (email, password_hash, status, created_at, updated_at) 
VALUES (
    'thanhkiu0209@gmail.com', 
    '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 
    'ACTIVE', 
    NOW(), 
    NOW()
)
ON DUPLICATE KEY UPDATE 
    password_hash = '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW',
    status = 'ACTIVE',
    updated_at = NOW();

INSERT INTO platform_admins (user_id, full_name)
SELECT user_id, 'Quản Trị Viên Hệ Thống' 
FROM users 
WHERE email = 'thanhkiu0209@gmail.com'
ON DUPLICATE KEY UPDATE full_name = 'Quản Trị Viên Hệ Thống';

-- =====================================================================
-- PHẦN 4: 20 HỒ SƠ GIA SƯ ĐẦY ĐỦ MÔN (MẬT KHẨU: 123@123a)
-- =====================================================================
INSERT IGNORE INTO users (email, phone, password_hash, status, created_at, updated_at) VALUES
    ('tutor.toan1@tcs.com', '0981000001', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.toan2@tcs.com', '0981000002', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.ly1@tcs.com', '0981000003', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.ly2@tcs.com', '0981000004', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.hoa1@tcs.com', '0981000005', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.hoa2@tcs.com', '0981000006', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.van1@tcs.com', '0981000007', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.van2@tcs.com', '0981000008', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.anh1@tcs.com', '0981000009', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.anh2@tcs.com', '0981000010', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.anh3@tcs.com', '0981000011', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.han1@tcs.com', '0981000012', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.han2@tcs.com', '0981000013', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.trung1@tcs.com', '0981000014', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.trung2@tcs.com', '0981000015', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.tin1@tcs.com', '0981000016', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.tin2@tcs.com', '0981000017', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.tieuhoc1@tcs.com', '0981000018', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.sinh1@tcs.com', '0981000019', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW()),
    ('tutor.sudia1@tcs.com', '0981000020', '$2a$10$HepRyX1MtX1rwgzMnC6nZenl7rsWrrK.OT05NSX1C9Rnb.IzntPKW', 'ACTIVE', NOW(), NOW());

-- 4.1 Toán 1 (Hà Nội - ĐH Sư Phạm - Cấp 3)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Nguyễn Thành Long', 'MALE', u.phone, 'Cầu Giấy, Hà Nội', 6,
       'Cử nhân Sư phạm Toán ĐH Sư Phạm Hà Nội, 6 năm kinh nghiệm luyện thi vào 10 và THPT Quốc gia điểm 8+, 9+. Chuyên dạy Toán hình, Đại số nâng cao cho học sinh mất gốc và bồi dưỡng HSG cấp tỉnh. Nhận dạy khu vực Cầu Giấy, Nam Từ Liêm và Online.',
       250000.00, 4.95, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.toan1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.2 Toán 2 (TP.HCM - Bách Khoa - Cấp 2 & 3)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Lê Hoàng Nam', 'MALE', u.phone, 'Quận 10, TP.HCM', 4,
       'Tốt nghiệp ĐH Bách Khoa TP.HCM, thủ khoa khối A tỉnh Lâm Đồng. 4 năm kinh nghiệm gia sư Toán 9, Toán 10, 11, 12. Phương pháp giải nhanh trắc nghiệm máy tính Casio, tư duy logic bản chất. Dạy tại Quận 10, Quận 3, Quận 1 và Online.',
       200000.00, 4.85, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.toan2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.3 Vật lý 1 (Hà Nội - KHTN - Luyện thi ĐH)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Vũ Minh Tuấn', 'MALE', u.phone, 'Thanh Xuân, Hà Nội', 5,
       'Cử nhân Vật lý ĐH Khoa học Tự nhiên - ĐHQGHN, Giải Nhì Vật lý Quốc gia. 5 năm dạy Vật lý lớp 10, 11, 12 và luyện thi Đại học khối A. Chuyên đề Dao động cơ, Sóng cơ, Dòng điện xoay chiều, Vật lý hạt nhân. Khu vực Thanh Xuân, Đống Đa, Hà Đông.',
       220000.00, 4.90, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.ly1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.4 Vật lý 2 (TP.HCM - Sư Phạm Lý)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Đặng Thu Thảo', 'FEMALE', u.phone, 'Bình Thạnh, TP.HCM', 3,
       'Cử nhân Sư phạm Vật lý ĐH Sư Phạm TP.HCM, giáo viên nhiệt huyết, kiên nhẫn. 3 năm kèm Vật lý lớp 8, 9, 10, 11 cho học sinh mất căn bản lấy lại điểm 8+. Nhận dạy tại Bình Thạnh, Phú Nhuận, Gò Vấp.',
       180000.00, 4.80, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.ly2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.5 Hóa học 1 (Hà Nội - ĐH Dược)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Phạm Đức Minh', 'MALE', u.phone, 'Hoàn Kiếm, Hà Nội', 7,
       'Thạc sĩ ĐH Dược Hà Nội, cựu chuyên Hóa Amsterdam. 7 năm chuyên luyện thi Hóa THPTQG điểm 9+ xét tuyển Y Đa khoa, Dược. Nắm vững phương pháp bảo toàn electron, bảo toàn khối lượng, bài toán este - peptit nâng cao. Khu vực Hoàn Kiếm, Hai Bà Trưng.',
       300000.00, 4.98, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.hoa1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.6 Hóa học 2 (TP.HCM - KHTN)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Bùi Bích Ngọc', 'FEMALE', u.phone, 'Quận 7, TP.HCM', 4,
       'Cử nhân Hóa học ĐH Khoa học Tự nhiên TP.HCM. Chuyên dạy Hóa THCS lớp 8, 9 ôn thi vào 10 và Hóa 10, 11 cơ bản. Hướng dẫn viết phương trình, giải bài toán vô cơ chi tiết dễ hiểu. Khu vực Quận 7, Quận 4, Nhà Bè.',
       200000.00, 4.75, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.hoa2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.7 Ngữ văn 1 (Hà Nội - Sư Phạm Văn)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Trần Thị Thu Hà', 'FEMALE', u.phone, 'Đống Đa, Hà Nội', 8,
       'Thạc sĩ Văn học ĐH Sư Phạm Hà Nội, 8 năm kinh nghiệm dạy Ngữ văn lớp 9 thi vào 10 và lớp 12 luyện thi tốt nghiệp THPT/Đại học. Rèn kỹ năng viết Nghị luận xã hội sắc bén, phân tích tác phẩm văn học đạt 8.5+. Khu vực Đống Đa, Ba Đình, Cầu Giấy.',
       280000.00, 4.95, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.van1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.8 Ngữ văn 2 (TP.HCM - KHXH&NV)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Nguyễn Mai Phương', 'FEMALE', u.phone, 'Tân Bình, TP.HCM', 5,
       'Cử nhân Văn học ĐH KHXH&NV TP.HCM. 5 năm kèm Văn cấp 2 (lớp 6, 7, 8, 9) và luyện thi vào lớp 10 trường công lập. Phương pháp học Văn qua sơ đồ tư duy, rèn chữ và chính tả. Khu vực Tân Bình, Phú Nhuận, Quận 3.',
       200000.00, 4.80, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.van2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.9 Tiếng Anh 1 - IELTS (Hà Nội - FTU - IELTS 8.0)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Phạm Quỳnh Anh', 'FEMALE', u.phone, 'Cầu Giấy, Hà Nội', 5,
       'Cử nhân Kinh tế đối ngoại ĐH Ngoại Thương, chứng chỉ IELTS 8.0 (Speaking 8.5, Writing 8.0). 5 năm kinh nghiệm luyện thi IELTS Academic mục tiêu 6.5 - 7.5+, củng cố ngữ pháp và phát âm chuẩn bản ngữ. Dạy tại Cầu Giấy, Nam Từ Liêm hoặc Online qua Zoom.',
       350000.00, 5.00, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.anh1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.10 Tiếng Anh 2 - Phổ thông (TP.HCM - Sư Phạm Anh)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Hoàng Trọng Nghĩa', 'MALE', u.phone, 'Quận 1, TP.HCM', 6,
       'Cử nhân Sư phạm Tiếng Anh ĐH Sư Phạm TP.HCM, chứng chỉ C1 CEFR. 6 năm dạy kèm Tiếng Anh tăng cường lớp 6-12, ôn thi vào lớp 10 chuyên Anh và luyện thi THPTQG. Khu vực Quận 1, Quận 3, Quận 4.',
       250000.00, 4.88, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.anh2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.11 Tiếng Anh 3 - Trẻ em & TOEIC (TP.HCM - RMIT)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Đỗ Ngọc Hân', 'FEMALE', u.phone, 'Quận 7, TP.HCM', 3,
       'Tốt nghiệp ĐH RMIT Việt Nam, TOEIC 950, chứng chỉ giảng dạy quốc tế TESOL. Chuyên dạy Tiếng Anh tiểu học Cambridge (Starters, Movers, Flyers) và tiếng Anh giao tiếp phản xạ cho người mất gốc. Khu vực Quận 7, TP. Thủ Đức và Online.',
       220000.00, 4.82, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.anh3@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.12 Tiếng Hàn 1 - TOPIK 6 (TP.HCM)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Nguyễn Minh Thư', 'FEMALE', u.phone, 'Quận 7, TP.HCM', 5,
       'Tốt nghiệp khoa Hàn Quốc học ĐH KHXH&NV TP.HCM, chứng chỉ TOPIK 6 (cấp cao nhất). 5 năm kinh nghiệm dạy Tiếng Hàn sơ cấp, trung cấp, luyện thi TOPIK I & TOPIK II và tiếng Hàn thương mại cho người đi làm. Nhận dạy tại Quận 7, Quận 1 và Online.',
       300000.00, 4.92, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.han1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.13 Tiếng Hàn 2 - Giao tiếp (Hà Nội)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Park Sung-Min', 'MALE', u.phone, 'Cầu Giấy, Hà Nội', 4,
       'Cử nhân Ngôn ngữ Hàn ĐH Ngoại Ngữ - ĐHQGHN, 2 năm trao đổi tại ĐH Yonsei Seoul. 4 năm kinh nghiệm dạy phát âm chuẩn Seoul, giao tiếp thực tế cho người đi làm và phỏng vấn visa du học Hàn Quốc. Khu vực Cầu Giấy, Thanh Xuân.',
       260000.00, 4.85, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.han2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.14 Tiếng Trung 1 - HSK 6 (TP.HCM)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Trần Quốc Huy', 'MALE', u.phone, 'Quận 5, TP.HCM', 6,
       'Cử nhân Sư phạm tiếng Trung ĐH Sư Phạm TP.HCM, chứng chỉ HSK 6 (285/300) và HSKK Cao cấp. 6 năm dạy Tiếng Trung giao tiếp, chữ Hán phồn thể/giản thể, luyện thi HSK 3, 4, 5, 6 và chứng chỉ TOCFL. Dạy tại Quận 5, Quận 10, Quận 11 và Online.',
       300000.00, 4.95, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.trung1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.15 Tiếng Trung 2 - Sơ cấp (Hà Nội)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Lý Ngọc Diệp', 'FEMALE', u.phone, 'Ba Đình, Hà Nội', 3,
       'Tốt nghiệp khoa Tiếng Trung ĐH Hà Nội (HANU), chứng chỉ HSK 5. 3 năm kèm tiếng Trung cho người mới bắt đầu từ con số 0, tiếng Trung bán hàng, du lịch và order hàng Taobao. Dạy tại Ba Đình, Đống Đa, Hoàn Kiếm.',
       200000.00, 4.78, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.trung2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.16 Tin học 1 - Python & HSG (Hà Nội)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Hoàng Anh Đức', 'MALE', u.phone, 'Cầu Giấy, Hà Nội', 4,
       'Kỹ sư Khoa học máy tính ĐH Công nghệ - ĐHQGHN, Giải Nhì Olympic Tin học Sinh viên. 4 năm kinh nghiệm dạy lập trình Python, C++, Pascal cho học sinh THCS, THPT ôn thi Học sinh giỏi Tin và thuật toán cơ bản. Khu vực Cầu Giấy, Nam Từ Liêm hoặc Online 1-1.',
       300000.00, 4.94, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.tin1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.17 Tin học 2 - Scratch & Web (TP.HCM)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Lê Quốc Tuấn', 'MALE', u.phone, 'Thủ Đức, TP.HCM', 3,
       'Kỹ sư Phần mềm ĐH Bách Khoa TP.HCM. 3 năm dạy lập trình Scratch, Game 2D, lập trình Web frontend (HTML, CSS, JavaScript) cho học sinh tiểu học và cấp 2. Phương pháp học qua dự án vui vẻ, sáng tạo. Khu vực TP. Thủ Đức, Bình Thạnh.',
       220000.00, 4.80, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.tin2@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.18 Tiếng Việt Tiểu học (Hà Nội)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Nguyễn Thị Lan Anh', 'FEMALE', u.phone, 'Hà Đông, Hà Nội', 6,
       'Cử nhân Giáo dục Tiểu học ĐH Sư Phạm Hà Nội, chứng nhận Giáo viên dạy giỏi cấp Quận. 6 năm chuyên rèn chữ đẹp, tập đọc, chính tả và môn Tiếng Việt cho học sinh chuẩn bị vào lớp 1, lớp 2, 3, 4, 5. Phương pháp sư phạm chuẩn, kiên nhẫn, yêu trẻ. Khu vực Hà Đông, Thanh Xuân.',
       180000.00, 4.96, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.tieuhoc1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.19 Sinh học (TP.HCM)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Thầy Võ Trọng Nhân', 'MALE', u.phone, 'Quận 5, TP.HCM', 5,
       'Thạc sĩ Di truyền học ĐH Sư Phạm TP.HCM. 5 năm chuyên luyện thi môn Sinh học THPTQG xét tuyển khối B (Y Đa khoa, Răng Hàm Mặt, Dược). Chuyên sâu Di truyền học phân tử, Di truyền quần thể, Phả hệ, Sinh thái học. Khu vực Quận 5, Quận 10, Tân Bình.',
       250000.00, 4.89, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.sinh1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- 4.20 Lịch sử & Địa lý (Hà Nội)
INSERT INTO tutors (user_id, full_name, gender, phone, address, experience_years, bio, hourly_rate, rating_avg, verification_status, created_at, updated_at)
SELECT u.user_id, 'Cô Đặng Thùy Dung', 'FEMALE', u.phone, 'Hai Bà Trưng, Hà Nội', 4,
       'Cử nhân Sư phạm Lịch sử & Địa lý ĐH Sư Phạm Hà Nội. 4 năm kèm môn Lịch sử và Địa lý lớp 9 thi vào 10, lớp 12 ôn thi khối C (C00) đạt điểm 9+. Phương pháp ghi nhớ mốc lịch sử qua sơ đồ thời gian, đọc Atlat Địa lý thành thạo. Khu vực Hai Bà Trưng, Hoàn Kiếm.',
       180000.00, 4.85, 'VERIFIED', NOW(), NOW()
FROM users u WHERE u.email = 'tutor.sudia1@tcs.com'
  AND NOT EXISTS (SELECT 1 FROM tutors t WHERE t.user_id = u.user_id);

-- =====================================================================
-- PHẦN 5: 5 TÀI KHOẢN PHỤ HUYNH & 30 LỚP HỌC MỞ (OPEN)
-- =====================================================================
INSERT IGNORE INTO users (email, phone, password_hash, status, created_at, updated_at) VALUES
    ('client.hanoi1@tcs.com', '0971000001', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('client.hanoi2@tcs.com', '0971000002', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('client.hcm1@tcs.com', '0971000003', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('client.hcm2@tcs.com', '0971000004', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW()),
    ('client.danang1@tcs.com', '0971000005', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5VpU/8.s8aA7.Q2.bQjFp5hE3aH2u', 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Lê Thu Trang', u.phone, 'Số 25 Hoàng Quốc Việt, Cầu Giấy, Hà Nội'
FROM users u WHERE u.email = 'client.hanoi1@tcs.com';

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Nguyễn Văn Đức', u.phone, 'Ngõ 119 Tây Sơn, Đống Đa, Hà Nội'
FROM users u WHERE u.email = 'client.hanoi2@tcs.com';

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Trần Minh Tâm', u.phone, '120 Nguyễn Thị Minh Khai, Quận 3, TP.HCM'
FROM users u WHERE u.email = 'client.hcm1@tcs.com';

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Hoàng Kim Oanh', u.phone, 'Khu đô thị Phú Mỹ Hưng, Quận 7, TP.HCM'
FROM users u WHERE u.email = 'client.hcm2@tcs.com';

INSERT IGNORE INTO clients (user_id, full_name, phone, address)
SELECT u.user_id, 'Phụ Huynh Đặng Quốc Bảo', u.phone, '54 Nguyễn Văn Linh, Hải Châu, Đà Nẵng'
FROM users u WHERE u.email = 'client.danang1@tcs.com';

-- 30 Lớp học
INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm gia sư Toán 12 kèm 1-1 luyện thi Đại học điểm 9+',
       'Cần tìm thầy/cô kèm môn Toán cho học sinh lớp 12 trường THPT Cầu Giấy. Mục tiêu xét tuyển ĐH Ngoại Thương khối A00. Yêu cầu nắm chắc chuyên đề Hàm số, Oxyz, Số phức và Tích phân nâng cao.',
       'Đạt điểm 9+ môn Toán THPTQG', 'Gia sư Sư phạm hoặc Bách Khoa có kinh nghiệm ôn thi ĐH',
       'Số 18 Duy Tân, Cầu Giấy, Hà Nội', 'OFFLINE', 15, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 60 DAY,
       3750000.00, 4000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Toán' AND g.grade_name = 'Lớp 12'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm gia sư Toán 12 kèm 1-1 luyện thi Đại học điểm 9+');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Cần gia sư Toán lớp 9 ôn thi tuyển sinh vào lớp 10',
       'Bé hiện học lực khá nhưng yếu phần Hình học không gian và bài toán thực tế. Cần gia sư dạy 2 buổi/tuần vào thứ 3 và thứ 6 lúc 18h30.',
       'Đỗ nguyện vọng 1 THPT Nguyễn Thị Minh Khai', 'Sinh viên Bách Khoa/Sư phạm hoặc Giáo viên kiên nhẫn',
       '120 Nguyễn Thị Minh Khai, Quận 3, TP.HCM', 'OFFLINE', 12, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 50 DAY,
       2400000.00, 2600000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Toán' AND g.grade_name = 'Lớp 9'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Cần gia sư Toán lớp 9 ôn thi tuyển sinh vào lớp 10');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Lớp kèm 1-1 IELTS Academic nâng band Speaking & Writing lên 7.5',
       'Học viên là sinh viên năm 3 cần bằng IELTS để apply học bổng du học. Hiện band 6.5, cần sửa bài Writing Task 1 & 2 chuyên sâu và luyện phản xạ Speaking các chủ đề khó.',
       'IELTS Overall 7.5 (Speaking 7.5, Writing 7.0)', 'Gia sư IELTS 8.0+ có kinh nghiệm sửa bài chi tiết',
       'Học trực tuyến qua Zoom/Google Meet', 'ONLINE', 16, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 45 DAY,
       5600000.00, 6000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Tiếng Anh' AND g.grade_name = 'Luyện thi chứng chỉ (IELTS, TOEIC...)'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Lớp kèm 1-1 IELTS Academic nâng band Speaking & Writing lên 7.5');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Cần tìm gia sư dạy Tiếng Hàn ôn thi TOPIK II cấp 4',
       'Đã hoàn thành sơ cấp TOPIK 2, cần học lên trung cấp để thi TOPIK 4 phục vụ công việc tại công ty Hàn Quốc. Cần gia sư dạy chắc ngữ pháp trung cấp và kỹ năng viết câu 53, 54.',
       'Đạt chứng chỉ TOPIK cấp 4 trong 4 tháng', 'Gia sư tốt nghiệp khoa Tiếng Hàn hoặc chứng chỉ TOPIK 5, 6',
       'Chung cư Sunrise City, Nguyễn Hữu Thọ, Quận 7, TP.HCM', 'OFFLINE', 20, CURDATE() + INTERVAL 4 DAY, CURDATE() + INTERVAL 70 DAY,
       6000000.00, 6500000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm2@tcs.com' AND s.subject_name = 'Tiếng Hàn' AND g.grade_name = 'Luyện thi chứng chỉ (IELTS, TOEIC...)'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Cần tìm gia sư dạy Tiếng Hàn ôn thi TOPIK II cấp 4');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm gia sư Tiếng Trung giao tiếp và luyện thi HSK 4',
       'Học viên đi làm ngành xuất nhập khẩu cần nâng cao vốn từ vựng thương mại, giao tiếp tự tin với đối tác Trung Quốc và thi chứng chỉ HSK 4.',
       'Giao tiếp lưu loát và đỗ HSK 4', 'Gia sư HSK 5-6 phát âm chuẩn Bắc Kinh',
       'Ngõ 119 Tây Sơn, Đống Đa, Hà Nội', 'HYBRID', 18, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 60 DAY,
       4500000.00, 5000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Tiếng Trung' AND g.grade_name = 'Luyện thi chứng chỉ (IELTS, TOEIC...)'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm gia sư Tiếng Trung giao tiếp và luyện thi HSK 4');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư kèm Lập trình Python & Thuật toán C++ ôn thi HSG',
       'Học sinh lớp 8 trường Chuyên Amsterdam đam mê Tin học, cần học cấu trúc dữ liệu và giải thuật (Quy hoạch động, Đồ thị, Cây) để thi Học sinh giỏi Tin cấp Thành phố.',
       'Đạt giải HSG môn Tin học', 'Sinh viên ngành CNTT ĐH Công nghệ hoặc Bách Khoa có giải Quốc gia',
       'KĐT Mỹ Đình 2, Nam Từ Liêm, Hà Nội', 'OFFLINE', 16, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 50 DAY,
       4800000.00, 5000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Tin học' AND g.grade_name = 'Lớp 8'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư kèm Lập trình Python & Thuật toán C++ ôn thi HSG');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Lớp Lập trình Scratch sáng tạo Game 2D cho học sinh lớp 4',
       'Bé thích chơi game và máy tính, phụ huynh muốn định hướng tư duy logic lập trình từ nhỏ thông qua ngôn ngữ kéo thả Scratch 3.0.',
       'Tự lập trình được 3 game mini', 'Gia sư trẻ trung, nhiệt tình, có kỹ năng sư phạm dạy trẻ em',
       'Học trực tuyến qua Google Meet', 'ONLINE', 10, CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 30 DAY,
       2000000.00, 2200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Tin học' AND g.grade_name = 'Lớp 4'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Lớp Lập trình Scratch sáng tạo Game 2D cho học sinh lớp 4');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Tìm cô giáo kèm Ngữ văn 12 ôn thi Tốt nghiệp THPT và Đại học',
       'Học sinh học khối D, hiện môn Văn điểm trung bình 6.5. Cần cô giáo rèn phương pháp mở bài, kết bài sáng tạo và luận điểm sâu sắc cho các tác phẩm trọng tâm (Vợ chồng A Phủ, Người lái đò sông Đà, Đất Nước).',
       'Đạt 8.5+ môn Văn thi Đại học', 'Cô giáo hoặc Thạc sĩ Sư phạm Ngữ văn',
       'Số 45 Kim Mã, Ba Đình, Hà Nội', 'OFFLINE', 12, CURDATE() + INTERVAL 2 DAY, CURDATE() + INTERVAL 45 DAY,
       3000000.00, 3200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi1@tcs.com' AND s.subject_name = 'Ngữ văn' AND g.grade_name = 'Lớp 12'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Tìm cô giáo kèm Ngữ văn 12 ôn thi Tốt nghiệp THPT và Đại học');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Cần gia sư Vật lý lớp 11 kèm tại nhà 2 buổi/tuần',
       'Học sinh cần củng cố kiến thức học kỳ 1 môn Vật lý 11 chương Điện từ học và Quang hình học để chuẩn bị cho kỳ thi học kỳ và định hướng thi khối A1.',
       'Nắm chắc lý thuyết và giải thành thạo bài tập 8+', 'Sinh viên giỏi Sư phạm Lý hoặc Bách Khoa',
       'Đường D2 (Nguyễn Gia Trí), Bình Thạnh, TP.HCM', 'OFFLINE', 14, CURDATE() + INTERVAL 3 DAY, CURDATE() + INTERVAL 50 DAY,
       2800000.00, 3000000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hcm1@tcs.com' AND s.subject_name = 'Vật lý' AND g.grade_name = 'Lớp 11'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Cần gia sư Vật lý lớp 11 kèm tại nhà 2 buổi/tuần');

INSERT INTO tutoring_classes (creator_id, subject_id, grade_id, title, description, learning_goal, tutor_requirement, address, lesson_mode, number_of_sessions, start_date, end_date, tuition_fee, budget, status, created_at, updated_at)
SELECT u.user_id, s.subject_id, g.grade_id,
       'Gia sư Hóa học 10 cho học sinh mới vào cấp 3',
       'Em chuyển cấp vào lớp 10 học chương trình GDPT mới còn bỡ ngỡ, cần gia sư dạy chắc lý thuyết cấu tạo nguyên tử, bảng tuần hoàn và phản ứng oxi hóa khử.',
       'Điểm trung bình môn Hóa đạt 8.0 trở lên', 'Gia sư chuyên Hóa có phương pháp giảng giải trực quan',
       'Khu tập thể Thanh Xuân Bắc, Thanh Xuân, Hà Nội', 'OFFLINE', 10, CURDATE() + INTERVAL 4 DAY, CURDATE() + INTERVAL 40 DAY,
       2000000.00, 2200000.00, 'OPEN', NOW(), NOW()
FROM users u, subjects s, grades g
WHERE u.email = 'client.hanoi2@tcs.com' AND s.subject_name = 'Hóa học' AND g.grade_name = 'Lớp 10'
  AND NOT EXISTS (SELECT 1 FROM tutoring_classes WHERE title = 'Gia sư Hóa học 10 cho học sinh mới vào cấp 3');

SET FOREIGN_KEY_CHECKS = 1;
