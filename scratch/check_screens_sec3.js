const fs = require('fs');

const fdsLines = fs.readFileSync('C:\\Users\\Admin\\.gemini\\antigravity\\brain\\173fa670-2800-4199-b83c-90dfebfa42be\\scratch\\report_4.1_fds.txt', 'utf8').split('\n');
const appTsx = fs.readFileSync('c:\\Users\\Admin\\Documents\\GitHub\\Tutor-Connect-System-TCS-\\frontend\\src\\app\\App.tsx', 'utf8');
const routesTsx = fs.readFileSync('c:\\Users\\Admin\\Documents\\GitHub\\Tutor-Connect-System-TCS-\\frontend\\src\\shared\\constants\\routes.tsx', 'utf8');

// Find all SCR definitions in Section 3 of FDS
const screensSec3 = [];
for (let i = 0; i < fdsLines.length; i++) {
  const line = fdsLines[i].trim();
  const m = line.match(/^SCR-(\d{2})\s*—\s*(.+)$/);
  if (m) {
    screensSec3.push({ id: `SCR-${m[1]}`, name: m[2].trim(), line: i + 1 });
  }
}

console.log('Screens in Section 3 of FDS count:', screensSec3.length);
screensSec3.forEach(s => console.log(`${s.id}: ${s.name} (line ${s.line})`));
