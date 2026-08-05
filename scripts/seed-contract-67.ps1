# Tạo contract test id=67
$ErrorActionPreference = 'Stop'
$Mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$Creds = "-u root -p12345"
$Db = "tutorconnectsystem"

function Run-MySql($query) {
    $tmp = [System.IO.Path]::GetTempFileName()
    Set-Content -Path $tmp -Value $query -Encoding utf8
    $content = Get-Content $tmp -Raw
    cmd /c "echo $content | `"$Mysql`" $Creds $Db" 2>&1 | Select-Object -Last 5
    Remove-Item $tmp -EA SilentlyContinue
}

Write-Host "=== Tao contract test 67 ===" -ForegroundColor Cyan

# 1. Seed data
Write-Host "Tao seed data..." -ForegroundColor Gray
& $Mysql $Creds $Db -e "INSERT IGNORE INTO categories (name, description, type, status) VALUES ('Giáo dục', 'Danh mục giáo dục', 'SYSTEM_CONFIG', 'ACTIVE');" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT IGNORE INTO subjects (subject_name, description) VALUES ('Toán', 'Môn toán học');" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT IGNORE INTO grades (grade_name) VALUES ('Lớp 10');" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT IGNORE INTO locations (address_line, province_id) VALUES ('TP. Hồ Chí Minh', 1);" 2>&1 | Out-Null

# 2. Users
Write-Host "Tao users..." -ForegroundColor Gray
& $Mysql $Creds $Db -e "INSERT IGNORE INTO users (email, phone, password_hash, status, created_at, updated_at, profile_completed_at) VALUES ('test.client67@tcs.com', '0900123001', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), NOW(), NOW());" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT IGNORE INTO users (email, phone, password_hash, status, created_at, updated_at, profile_completed_at) VALUES ('test.tutor67@tcs.com', '0900123002', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), NOW(), NOW());" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT IGNORE INTO users (email, phone, password_hash, status, created_at, updated_at, profile_completed_at) VALUES ('test.center67@tcs.com', '0900123003', '\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), NOW(), NOW());" 2>&1 | Out-Null

# 3. Profiles
Write-Host "Tao profiles..." -ForegroundColor Gray
& $Mysql $Creds $Db -e "INSERT IGNORE INTO clients (user_id, full_name, phone) SELECT user_id, 'Phụ huynh Test 67', '0900123001' FROM users WHERE email = 'test.client67@tcs.com';" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT IGNORE INTO tutors (user_id, full_name, gender, phone, experience_years, hourly_rate, rating_avg, bio) SELECT user_id, 'Gia Sư Test 67', 'MALE', '0900123002', 3, 150000.00, 4.80, 'Gia sư toán' FROM users WHERE email = 'test.tutor67@tcs.com';" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT IGNORE INTO tutor_centers (user_id, company_name, license_no, phone, address, description) SELECT user_id, 'Trung Tâm Test 67', 'TEST-TCS-67', '0900123003', 'TP.HCM', 'Trung tâm dạy thêm' FROM users WHERE email = 'test.center67@tcs.com';" 2>&1 | Out-Null

# 4. Wallets
Write-Host "Tao wallets..." -ForegroundColor Gray
& $Mysql $Creds $Db -e "INSERT IGNORE INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at) SELECT user_id, 0.00, 0.00, 'ACTIVE', NOW() FROM users WHERE email IN ('test.client67@tcs.com', 'test.tutor67@tcs.com', 'test.center67@tcs.com');" 2>&1 | Out-Null

# 5. Template
Write-Host "Tao contract template..." -ForegroundColor Gray
& $Mysql $Creds $Db -e "INSERT IGNORE INTO contract_templates (name, content, created_by, is_default, status) SELECT 'Mẫu HĐ Gia Sư 67', '<p>Hợp đồng dịch vụ gia sư tại nhà. Phí: 150,000 đ/giờ.</p>', user_id, 1, 'ACTIVE' FROM users WHERE email = 'test.center67@tcs.com';" 2>&1 | Out-Null

# 6. Tutoring class
Write-Host "Tao tutoring class..." -ForegroundColor Gray
$sql6 = "INSERT IGNORE INTO tutoring_classes (creator_id, class_type, center_id, category_id, subject_id, grade_id, title, description, location_id, lesson_mode, number_of_sessions, tuition_fee, start_date, end_date, recurring_type, status, created_at) SELECT (SELECT user_id FROM users WHERE email = 'test.center67@tcs.com'), 'CENTER', (SELECT user_id FROM users WHERE email = 'test.center67@tcs.com'), (SELECT category_id FROM categories WHERE name = 'Giáo dục'), (SELECT subject_id FROM subjects WHERE subject_name = 'Toán'), (SELECT grade_id FROM grades WHERE grade_name = 'Lớp 10'), 'Lớp Toán Lớp 10 Test 67', 'Lớp toán nâng cao cho học sinh lớp 10', (SELECT location_id FROM locations WHERE address_line = 'TP. Hồ Chí Minh'), 'OFFLINE', 8, 2000000.00, '2026-07-01', '2026-09-01', 'WEEKLY', 'OPEN', NOW();"
$sql6 | & $Mysql $Creds $Db 2>&1 | Out-Null

# 7. Class student
Write-Host "Tao class student..." -ForegroundColor Gray
$sql7 = "INSERT IGNORE INTO class_students (class_id, enrolled_by_user_id, student_name, student_phone, status) SELECT (SELECT class_id FROM tutoring_classes WHERE title = 'Lớp Toán Lớp 10 Test 67'), (SELECT user_id FROM users WHERE email = 'test.client67@tcs.com'), 'Học sinh Test 67', '0900123001', 'ENROLLED';"
$sql7 | & $Mysql $Creds $Db 2>&1 | Out-Null

# 8. Contract 67
Write-Host "Tao contract 67..." -ForegroundColor Gray
& $Mysql $Creds $Db -e "DELETE FROM contract_signatures WHERE contract_id = 67;" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "DELETE FROM contracts WHERE contract_id = 67;" 2>&1 | Out-Null

$sql8 = "INSERT INTO contracts (contract_id, contract_no, class_student_id, template_id, contract_file_url, terms_summary, status, source_type, signed_at, created_at, updated_at) SELECT 67, 'HD-2026-0067', (SELECT class_student_id FROM class_students WHERE student_name = 'Học sinh Test 67'), (SELECT template_id FROM contract_templates WHERE name = 'Mẫu HĐ Gia Sư 67'), '/uploads/contracts/HD-2026-0067.pdf', 'Hợp đồng dịch vụ gia sư tại nhà. Phí: 150,000 đ/giờ. Thời gian: 2 buổi/tuần.', 'DRAFT', 'CENTER', NULL, NOW(), NOW();"
$sql8 | & $Mysql $Creds $Db 2>&1 | Out-Null

# 9. Signatures
Write-Host "Tao signatures..." -ForegroundColor Gray
& $Mysql $Creds $Db -e "INSERT INTO contract_signatures (party_role, contract_id, signer_id, email, otp_code, otp_expires_at, otp_attempts, signed_at, signature_status) SELECT 'CLIENT', 67, user_id, email, NULL, NULL, 0, NOW(), 'PENDING' FROM users WHERE email = 'test.client67@tcs.com';" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT INTO contract_signatures (party_role, contract_id, signer_id, email, otp_code, otp_expires_at, otp_attempts, signed_at, signature_status) SELECT 'TUTOR', 67, user_id, email, NULL, NULL, 0, NOW(), 'PENDING' FROM users WHERE email = 'test.tutor67@tcs.com';" 2>&1 | Out-Null
& $Mysql $Creds $Db -e "INSERT INTO contract_signatures (party_role, contract_id, signer_id, email, otp_code, otp_expires_at, otp_attempts, signed_at, signature_status) SELECT 'CENTER', 67, user_id, email, NULL, NULL, 0, NOW(), 'PENDING' FROM users WHERE email = 'test.center67@tcs.com';" 2>&1 | Out-Null

# 10. Verify
Write-Host ""
Write-Host "=== VERIFY ===" -ForegroundColor Green
Write-Host "Contract:" -ForegroundColor White
& $Mysql $Creds $Db -e "SELECT contract_id, contract_no, status, source_type FROM contracts WHERE contract_id = 67;" 2>&1 | Select-Object -Skip 1 | Select-Object -First 3
Write-Host "Signatures:" -ForegroundColor White
& $Mysql $Creds $Db -e "SELECT cs.party_role, u.email, cs.signature_status FROM contract_signatures cs JOIN users u ON cs.signer_id = u.user_id WHERE cs.contract_id = 67;" 2>&1 | Select-Object -Skip 1 | Select-Object -First 5

Write-Host ""
Write-Host "=== TAI KHOAN TEST ===" -ForegroundColor Yellow
Write-Host "Email: test.client67@tcs.com  | Password: Test@1234  | Role: CLIENT"
Write-Host "Email: test.tutor67@tcs.com   | Password: Test@1234  | Role: TUTOR"
Write-Host "Email: test.center67@tcs.com | Password: Test@1234  | Role: TUTOR_CENTER"
Write-Host ""
Write-Host "Test URL: http://localhost:3000/contract/67" -ForegroundColor Cyan
Write-Host ""
Write-Host "Hoan tat!" -ForegroundColor Green
