[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$csv = Import-Csv "scratch/report_53_testcases.csv" -Encoding UTF8
$csv | Where-Object { [int]($_.Sheet -replace 'Workflow Name','') -le 5 } | ForEach-Object {
    Write-Host "=================================================================="
    Write-Host "[$($_.Sheet)] $($_.TC_ID) - $($_.Description)"
    Write-Host "Workflow: $($_.WorkflowTitle)"
    Write-Host "------------------------------------------------------------------"
    Write-Host "Pre-conditions:`n$($_.Precondition)"
    Write-Host "Test Procedure:`n$($_.Procedure)"
    Write-Host "Expected Results:`n$($_.Expected)"
}
