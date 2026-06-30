Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('c:\Users\Admin\IdeaProjects\Tutor-Connect-System-TCS-\docs\Report_3.0_PRD.docx')
$entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' }
$ms = New-Object System.IO.MemoryStream
$entry.Open().CopyTo($ms)
$ms.Position = 0
$reader = New-Object System.IO.StreamReader($ms)
$content = $reader.ReadToEnd()
$reader.Close()
$ms.Close()
$zip.Dispose()
[xml]$xml = $content
$nsMgr = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
$nsMgr.AddNamespace("w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
$body = $xml.SelectSingleNode("//w:body", $nsMgr)
$sb = New-Object System.Text.StringBuilder
foreach ($p in $body.ChildNodes) {
    if ($p.LocalName -eq 'p') {
        $text = $p.InnerText
        if ($text.Trim().Length -gt 0) {
            $null = $sb.AppendLine($text)
        }
    }
}
$sb.ToString()