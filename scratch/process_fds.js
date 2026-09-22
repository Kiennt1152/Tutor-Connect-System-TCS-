const fs = require('fs');

const xmlPath = 'C:\\Users\\Admin\\.gemini\\antigravity\\brain\\173fa670-2800-4199-b83c-90dfebfa42be\\scratch\\fds_unpack\\word\\document.xml';
let xml = fs.readFileSync(xmlPath, 'utf8');

console.log('Original XML length:', xml.length);

function replaceExact(original, replacement, desc) {
  if (!xml.includes(original)) {
    console.error(`ERROR: Could not find "${desc}" in document.xml`);
    process.exit(1);
  }
  xml = xml.replace(original, replacement);
  console.log(`[OK] Replaced: ${desc}`);
}

// 1. Fix SCR-15 to SCR-23
if (xml.includes('SCR-15 — Recruitment Post Management')) {
  replaceExact(
    '<w:rPr><w:b/><w:bCs/></w:rPr><w:t>SCR-15 — Recruitment Post Management</w:t>',
    '<w:rPr><w:b/><w:bCs/><w:highlight w:val="yellow"/></w:rPr><w:t>SCR-23 — Recruitment Post Management</w:t>',
    'SCR-15 -> SCR-23'
  );
} else {
  console.log('[SKIP] SCR-15 already updated');
}

// 2. Fix SCR-54 typo
if (xml.includes('SCR-54 — Use Account Management')) {
  replaceExact(
    '<w:rPr><w:b/><w:bCs/></w:rPr><w:t>SCR-54 — Use Account Management</w:t>',
    '<w:rPr><w:b/><w:bCs/><w:highlight w:val="yellow"/></w:rPr><w:t>SCR-54 — User Account Management</w:t>',
    'SCR-54 typo'
  );
} else {
  console.log('[SKIP] SCR-54 already updated');
}

// 3. Fix SCR-19 title
if (xml.includes('SCR-19 — Tutor Assignment Management')) {
  replaceExact(
    '<w:rPr><w:b/><w:bCs/></w:rPr><w:t>SCR-19 — Tutor Assignment Management</w:t>',
    '<w:rPr><w:b/><w:bCs/><w:highlight w:val="yellow"/></w:rPr><w:t>SCR-19 — Class Assignment Management</w:t>',
    'SCR-19 title harmonization'
  );
} else {
  console.log('[SKIP] SCR-19 already updated');
}

// 4. JOB-12 trigger to 1:00 AM
if (xml.includes('Daily at 2:00 AM')) {
  replaceExact(
    '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr><w:t>Daily at 2:00 AM</w:t>',
    '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Daily at 1:00 AM</w:t>',
    'JOB-12 Table 5.1 trigger'
  );
}

if (xml.includes('Scheduled — daily at 2:00 AM')) {
  replaceExact(
    '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr><w:t>Scheduled — daily at 2:00 AM</w:t>',
    '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Scheduled — daily at 1:00 AM</w:t>',
    'JOB-12 Section 5.13 trigger'
  );
}

// 5. JOB-10 trigger (Stale Escrow Sweeper)
if (xml.includes('Every 30 min')) {
  replaceExact(
    '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr><w:t>Every 30 min</w:t>',
    '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Every 5 min (Reconcile) / 6 hrs (Auto-Release)</w:t>',
    'JOB-10 Table 5.1 trigger'
  );
}

if (xml.includes('Scheduled — every 30 minutes')) {
  replaceExact(
    '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr><w:t>Scheduled — every 30 minutes</w:t>',
    '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Scheduled — every 5 minutes (Payment Reconciliation) and every 6 hours (Class Auto-Confirmation)</w:t>',
    'JOB-10 Section 5.11 trigger'
  );
}

// 6. JOB-11 trigger (Circumvention Scanner)
// In Table 5.1 row JOB-11:
const job11RowMatch = xml.indexOf('JOB-11</w:t>', 2000000);
if (job11RowMatch !== -1) {
  const hourlyIdx = xml.indexOf('<w:t>Hourly</w:t>', job11RowMatch);
  if (hourlyIdx !== -1 && hourlyIdx - job11RowMatch < 5000) {
    const target = xml.substring(hourlyIdx - 60, hourlyIdx + '<w:t>Hourly</w:t>'.length);
    const replacement = target.replace(
      '<w:t>Hourly</w:t>',
      '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Real-time + Every 5 min</w:t>'
    );
    xml = xml.substring(0, hourlyIdx - 60) + replacement + xml.substring(hourlyIdx + '<w:t>Hourly</w:t>'.length);
    console.log('[OK] Replaced: JOB-11 Table 5.1 trigger');
  }
}

