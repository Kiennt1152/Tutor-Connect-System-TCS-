Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipPath = (Resolve-Path "docs/Report_5.3_SystemTest.xlsx").Path
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)

$sharedStringsEntry = $zip.Entries | Where-Object { $_.FullName -eq "xl/sharedStrings.xml" }
$sharedStrings = @()
if ($sharedStringsEntry) {
    $stream = $sharedStringsEntry.Open()
    $reader = New-Object System.IO.StreamReader($stream)
    [xml]$ssXml = $reader.ReadToEnd()
    $reader.Close()
    $stream.Close()
    foreach ($si in $ssXml.sst.si) {
        $sharedStrings += $si.InnerText
    }
}

$workbookEntry = $zip.Entries | Where-Object { $_.FullName -eq "xl/workbook.xml" }
$stream = $workbookEntry.Open()
$reader = New-Object System.IO.StreamReader($stream)
[xml]$wbXml = $reader.ReadToEnd()
$reader.Close()
$stream.Close()

$relsEntry = $zip.Entries | Where-Object { $_.FullName -eq "xl/_rels/workbook.xml.rels" }
$stream = $relsEntry.Open()
$reader = New-Object System.IO.StreamReader($stream)
[xml]$relsXml = $reader.ReadToEnd()
$reader.Close()
$stream.Close()

$relMap = @{}
foreach ($rel in $relsXml.Relationships.Relationship) {
    $relMap[$rel.Id] = $rel.Target
}

function Dump-Sheet-All($sheetName) {
    $sheetObj = $wbXml.workbook.sheets.sheet | Where-Object { $_.name -eq $sheetName }
    if (-not $sheetObj) { return }
    $rId = $sheetObj.id
    if (-not $rId) {
        $rId = $sheetObj.GetAttribute("id", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")
    }
    $target = $relMap[$rId]
    $path = if ($target.StartsWith("/")) { $target.Substring(1) } elseif ($target.StartsWith("xl/")) { $target } else { "xl/" + $target }
    $sheetEntry = $zip.Entries | Where-Object { $_.FullName -eq $path }
    if (-not $sheetEntry) { 
        Write-Host "Target not found: $path"
        return 
    }
    
    $stream = $sheetEntry.Open()
    $reader = New-Object System.IO.StreamReader($stream)
    [xml]$sXml = $reader.ReadToEnd()
    $reader.Close()
    $stream.Close()

    Write-Host "`n========================================================"
    Write-Host "SHEET: $sheetName"
    Write-Host "========================================================"
    
    $rows = $sXml.SelectNodes("//*[local-name()='row']")
    Write-Host "Total rows found: $($rows.Count)"
    
    foreach ($row in $rows) {
        $rowNum = $row.GetAttribute("r")
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
        $nonEmpty = $cols.Values | Where-Object { $_ -ne "" }
        if ($nonEmpty) {
            $preview = ($cols.Keys | Sort-Object | ForEach-Object { "$_=$($cols[$_])" }) -join " | "
            if ($preview.Length -gt 200) { $preview = $preview.Substring(0, 200) + "..." }
            Write-Host "Row ${rowNum}: $preview"
        }
    }
}

foreach ($sheet in $wbXml.workbook.sheets.sheet) {
    Dump-Sheet-All($sheet.name)
}

$zip.Dispose()
