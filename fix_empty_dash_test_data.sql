SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
USE tutorconnectsystem;

-- 1. Cap nhat thong tin ngan hang cho yeu cau rut tien (withdrawal_requests)
UPDATE withdrawal_requests 
SET bank_name = 'MBBank (Ngân hàng Quân đội)',
    account_no = '0987654321',
    account_holder_name = 'PHẠM THỊ DUNG'
WHERE withdrawal_id = 1;

UPDATE withdrawal_requests 
SET bank_name = 'Techcombank',
    account_no = '19036789123',
    account_holder_name = 'PHẠM THỊ DUNG'
WHERE withdrawal_id = 2;

-- 2. Cap nhat phuong thuc thanh toan vi (payment_methods)
UPDATE payment_methods 
SET account_holder_name = 'PHẠM THỊ DUNG'
WHERE payment_method_id IN (1, 2);

-- 3. Cap nhat thong tin ngan hang cho yeu cau hoan tien (refund_requests)
UPDATE refund_requests 
SET bank_name = 'Vietcombank',
    account_no = '1012345678',
    account_holder_name = 'HUỲNH ĐỨC MINH',
    refund_reference_code = 'REF-ESCROW-1',
    transfer_status = 'PENDING'
WHERE refund_id = 1;

UPDATE refund_requests 
SET bank_name = 'Techcombank',
    account_no = '1903456789',
    account_holder_name = 'NGUYỄN VĂN HÙNG',
    refund_reference_code = 'REF-ESCROW-2',
    transfer_status = 'PENDING'
WHERE refund_id = 2;

-- 4. Gan lop trung tam hop le cho giao dich ky quy (escrow_transactions)
UPDATE escrow_transactions 
SET class_student_id = 204,
    assignment_id = NULL,
    deposited_at = COALESCE(deposited_at, DATE_SUB(NOW(), INTERVAL 4 DAY))
WHERE escrow_id = 105;

-- 5. Cap nhat lop hoc muc tieu cho bao cao (reports)
UPDATE reports 
SET target_type = 'CLASS',
    target_id = 201
WHERE report_id = 1;

UPDATE reports 
SET target_type = 'CLASS',
    target_id = 202
WHERE report_id = 2;

-- 6. Chuan hoa phieu ho tro ky thuat CSKH (support_tickets)
UPDATE support_tickets 
SET assigned_admin_id = 1,
    due_at = DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE ticket_id = 2;

UPDATE support_tickets 
SET assigned_admin_id = 2,
    due_at = DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE ticket_id = 4;

UPDATE support_tickets 
SET assigned_admin_id = 1,
    due_at = DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE ticket_id = 5;

UPDATE support_tickets 
SET subject = 'Yêu cầu kiểm tra sự cố đồng bộ lịch học trên Google Calendar',
    description = 'Lịch học lớp Toán 12 của tôi không tự động đồng bộ sang Google Calendar cá nhân, xin Admin hỗ trợ kiểm tra lại API.',
    assigned_admin_id = 1,
    due_at = DATE_ADD(NOW(), INTERVAL 1 DAY)
WHERE ticket_id = 7;

UPDATE support_tickets 
SET subject = 'Khiếu nại trừ điểm uy tín gia sư không thỏa đáng',
    description = 'Hệ thống tự động trừ 5 điểm uy tín do ghi nhận vào lớp muộn, tuy nhiên tôi đã có thỏa thuận lùi giờ trước 1 tiếng với học viên.',
    assigned_admin_id = 1,
    due_at = DATE_ADD(NOW(), INTERVAL 2 DAY)
WHERE ticket_id = 8;

UPDATE support_tickets 
SET subject = 'Hỗ trợ thay đổi số tài khoản nhận học phí',
    description = 'Tôi muốn cập nhật số tài khoản ngân hàng thụ hưởng sang VietinBank do thẻ cũ sắp hết hạn.',
    assigned_admin_id = 2,
    due_at = DATE_ADD(NOW(), INTERVAL 1 DAY)
WHERE ticket_id = 9;

UPDATE support_tickets 
SET subject = 'Thắc mắc về chính sách bảo hiểm buổi học vắng mặt',
    description = 'Phụ huynh báo bận trước 2 giờ thì gia sư có được nhận 50% thù lao buổi học theo chính sách bảo lưu không?',
    assigned_admin_id = 1,
    due_at = DATE_SUB(NOW(), INTERVAL 1 HOUR)
WHERE ticket_id = 10;

UPDATE support_tickets 
SET subject = 'Báo lỗi tải tệp giáo trình PDF dung lượng lớn',
    description = 'Khi tải tệp giáo trình bài giảng 15MB lên hệ thống báo lỗi máy chủ, nhờ Admin kiểm tra giới hạn upload.',
    assigned_admin_id = 1,
    due_at = DATE_SUB(NOW(), INTERVAL 2 HOUR)
WHERE ticket_id = 11;

UPDATE support_tickets 
SET subject = 'Đề nghị thẩm định hồ sơ Trung tâm nhanh chóng',
    description = 'Trung tâm Trí Việt đã bổ sung đầy đủ giấy phép kinh doanh, nhờ Admin thẩm định sớm để kịp mở lớp mới.',
    assigned_admin_id = 1,
    due_at = DATE_SUB(NOW(), INTERVAL 3 HOUR)
WHERE ticket_id = 12;

-- 7. Cap nhat mon hoc va khoi lop cho lop hoc (tutoring_classes)
UPDATE tutoring_classes 
SET subject_id = 1, grade_id = 12 
WHERE class_id = 1;

UPDATE tutoring_classes 
SET grade_id = 12 
WHERE class_id = 101;

UPDATE tutoring_classes 
SET grade_id = 12 
WHERE class_id = 102;

UPDATE tutoring_classes 
SET grade_id = 11 
WHERE class_id = 103;

-- 8. Cap nhat ngay het han cho che tai xu phat (user_penalties)
UPDATE user_penalties 
SET expires_at = DATE_ADD(created_at, INTERVAL 30 DAY) 
WHERE expires_at IS NULL;

SELECT 'ĐÃ CẬP NHẬT HOÀN TẤT TOÀN BỘ CÁC TRƯỜNG THIẾU THÔNG TIN / DẤU GẠCH TRONG CƠ SỞ DỮ LIỆU!' AS Status;
