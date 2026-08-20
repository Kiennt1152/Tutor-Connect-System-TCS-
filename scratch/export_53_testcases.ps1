[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipPath = (Resolve-Path "docs/Report_5.3_SystemTest.xlsx").Path
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)

$sharedStringsEntry = $zip.Entries | Where-Object { $_.FullName -eq "xl/sharedStrings.xml" }
$sharedStrings = @()
if ($sharedStringsEntry) {
    $stream = $sharedStringsEntry.Open()
    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
    [xml]$ssXml = $reader.ReadToEnd()
    $reader.Close()
    $stream.Close()
    foreach ($si in $ssXml.sst.si) {
        $text = ""
        if ($si.t) { $text = $si.t }
        elseif ($si.r) {
            foreach ($r in $si.r) { $text += $r.t }
        } else {
            $text = $si.InnerText
        }
        $sharedStrings += $text
    }
}

$workbookEntry = $zip.Entries | Where-Object { $_.FullName -eq "xl/workbook.xml" }
$stream = $workbookEntry.Open()
$reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
[xml]$wbXml = $reader.ReadToEnd()
$reader.Close()
$stream.Close()

$relsEntry = $zip.Entries | Where-Object { $_.FullName -eq "xl/_rels/workbook.xml.rels" }
$stream = $relsEntry.Open()
$reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
[xml]$relsXml = $reader.ReadToEnd()
$reader.Close()
$stream.Close()

$relMap = @{}
foreach ($rel in $relsXml.Relationships.Relationship) {
    $relMap[$rel.Id] = $rel.Target
}

$allResults = @()

foreach ($sheet in $wbXml.workbook.sheets.sheet) {
    $sheetName = $sheet.name
    $rId = $sheet.id
    if (-not $rId) {
        $rId = $sheet.GetAttribute("id", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")
    }
    $target = $relMap[$rId]
    $path = if ($target.StartsWith("/")) { $target.Substring(1) } elseif ($target.StartsWith("xl/")) { $target } else { "xl/" + $target }
    $sheetEntry = $zip.Entries | Where-Object { $_.FullName -eq $path }
    if (-not $sheetEntry) { continue }
    
    $stream = $sheetEntry.Open()
    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
    [xml]$sXml = $reader.ReadToEnd()
    $reader.Close()
    $stream.Close()

    $workflowTitle = ""
    $workflowReq = ""
    
    $rows = $sXml.SelectNodes("//*[local-name()='row']")
    foreach ($row in $rows) {
        $rowNum = [int]$row.GetAttribute("r")
        $cols = @{}
        $cells = $row.SelectNodes("./*[local-name()='c']")
        foreach ($c in $cells) {
            $r = $c.GetAttribute("r")
            $colRef = $r -replace '\d+',''
            $t = $c.GetAttribute("t")
            $vNode = $c.SelectSingleNode("./*[local-name()='v']")
            $isNode = $c.SelectSingleNode("./*[local-name()='is']")
            $val = ""
            if ($t -eq "s" -and $vNode) {
                $idx = [int]$vNode.InnerText
                $val = $sharedStrings[$idx]
            } elseif ($isNode) {
                $val = $isNode.InnerText
            } elseif ($vNode) {
                $val = $vNode.InnerText
            }
            $cols[$colRef] = $val
        }
        
        if ($rowNum -eq 2 -and $cols["B"]) { $workflowTitle = $cols["B"] }
        if ($rowNum -eq 3 -and $cols["B"]) { $workflowReq = $cols["B"] }
        
        # Test case rows in Test Cases or Workflow sheets
        if ($cols["A"] -and ($cols["A"] -match '^ST-' -or $cols["A"] -match '^TC_ST_' -or ($sheetName -eq "Test Cases" -and $rowNum -ge 10))) {
            $allResults += [PSCustomObject]@{
                Sheet = $sheetName
                WorkflowTitle = $workflowTitle
                TC_ID = $cols["A"]
                Description = $cols["B"]
                Procedure = $cols["C"]
                Expected = $cols["D"]
                Precondition = $cols["E"]
                Round1 = $cols["F"]
                Tester = $cols["H"]
            }
        }
    }
}

$zip.Dispose()

$allResults | Export-Csv -Path "scratch/report_53_testcases.csv" -NoTypeInformation -Encoding UTF8
Write-Host "Total Test Cases extracted: $($allResults.Count)"
$allResults | Format-Table -Property Sheet, TC_ID, Description, Round1 -AutoSize
