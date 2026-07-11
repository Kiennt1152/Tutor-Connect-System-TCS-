# Usage: powershell -ExecutionPolicy Bypass -File .\scripts\kill-port.ps1 3000
param(
    [Parameter(Mandatory = $true)][int]$Port
)

$conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if (-not $conn) {
    Write-Host "Port $Port is FREE"
    exit 0
}

$pids = $conn | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($pid in $pids) {
    Write-Host "Killing PID $pid on port $Port"
    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
}

Start-Sleep -Milliseconds 500
$stillUp = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($stillUp) {
    Write-Host "Port $Port still in use by PID(s): $(($stillUp.OwningProcess | Sort-Object -Unique) -join ', ')"
    exit 1
}
Write-Host "Port $Port is now FREE"