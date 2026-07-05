# Tao tai khoan test chua chinh ho so de kiem tra firstLogin redirect.
# Usage: powershell -ExecutionPolicy Bypass -File .\scripts\create-test-account.ps1
# Mat khau mac dinh: Test@1234 (thoa BR-UC01-05: >=8 ky tu, gom chu + so, ASCII).

param(
    [string]$Email    = 'onboarding.test@example.com',
    [string]$Password = 'Test@1234',
    [string]$Display  = 'Onboarding Test',
    [string]$Phone    = '0901234567',
    [string]$Role     = 'CLIENT',   # CLIENT | TUTOR | TUTOR_CENTER
    [string]$BaseUrl  = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'

Write-Host "== Kiem tra backend =="
try {
    $null = Invoke-RestMethod -Uri "$BaseUrl/api/home" -Method GET -TimeoutSec 5
} catch {
    Write-Host "Backend chua chay tai $BaseUrl. Hay start Spring Boot truoc." -ForegroundColor Red
    exit 1
}

Write-Host "== Buoc 1/4: Gui OTP toi $Email =="
$otpBody = @{ email = $Email; role = $Role } | ConvertTo-Json
$otpResp = Invoke-RestMethod -Uri "$BaseUrl/api/identity/send-otp" `
    -Method POST -ContentType 'application/json' -Body $otpBody
Write-Host "  -> $($otpResp.message)"

# Lay OTP gan nhat tu DB (lay qua MySQL CLI). Can mysql client.
$otpCode = $null
$mysqlCmds = @(
    "mysql -uroot -e `"SELECT code FROM tutorconnectsystem.email_otps WHERE email='$Email' AND purpose='REGISTRATION' AND consumed_at IS NULL ORDER BY created_at DESC LIMIT 1;`""
    "`"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`" -uroot -e `"SELECT code FROM tutorconnectsystem.email_otps WHERE email='$Email' AND purpose='REGISTRATION' AND consumed_at IS NULL ORDER BY created_at DESC LIMIT 1;`""
)
foreach ($cmd in $mysqlCmds) {
    try {
        $otpCode = (& cmd /c $cmd 2>$null) -replace '^\s*code\s*$','' -replace '\s','' | Select-Object -First 1
        $otpCode = $otpCode.Trim()
    } catch { }
    if ($otpCode -and $otpCode -match '^\d{4,8}$') { break }
}

if (-not $otpCode) {
    Write-Host "Khong the lay OTP tu DB. Hay doc ma tu console cua backend (log emailService.sendRegistrationOtp)" -ForegroundColor Yellow
    exit 1
}
Write-Host "  -> OTP: $otpCode"

Write-Host "== Buoc 2/4: Xac thuc OTP =="
$verifyBody = @{ email = $Email; code = $otpCode } | ConvertTo-Json
$verifyResp = Invoke-RestMethod -Uri "$BaseUrl/api/identity/verify-otp" `
    -Method POST -ContentType 'application/json' -Body $verifyBody
$verifiedToken = $verifyResp.verifiedEmailToken
Write-Host "  -> $($verifyResp.message)"

Write-Host "== Buoc 3/4: Dang ky =="
$registerBody = @{
    email                 = $Email
    role                  = $Role
    displayName           = $Display
    phone                 = $Phone
    password              = $Password
    confirmPassword       = $Password
    verifiedEmailToken    = $verifiedToken
} | ConvertTo-Json
$registerResp = Invoke-RestMethod -Uri "$BaseUrl/api/identity/register" `
    -Method POST -ContentType 'application/json' -Body $registerBody
Write-Host "  -> $($registerResp.message)"

Write-Host "== Buoc 4/4: Dang nhap (firstLogin phai = true) =="
$loginBody = @{ email = $Email; password = $Password } | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/identity/login" `
    -Method POST -ContentType 'application/json' -Body $loginBody
Write-Host "  Role: $($loginResp.role)"
Write-Host "  firstLogin: $($loginResp.firstLogin)  (phai = True)"

Write-Host ""
Write-Host "=== TAI KHOAN SAN SANG ===" -ForegroundColor Green
Write-Host "Email:    $Email"
Write-Host "Password: $Password"
Write-Host "Role:     $Role"
Write-Host "Login:    http://localhost:3000/login"
Write-Host "Expected: Login xong se redirect /profile va hien banner onboarding."