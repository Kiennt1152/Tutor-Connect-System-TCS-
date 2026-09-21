const fs = require('fs');
const { execSync } = require('child_process');

function checkDocx(filePath, checks) {
  console.log('==================================================');
  console.log('Checking:', filePath);
  console.log('Size:', fs.statSync(filePath).size, 'bytes');

  // Extract document.xml using PowerShell
  const tmpTxt = 'scratch_check_temp.txt';
  const psCmd = `powershell -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; $zip = [System.IO.Compression.ZipFile]::OpenRead('${filePath}'); $entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' }; $reader = New-Object System.IO.StreamReader($entry.Open()); [System.IO.File]::WriteAllText('${tmpTxt}', $reader.ReadToEnd(), [System.Text.Encoding]::UTF8); $reader.Dispose(); $zip.Dispose()"`;
  execSync(psCmd);

  const xml = fs.readFileSync(tmpTxt, 'utf8');
  fs.unlinkSync(tmpTxt);

  let passed = 0;
  for (const [key, term] of Object.entries(checks)) {
    const found = xml.includes(term);
    console.log(`   [${found ? 'PASS' : 'FAIL'}] ${key}`);
    if (found) passed++;
  }

  const highlightCount = (xml.match(/yellow/g) || []).length;
  console.log(`   Result: ${passed}/${Object.keys(checks).length} passed. Highlight occurrences (yellow): ${highlightCount}`);
}

const tdsChecks = {
  'Lombok 1.18.36': '1.18.36',
  'ShedLock in Table 1.1': 'ShedLock (core + spring + jdbc)',
  'ShedLock in 6.3': 'net.javacrumbs.shedlock:shedlock-spring',
  'AI Priority Groq -> Cerebras': 'Priority order: Groq',
  'PLATFORM_FEE in 3.1 & 3.3': 'PLATFORM_FEE',
  'TICKET, TRANSACTION in 3.1 & 3.3': 'TICKET, TRANSACTION',
  'Private file endpoint': '/api/files/private/{fileId}',
  'Uploads pattern /uploads/**': '/uploads/**',
  'Flyway V1 to V45': 'V1 to V45 and R__seed_catalog.sql',
  '82 Entities Architecture Note': 'Architecture Note: The production codebase implements 82 JPA entity tables'
};

const fdsChecks = {
  'SCR-23 Recruitment Post': 'SCR-23 — Recruitment Post Management',
  'SCR-54 User Account': 'SCR-54 — User Account Management',
  'SCR-19 Class Assignment': 'SCR-19 — Class Assignment Management',
  'JOB-12 1:00 AM': 'Daily at 1:00 AM',
  'JOB-10 5 min & 6 hrs': 'Every 5 min (Reconcile) / 6 hrs (Auto-Release)',
  'JOB-11 Real-time & 5 min': 'Real-time + Every 5 min',
  'JOB-09 Event-driven': 'Event-driven — when a review is submitted or status updated',
  'Section 3 67 Screens Note': '*(Note on System Scope: The 67 total system screens comprise 59 detailed UI screens'
};

checkDocx('C:\\Users\\Admin\\Downloads\\Report_4.0_TDS_v1.3 (2).docx', tdsChecks);
checkDocx('C:\\Users\\Admin\\Downloads\\Report_4.1_FDS_v1.2 (1).docx', fdsChecks);
