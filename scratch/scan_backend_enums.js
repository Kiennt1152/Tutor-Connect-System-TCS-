const fs = require('fs');
const path = require('path');

function getFiles(dir, files = []) {
  if (!fs.existsSync(dir)) return files;
  const fileList = fs.readdirSync(dir);
  for (const file of fileList) {
    const name = path.join(dir, file);
    if (fs.statSync(name).isDirectory()) {
      getFiles(name, files);
    } else {
      if (name.endsWith('.java')) files.push(name);
    }
  }
  return files;
}

const javaFiles = getFiles('backend/src/main/java');
const enumList = [];

for (const f of javaFiles) {
  const content = fs.readFileSync(f, 'utf8');
  if (content.match(/public\s+enum\s+([A-Za-z0-9_]+)/)) {
    const enumName = content.match(/public\s+enum\s+([A-Za-z0-9_]+)/)[1];
    // extract enum values
    const bodyMatch = content.match(/public\s+enum\s+[A-Za-z0-9_]+[\s\S]*?\{([\s\S]*?)[;}]/);
    let values = [];
    if (bodyMatch) {
      const raw = bodyMatch[1].replace(/\/\/.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '');
      values = raw.split(',')
        .map(v => v.trim().split('(')[0].trim())
        .filter(v => v && /^[A-Z0-9_]+$/.test(v));
    }
    enumList.push({
      file: f.replace(/\\/g, '/').replace('backend/src/main/java/', ''),
      name: enumName,
      values: values
    });
  }
}

console.log('Total Enums in backend:', enumList.length);
enumList.sort((a,b) => a.name.localeCompare(b.name));
enumList.forEach(e => console.log(e.name + ': [' + e.values.join(', ') + '] (' + e.file + ')'));
