Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('c:\Users\Admin\IdeaProjects\Tutor-Connect-System-TCS-\docs\Report_3.1_UCS.docx')
$entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' }
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$content = $reader.ReadToEnd()
$reader.Close()
$stream.Close()
$zip.Dispose()

$text = $content -replace '<[^>]+>', ' '
$text = $text -replace '\s+', ' '
$text
