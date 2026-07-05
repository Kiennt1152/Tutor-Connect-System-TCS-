$content = Get-Content 'C:\Users\Admin\.cursor\projects\c-Users-Admin-IdeaProjects-Tutor-Connect-System-TCS\agent-tools\8d9f5d0c-18b7-4ef4-96e1-5b58b43de9a7.txt' -Raw
$idx64 = $content.IndexOf('UC-64')
$idx66 = $content.IndexOf('UC-66')
if ($idx64 -ge 0 -and $idx66 -ge 0) {
    $content.Substring($idx64, $idx66 - $idx64)
}