// In Section 5.12:
const sec512Match = xml.indexOf('5.12 JOB-11', 2000000);
if (sec512Match !== -1) {
  const schedHourlyIdx = xml.indexOf('<w:t>Scheduled — hourly</w:t>', sec512Match);
  if (schedHourlyIdx !== -1 && schedHourlyIdx - sec512Match < 10000) {
    const target = xml.substring(schedHourlyIdx - 80, schedHourlyIdx + '<w:t>Scheduled — hourly</w:t>'.length);
    const replacement = target.replace(
      '<w:t>Scheduled — hourly</w:t>',
      '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Real-time interceptor (on chat message send) &amp; Scheduled every 5 minutes (penalty unban scheduler)</w:t>'
    );
    xml = xml.substring(0, schedHourlyIdx - 80) + replacement + xml.substring(schedHourlyIdx + '<w:t>Scheduled — hourly</w:t>'.length);
    console.log('[OK] Replaced: JOB-11 Section 5.12 trigger');
  }
}

// 7. JOB-09 trigger (Reputation Score)
// In Table 5.1 row JOB-09:
const job09RowMatch = xml.indexOf('JOB-09</w:t>', 2000000);
if (job09RowMatch !== -1) {
  const dailyIdx = xml.indexOf('<w:t>Daily</w:t>', job09RowMatch);
  if (dailyIdx !== -1 && dailyIdx - job09RowMatch < 5000) {
    const target = xml.substring(dailyIdx - 60, dailyIdx + '<w:t>Daily</w:t>'.length);
    const replacement = target.replace(
      '<w:t>Daily</w:t>',
      '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Event-driven</w:t>'
    );
    xml = xml.substring(0, dailyIdx - 60) + replacement + xml.substring(dailyIdx + '<w:t>Daily</w:t>'.length);
    console.log('[OK] Replaced: JOB-09 Table 5.1 trigger');
  }
}

// In Section 5.10:
const sec510Match = xml.indexOf('5.10 JOB-09', 2000000);
if (sec510Match !== -1) {
  const schedDailyIdx = xml.indexOf('<w:t>Scheduled — daily</w:t>', sec510Match);
  if (schedDailyIdx !== -1 && schedDailyIdx - sec510Match < 10000) {
    const target = xml.substring(schedDailyIdx - 80, schedDailyIdx + '<w:t>Scheduled — daily</w:t>'.length);
    const replacement = target.replace(
      '<w:t>Scheduled — daily</w:t>',
      '<w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Event-driven — when a review is submitted or status updated</w:t>'
    );
    xml = xml.substring(0, schedDailyIdx - 80) + replacement + xml.substring(schedDailyIdx + '<w:t>Scheduled — daily</w:t>'.length);
    console.log('[OK] Replaced: JOB-09 Section 5.10 trigger');
  }
}

// 8. Section 3 Note on 67 screens total
if (!xml.includes('*(Note on System Scope:')) {
  const sec3HeadingMatch = xml.indexOf('<w:t>3. Screen Inventory</w:t>');
  if (sec3HeadingMatch !== -1) {
    const pEndIdx = xml.indexOf('</w:p>', sec3HeadingMatch);
    if (pEndIdx !== -1) {
      const noteXml = `<w:p w14:paraId="2E4F90A1" w14:textId="77777777" w:rsidR="005F4BF9" w:rsidRDefault="00A714AD"><w:pPr><w:spacing w:before="60" w:after="120"/><w:rPr><w:i/><w:iCs/><w:color w:val="555555"/><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr></w:pPr><w:r><w:rPr><w:i/><w:iCs/><w:color w:val="1A1A1A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>*(Note on System Scope: The 67 total system screens comprise 59 detailed UI screens documented below in Section 3, and 8 non-UI background service / system processing engines: SCR-07, SCR-22, SCR-31, SCR-33, SCR-45, SCR-46, SCR-49, SCR-52 as indexed in Section 2.1).*</w:t></w:r></w:p>`;
      xml = xml.substring(0, pEndIdx + 6) + noteXml + xml.substring(pEndIdx + 6);
      console.log('[OK] Inserted: Section 3 Scope Note');
    }
  }
}

fs.writeFileSync(xmlPath, xml, 'utf8');
console.log('Saved modified FDS document.xml! New length:', xml.length);
