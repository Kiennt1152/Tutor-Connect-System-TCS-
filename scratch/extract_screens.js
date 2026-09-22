const fs = require('fs');

const fdsLines = fs.readFileSync('C:\\Users\\Admin\\.gemini\\antigravity\\brain\\173fa670-2800-4199-b83c-90dfebfa42be\\scratch\\report_4.1_fds.txt', 'utf8').split('\n');

const screens = [];
let i = 0;
while (i < fdsLines.length) {
  const line = fdsLines[i].trim();
  if (line.startsWith('SCR-') && line.length <= 8) {
    const id = line;
    const name = fdsLines[i+1] ? fdsLines[i+1].replace(/\|/g, '').trim() : '';
    const module = fdsLines[i+2] ? fdsLines[i+2].replace(/\|/g, '').trim() : '';
    const audience = fdsLines[i+3] ? fdsLines[i+3].replace(/\|/g, '').trim() : '';
    const entry = fdsLines[i+4] ? fdsLines[i+4].replace(/\|/g, '').trim() : '';
    const ft = fdsLines[i+5] ? fdsLines[i+5].replace(/\|/g, '').trim() : '';
    screens.push({ id, name, module, audience, entry, ft });
    i += 5;
  } else {
    i++;
  }
}

console.log('Total screens found:', screens.length);
screens.forEach(s => console.log(`${s.id} | ${s.name} | ${s.module} | ${s.entry}`));
