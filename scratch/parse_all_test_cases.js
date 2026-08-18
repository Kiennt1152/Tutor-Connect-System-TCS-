const fs = require('fs');
const path = require('path');

const tempDir = path.join(process.env.TEMP, 'excel_test_report');

// Read shared strings
let sharedStrings = [];
const sharedStringsPath = path.join(tempDir, 'xl', 'sharedStrings.xml');
if (fs.existsSync(sharedStringsPath)) {
  const xml = fs.readFileSync(sharedStringsPath, 'utf8');
  const siMatches = xml.match(/<si>[\s\S]*?<\/si>/g) || [];
  sharedStrings = siMatches.map(si => {
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
  sheets.push({
    name: nameMatch ? nameMatch[1] : '',
    rId: idMatch ? idMatch[1] : ''
  });
}

// Read workbook relationships to map r:id to target sheet XML
const relsPath = path.join(tempDir, 'xl', '_rels', 'workbook.xml.rels');
const relsXml = fs.readFileSync(relsPath, 'utf8');
const relMap = {};
const relMatches = relsXml.match(/<Relationship\s+[^>]*Id="([^"]+)"[^>]*Target="([^"]+)"[^>]*\/>/g) || [];
for (const r of relMatches) {
  const idMatch = r.match(/Id="([^"]+)"/);
  const targetMatch = r.match(/Target="([^"]+)"/);
  if (idMatch && targetMatch) {
    relMap[idMatch[1]] = targetMatch[1].replace('worksheets/', '');
  }
}

const allTestCases = [];

sheets.forEach((sh, idx) => {
  const sheetFile = relMap[sh.rId] || `sheet${idx + 1}.xml`;
  const sheetPath = path.join(tempDir, 'xl', 'worksheets', sheetFile);
  if (!fs.existsSync(sheetPath)) return;

  const sheetXml = fs.readFileSync(sheetPath, 'utf8');
  const rowMatches = sheetXml.match(/<row\s+[^>]*r="(\d+)"[^>]*>[\s\S]*?<\/row>/g) || [];

  let workflowName = '';
  let testReq = '';

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

    if (rowNum === 1 && rowData['B']) workflowName = rowData['B'];
    if (rowNum === 3 && rowData['B']) testReq = rowData['B'];

    // Test case rows start with TC ID like ST- or TC-
    if (rowData['A'] && (rowData['A'].startsWith('ST-') || rowData['A'].startsWith('TC-') || /^[A-Z]{2,4}-[A-Z0-9]+/.test(rowData['A']))) {
      allTestCases.push({
        sheet: sh.name,
        workflow: workflowName,
        id: rowData['A'],
        name: rowData['B'] || '',
        procedure: rowData['C'] || '',
        expected: rowData['D'] || '',
        precondition: rowData['E'] || '',
        round1Result: rowData['F'] || '',
        tester: rowData['H'] || ''
      });
    }
  });
});

console.log(`Total Test Cases extracted: ${allTestCases.length}\n`);
allTestCases.forEach((tc, idx) => {
  console.log(`[${idx + 1}] ID: ${tc.id} | Sheet: ${tc.sheet} | Workflow: ${tc.workflow}`);
  console.log(`    Name: ${tc.name}`);
  console.log(`    Procedure: ${tc.procedure.replace(/\n/g, ' -> ')}`);
  console.log(`    Expected: ${tc.expected.replace(/\n/g, ' ')}`);
  console.log(`    Precondition: ${tc.precondition}`);
  console.log(`    Result in sheet: ${tc.round1Result}`);
  console.log('----------------------------------------------------');
});

fs.writeFileSync(path.join(__dirname, 'all_test_cases.json'), JSON.stringify(allTestCases, null, 2));
