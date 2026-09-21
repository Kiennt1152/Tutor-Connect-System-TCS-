const fs = require('fs');

const fdsLines = fs.readFileSync('C:\\Users\\Admin\\.gemini\\antigravity\\brain\\173fa670-2800-4199-b83c-90dfebfa42be\\scratch\\report_4.1_fds.txt', 'utf8').split('\n');

for (let i = 3649; i < fdsLines.length; i++) {
  const line = fdsLines[i];
  if (line.startsWith('5.') || line.startsWith('JOB-') || line.includes('Schedule') || line.includes('Cron') || line.includes('Trigger') || line.includes('Processing Logic') || line.includes('Spring Component') || line.includes('Method')) {
    console.log((i+1) + ': ' + line.substring(0, 120));
  }
}
