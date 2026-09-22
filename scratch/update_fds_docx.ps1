$targetFile = "C:\Users\Admin\Downloads\Report_4.1_FDS_v1.2 (1).docx"
$backupFile = "C:\Users\Admin\Downloads\Report_4.1_FDS_v1.2 (1)_BACKUP.docx"

if (-not (Test-Path $backupFile)) {
    Write-Host "Creating backup $backupFile..."
    Copy-Item $targetFile $backupFile -Force
}

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = [Microsoft.Office.Interop.Word.WdAlertLevel]::wdAlertsNone

try {
    Write-Host "Opening $targetFile..."
    $doc = $word.Documents.Open($targetFile)
    $cYellow = [Microsoft.Office.Interop.Word.WdColorIndex]::wdYellow
    $emDash = [char]8212

    function Replace-Text($findText, $replaceText) {
        $find = $doc.Content.Find
        $find.Text = $findText
        $find.MatchCase = $true
        $find.MatchWholeWord = $false
        $count = 0
        while ($find.Execute()) {
            $rng = $find.Parent
            $rng.Text = $replaceText
            $rng.HighlightColorIndex = $cYellow
            $count++
            Write-Host "   [OK] Replaced '$findText' -> '$replaceText' (match $count)"
        }
        if ($count -eq 0) {
            Write-Warning "   [WARN] Text not found: '$findText'"
        }
    }

    Write-Host "1. Fixing SCR-15 to SCR-23 (Recruitment Post Management)..."
    $t1 = "SCR-15 " + $emDash + " Recruitment Post Management"
    $r1 = "SCR-23 " + $emDash + " Recruitment Post Management"
    Replace-Text $t1 $r1

    Write-Host "2. Fixing SCR-54 typo (Use Account Management)..."
    $t2 = "SCR-54 " + $emDash + " Use Account Management"
    $r2 = "SCR-54 " + $emDash + " User Account Management"
    Replace-Text $t2 $r2

    Write-Host "3. Harmonizing SCR-19 title..."
    $t3 = "SCR-19 " + $emDash + " Tutor Assignment Management"
    $r3 = "SCR-19 " + $emDash + " Class Assignment Management"
    Replace-Text $t3 $r3

    Write-Host "4. Updating JOB-12 trigger to 1:00 AM..."
    Replace-Text "Daily at 2:00 AM" "Daily at 1:00 AM"
    $t4 = "Scheduled " + $emDash + " daily at 2:00 AM"
    $r4 = "Scheduled " + $emDash + " daily at 1:00 AM"
    Replace-Text $t4 $r4

    Write-Host "5. Updating JOB-10 trigger (Stale Escrow Sweeper)..."
    Replace-Text "Every 30 min" "Every 5 min (Reconciliation) / 6 hours (Auto-Release)"
    $t5 = "Scheduled " + $emDash + " every 30 minutes"
    $r5 = "Scheduled " + $emDash + " every 5 minutes (Payment Reconciliation) and every 6 hours (Class Auto-Confirmation)"
    Replace-Text $t5 $r5

    Write-Host "6. Updating JOB-11 trigger (Circumvention Scanner)..."
    $t6 = "Scheduled " + $emDash + " hourly"
    $r6 = "Real-time interceptor (on message send) and Scheduled every 5 min (penalty unban scheduler)"
    Replace-Text $t6 $r6

    Write-Host "7. Updating JOB-09 trigger (Reputation Score)..."
    $t7 = "Scheduled " + $emDash + " daily"
    $r7 = "Event-driven " + $emDash + " when a review is submitted or updated"
    Replace-Text $t7 $r7

    Write-Host "8. Adding 67 screens clarification note to Section 3..."
    $fSec3 = $doc.Content.Find
    $fSec3.Text = "3. Screen Inventory"
    $fSec3.MatchCase = $true
    if ($fSec3.Execute()) {
        $p = $fSec3.Parent
        $noteRange = $doc.Range($p.End, $p.End)
        $noteRange.Text = "`r`n*(Note: System scope comprises 67 screens total: 59 UI screens detailed in Section 3, plus 8 system/background service engines detailed in Section 2.1: SCR-07, SCR-22, SCR-31, SCR-33, SCR-45, SCR-46, SCR-49, SCR-52).*`r`n"
        $noteRange.HighlightColorIndex = $cYellow
        Write-Host "   [OK] Clarification note added after '3. Screen Inventory'"
    }

    Write-Host "Saving changes to $targetFile..."
    $doc.Save()
    $doc.Close()
    Write-Host "Done FDS updates successfully!"
} catch {
    Write-Error ("Error updating FDS: " + $_.Exception.Message)
} finally {
    $word.Quit()
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($word) | Out-Null
    [System.GC]::Collect()
    [System.GC]::WaitForPendingFinalizers()
}
