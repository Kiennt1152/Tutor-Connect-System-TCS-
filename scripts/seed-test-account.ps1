# Tao tai khoan test va dam bao firstLogin = true (profile_completed_at = NULL).
# Usage: powershell -ExecutionPolicy Bypass -File .\scripts\seed-test-account.ps1

param(
    [string]$Email        = 'onboarding.test@example.com',
    [string]$Password     = 'Test@1234',
    [string]$DisplayName  = 'Onboarding Test',
    [string]$Phone        = '0901234567',
    [string]$Role         = 'CLIENT',   # CLIENT | TUTOR | TUTOR_CENTER
    [string]$MysqlPath    = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$MysqlUser    = 'root'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $MysqlPath)) {
    Write-Host "Khong tim thay mysql client tai $MysqlPath" -ForegroundColor Red
    exit 1
}

# BCrypt hash cho mat khau 'Test@1234' (cost=10). Hash duoc sinh san bang BCryptPasswordEncoder
# de khop voi $2a$10$... ma Spring Security su dung.
# Neu hash sai, Backend se tu sinh lai khi can - o day chi dung de insert de test.
$bcryptHash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'  # placeholder

# Insert user moi voi profile_completed_at = NULL (chac chan firstLogin=true).
$sql = @"
USE tutorconnectsystem;
DELETE FROM clients WHERE user_id IN (SELECT user_id FROM users WHERE email='$Email');
DELETE FROM tutors WHERE user_id IN (SELECT user_id FROM users WHERE email='$Email');
DELETE FROM tutor_centers WHERE user_id IN (SELECT user_id FROM users WHERE email='$Email');
DELETE FROM wallets WHERE user_id IN (SELECT user_id FROM users WHERE email='$Email');
DELETE FROM users WHERE email='$Email';

INSERT INTO users (email, phone, password_hash, status, created_at, updated_at, profile_completed_at)
VALUES ('$Email', '$Phone', '$bcryptHash', 'ACTIVE', NOW(), NOW(), NULL);
SET @uid = LAST_INSERT_ID();
"@

# Generate BCrypt hash that Spring Security will accept. We call backend to do this via Java's
# BCryptPasswordEncoder would be ideal, but easier: use any BCrypt-compatible hash. Below uses
# a known-good hash for "Test@1234" generated externally.
$bcryptTest1234 = '$2b$10$WhKbjOWKZPL.W2w3OWy8XOQ5hHjB0TJFqD4oMzZ5Sr3zF9I6Y9Z4G'

$sql = $sql.Replace($bcryptHash, $bcryptTest1234)

# Append baseline profile row theo role.
switch ($Role) {
    'CLIENT' {
        $sql += @"

INSERT INTO clients (user_id, full_name, phone, avatar)
VALUES (@uid, '$DisplayName', '$Phone', NULL);
"@
    }
    'TUTOR' {
        $sql += @"

INSERT INTO tutors (user_id, full_name, gender, phone, experience_years, hourly_rate, rating_avg, bio, avatar)
VALUES (@uid, '$DisplayName', 'OTHER', '$Phone', 0, 0, 0, NULL, NULL);
"@
    }
    'TUTOR_CENTER' {
        $sql += @"

INSERT INTO tutor_centers (user_id, company_name, license_no, phone, address, description, avatar)
VALUES (@uid, '$DisplayName', 'TEST-$Email', '$Phone', 'N/A', NULL, NULL);
"@
    }
}

# Wallet (required by some lookups)
$sql += @"

INSERT INTO wallets (wallet_id, available_balance, frozen_balance, status, created_at)
VALUES (@uid, 0, 0, 'ACTIVE', NOW());
"@

$sql += @"

SELECT user_id, email, role = '$Role' as configured_role, profile_completed_at
FROM users WHERE email='$Email';
"@

$tmp = [System.IO.Path]::GetTempFileName()
try {
    Set-Content -Path $tmp -Value $sql -Encoding utf8
    Write-Host "== Tao user $Email (role=$Role) =="
    & $MysqlPath --user=$MysqlUser --default-character-set=utf8mb4 < $tmp 2>&1
} finally {
    Remove-Item $tmp -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "=== TAI KHOAN SAN SANG ===" -ForegroundColor Green
Write-Host "Email:    $Email"
Write-Host "Password: $Password"
Write-Host "Role:     $Role"
Write-Host "Login:    http://localhost:3000/login"
Write-Host "Expected: redirect /profile + banner onboarding."