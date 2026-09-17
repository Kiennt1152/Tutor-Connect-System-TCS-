-- V42: Bỏ hẳn quy ước 23:59, dùng 00:00 làm mốc kết thúc NỬA ĐÊM.
--
-- Trước đây khung giờ chạm nửa đêm được lưu 23:59 rồi giao diện đổi lại thành 00:00 khi hiển
-- thị. Quy ước hai mặt đó làm backend tính thiếu 1 phút mỗi buổi (50.000đ/giờ ra 49.166,67đ).
-- Nay chỉ còn MỘT giá trị duy nhất: 00:00 = 24:00 của chính ngày hôm đó. Mọi phép so sánh và
-- tính giờ ở backend đi qua com.tcs.common.util.SlotTime.
--
-- Chỉ đổi cột/khóa GIỜ KẾT THÚC. Giờ bắt đầu 23:59 (nếu có) là mốc thật, giữ nguyên.

UPDATE schedule_slots
SET end_time = '00:00:00'
WHERE end_time = '23:59:00';

UPDATE lesson_reschedule_requests
SET new_end_time = '00:00:00'
WHERE new_end_time = '23:59:00';

-- Lịch rảnh của gia sư dùng chung kiểu dữ liệu; 23:59 ở đây cũng luôn mang nghĩa "tới hết ngày".
UPDATE tutor_availabilities
SET end_time = '00:00:00'
WHERE end_time = '23:59:00';

-- Bản sao nguyên vẹn của form lớp (slots[].end). Hai dạng chuỗi tùy bộ ghi JSON.
UPDATE tutoring_classes
SET details_json = REPLACE(details_json, '"end":"23:59"', '"end":"00:00"')
WHERE details_json LIKE '%"end":"23:59"%';

UPDATE tutoring_classes
SET details_json = REPLACE(details_json, '"end": "23:59"', '"end": "00:00"')
WHERE details_json LIKE '%"end": "23:59"%';

-- Ảnh chụp học phí trước khi ghép gia sư (V41) cũng giữ nguyên bản form, đổi kèm cho khớp.
UPDATE tutoring_classes
SET pre_match_details_json = REPLACE(pre_match_details_json, '"end":"23:59"', '"end":"00:00"')
WHERE pre_match_details_json LIKE '%"end":"23:59"%';

UPDATE tutoring_classes
SET pre_match_details_json = REPLACE(pre_match_details_json, '"end": "23:59"', '"end": "00:00"')
WHERE pre_match_details_json LIKE '%"end": "23:59"%';
