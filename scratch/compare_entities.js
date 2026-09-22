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
const entityMap = [];

for (const f of javaFiles) {
  const content = fs.readFileSync(f, 'utf8');
  if (content.includes('@Entity')) {
    const tableMatch = content.match(/@Table\s*\(\s*name\s*=\s*"([^"]+)"/);
    const classMatch = content.match(/public\s+(?:class|record)\s+([A-Za-z0-9_]+)/);
    entityMap.push({
      file: f.replace(/\\/g, '/').replace('backend/src/main/java/', ''),
      className: classMatch ? classMatch[1] : 'Unknown',
      tableName: tableMatch ? tableMatch[1] : (classMatch ? classMatch[1].toLowerCase() : 'Unknown')
    });
  }
}

console.log('Total JPA Entities in backend:', entityMap.length);
entityMap.sort((a,b) => a.tableName.localeCompare(b.tableName));
entityMap.forEach(e => console.log(e.tableName + ' -> ' + e.className + ' (' + e.file + ')'));
