[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$csv = Import-Csv "scratch/report_53_testcases.csv" -Encoding UTF8
foreach ($tc in $csv) {
    Write-Host "=================================================================="
    Write-Host "[$($tc.Sheet)] $($tc.TC_ID) - $($tc.Description)"
    Write-Host "Workflow: $($tc.WorkflowTitle)"
    Write-Host "------------------------------------------------------------------"
    Write-Host "Pre-conditions:`n$($tc.Precondition)"
    Write-Host "Test Procedure:`n$($tc.Procedure)"
    Write-Host "Expected Results:`n$($tc.Expected)"
    Write-Host "Status: $($tc.Round1)"
}
