Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('c:\Users\Admin\IdeaProjects\Tutor-Connect-System-TCS-\docs\Report_3.1_UCS.docx')
$entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' }
$stream = $entry.Open()
$content = New-Object System.IO.StreamReader($stream).ReadToEnd()
$stream.Close()
$zip.Dispose()

$ns = @{w = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}

$xml = [xml]$content
$body = $xml.SelectSingleNode('//w:body', $ns)

# Get all text with paragraph markers
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
