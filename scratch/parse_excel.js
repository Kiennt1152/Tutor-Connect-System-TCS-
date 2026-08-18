const fs = require('fs');
const path = require('path');

const tempDir = path.join(process.env.TEMP, 'excel_test_report');

// Read shared strings
let sharedStrings = [];
const sharedStringsPath = path.join(tempDir, 'xl', 'sharedStrings.xml');
if (fs.existsSync(sharedStringsPath)) {
  const xml = fs.readFileSync(sharedStringsPath, 'utf8');
  // Match <si>...</si> entries
  const siMatches = xml.match(/<si>[\s\S]*?<\/si>/g) || [];
  sharedStrings = siMatches.map(si => {
    // Collect all <t> or <t ...> text
    const tMatches = si.match(/<t(?:\s[^>]*)?>([\s\S]*?)<\/t>/g) || [];
    return tMatches.map(t => t.replace(/<[^>]+>/g, '')).join('');
  });
}

// Read workbook.xml to get sheet names
const workbookPath = path.join(tempDir, 'xl', 'workbook.xml');
const wbXml = fs.readFileSync(workbookPath, 'utf8');
const sheets = [];
const sheetMatches = wbXml.match(/<sheet\s+[^>]*name="([^"]+)"[^>]*r:id="([^"]+)"[^>]*\/>/g) || [];
for (const s of sheetMatches) {
  const nameMatch = s.match(/name="([^"]+)"/);
  const idMatch = s.match(/r:id="([^"]+)"/);
  const sheetIdMatch = s.match(/sheetId="([^"]+)"/);
  sheets.push({
    name: nameMatch ? nameMatch[1] : '',
    rId: idMatch ? idMatch[1] : '',
    sheetId: sheetIdMatch ? sheetIdMatch[1] : '1'
  });
}
console.log('Sheets found:', sheets);

// Process each sheet
const worksheetsDir = path.join(tempDir, 'xl', 'worksheets');
const sheetFiles = fs.readdirSync(worksheetsDir).filter(f => f.endsWith('.xml'));

sheetFiles.forEach((file, index) => {
  const sheetName = sheets[index] ? sheets[index].name : file;
  console.log(`\n========================================`);
  console.log(`SHEET [${index + 1}]: ${sheetName} (File: ${file})`);
  console.log(`========================================`);

  const sheetXml = fs.readFileSync(path.join(worksheetsDir, file), 'utf8');
  const rowMatches = sheetXml.match(/<row\s+[^>]*r="(\d+)"[^>]*>[\s\S]*?<\/row>/g) || [];

  const rows = [];
  rowMatches.forEach(rowXml => {
    const rowNumMatch = rowXml.match(/r="(\d+)"/);
    const rowNum = rowNumMatch ? parseInt(rowNumMatch[1]) : 0;
    const cellMatches = rowXml.match(/<c\s+[^>]*r="([A-Z]+\d+)"(?:[^>]*t="([^"]*)")?[^>]*>[\s\S]*?<\/c>/g) || [];

    const rowData = {};
    cellMatches.forEach(cXml => {
      const rMatch = cXml.match(/r="([A-Z]+)(\d+)"/);
      const tMatch = cXml.match(/t="([^"]+)"/);
      const vMatch = cXml.match(/<v>([\s\S]*?)<\/v>/);

      if (rMatch && vMatch) {
        const col = rMatch[1];
        let val = vMatch[1];
        if (tMatch && tMatch[1] === 's') {
          const sIdx = parseInt(val);
          val = sharedStrings[sIdx] || '';
        }
        rowData[col] = val;
      }
    });

    if (Object.keys(rowData).length > 0) {
      rows.push({ rowNum, data: rowData });
    }
  });

  console.log(`Total rows with data: ${rows.length}`);
  rows.slice(0, 30).forEach(r => {
    console.log(`Row ${r.rowNum}:`, JSON.stringify(r.data));
  });
  if (rows.length > 30) {
    console.log(`... and ${rows.length - 30} more rows`);
  }
});
