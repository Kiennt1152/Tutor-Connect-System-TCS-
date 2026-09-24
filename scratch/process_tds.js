const fs = require('fs');

const xmlPath = 'C:\\Users\\Admin\\.gemini\\antigravity\\brain\\173fa670-2800-4199-b83c-90dfebfa42be\\scratch\\tds_unpack\\word\\document.xml';
let xml = fs.readFileSync(xmlPath, 'utf8');

console.log('Original TDS XML length:', xml.length);

function replaceExact(original, replacement, desc) {
  if (!xml.includes(original)) {
    console.error(`ERROR: Could not find "${desc}" in document.xml`);
    process.exit(1);
  }
  xml = xml.replace(original, replacement);
  console.log(`[OK] Replaced: ${desc}`);
}

// 1. Lombok version in Section 1.1: 1.18.46 -> 1.18.36
replaceExact(
  '<w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr><w:t>1.18.46</w:t>',
  '<w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>1.18.36</w:t>',
  'Lombok version in Table 1.1'
);

// 2. Lombok version in Section 6.3
replaceExact(
  '<w:color w:val="1A1A1A"/><w:sz w:val="18"/><w:szCs w:val="18"/></w:rPr><w:t>org.projectlombok:lombok (1.18.46)</w:t>',
  '<w:color w:val="1A1A1A"/><w:sz w:val="18"/><w:szCs w:val="18"/><w:highlight w:val="yellow"/></w:rPr><w:t>org.projectlombok:lombok (1.18.36)</w:t>',
  'Lombok version in Section 6.3'
);

// 3. Insert ShedLock in Table 1.1 right after TwelveMonkeys ImageIO row
const tmIdx = xml.indexOf('TwelveMonkeys ImageIO');
if (tmIdx !== -1) {
  const trEndIdx = xml.indexOf('</w:tr>', tmIdx) + 7;
  const shedlockRowXml = `<w:tr w:rsidR="008051FE" w14:paraId="435EAF58" w14:textId="77777777"><w:tc><w:tcPr><w:tcW w:w="2500" w:type="dxa"/><w:tcBorders><w:top w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:left w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:bottom w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:right w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/></w:tcBorders><w:shd w:val="clear" w:color="auto" w:fill="F3F3F3"/><w:tcMar><w:top w:w="0" w:type="dxa"/><w:left w:w="40" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/><w:right w:w="40" w:type="dxa"/></w:tcMar><w:vAlign w:val="center"/></w:tcPr><w:p w14:paraId="117A5CC3" w14:textId="77777777" w:rsidR="008051FE" w:rsidRDefault="001F1075"><w:pPr><w:widowControl w:val="0"/><w:spacing w:line="276" w:lineRule="auto"/><w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/></w:rPr></w:pPr><w:r><w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Distributed Lock</w:t></w:r></w:p></w:tc><w:tc><w:tcPr><w:tcW w:w="2500" w:type="dxa"/><w:tcBorders><w:top w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:left w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:bottom w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:right w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/></w:tcBorders><w:shd w:val="clear" w:color="auto" w:fill="F3F3F3"/><w:tcMar><w:top w:w="0" w:type="dxa"/><w:left w:w="40" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/><w:right w:w="40" w:type="dxa"/></w:tcMar><w:vAlign w:val="center"/></w:tcPr><w:p w14:paraId="1E454178" w14:textId="77777777" w:rsidR="008051FE" w:rsidRDefault="001F1075"><w:pPr><w:widowControl w:val="0"/><w:spacing w:line="276" w:lineRule="auto"/><w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/></w:rPr></w:pPr><w:r><w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>ShedLock (core + spring + jdbc)</w:t></w:r></w:p></w:tc><w:tc><w:tcPr><w:tcW w:w="1600" w:type="dxa"/><w:tcBorders><w:top w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:left w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:bottom w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:right w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/></w:tcBorders><w:shd w:val="clear" w:color="auto" w:fill="F3F3F3"/><w:tcMar><w:top w:w="0" w:type="dxa"/><w:left w:w="40" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/><w:right w:w="40" w:type="dxa"/></w:tcMar><w:vAlign w:val="center"/></w:tcPr><w:p w14:paraId="61ED348B" w14:textId="77777777" w:rsidR="008051FE" w:rsidRDefault="001F1075"><w:pPr><w:widowControl w:val="0"/><w:spacing w:line="276" w:lineRule="auto"/><w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/></w:rPr></w:pPr><w:r><w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>5.16.0</w:t></w:r></w:p></w:tc><w:tc><w:tcPr><w:tcW w:w="2760" w:type="dxa"/><w:tcBorders><w:top w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:left w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:bottom w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/><w:right w:val="single" w:sz="5" w:space="0" w:color="CCCCCC"/></w:tcBorders><w:shd w:val="clear" w:color="auto" w:fill="F3F3F3"/><w:tcMar><w:top w:w="0" w:type="dxa"/><w:left w:w="40" w:type="dxa"/><w:bottom w:w="0" w:type="dxa"/><w:right w:w="40" w:type="dxa"/></w:tcMar><w:vAlign w:val="center"/></w:tcPr><w:p w14:paraId="640588D2" w14:textId="77777777" w:rsidR="008051FE" w:rsidRDefault="001F1075"><w:pPr><w:widowControl w:val="0"/><w:spacing w:line="276" w:lineRule="auto"/><w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/></w:rPr></w:pPr><w:r><w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>Distributed lock provider preventing duplicate cron execution across clustered multi-instance deployments (V40 migration table shedlock).</w:t></w:r></w:p></w:tc></w:tr>`;
  xml = xml.substring(0, trEndIdx) + shedlockRowXml + xml.substring(trEndIdx);
  console.log('[OK] Inserted ShedLock row in Table 1.1');
}

// 4. Insert ShedLock in Section 6.3 Build Dependencies
const tmDepIdx = xml.indexOf('imageio-jpeg + imageio-webp (3.11.0)');
if (tmDepIdx !== -1) {
  const pEndIdx = xml.indexOf('</w:p>', tmDepIdx) + 6;
  const shedlockDepXml = `<w:p w14:paraId="056B71D9" w14:textId="77777777" w:rsidR="008051FE" w:rsidRDefault="001F1075"><w:pPr><w:rPr><w:color w:val="1A1A1A"/><w:sz w:val="18"/><w:szCs w:val="18"/></w:rPr></w:pPr><w:r><w:rPr><w:color w:val="1A1A1A"/><w:sz w:val="18"/><w:szCs w:val="18"/><w:highlight w:val="yellow"/></w:rPr><w:t>net.javacrumbs.shedlock:shedlock-spring + shedlock-provider-jdbc-template (5.16.0 — distributed cron locking)</w:t></w:r></w:p>`;
  xml = xml.substring(0, pEndIdx) + shedlockDepXml + xml.substring(pEndIdx);
  console.log('[OK] Inserted ShedLock in Section 6.3 Build Dependencies');
}

// 5. AI chat provider priority order
replaceExact(
  'Priority order: Cerebras',
  'Priority order: Groq → Cerebras',
  'AI chat provider priority order'
);

// 6. Payment transactions PLATFORM_FEE in Table 3.1 & Section 3.3
replaceExact(
  '<w:t>DEPOSIT / WITHDRAWAL / REFUND / ESCROW_DEPOSIT / ESCROW_RELEASE</w:t>',
  '<w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>DEPOSIT / WITHDRAWAL / REFUND / ESCROW_DEPOSIT / ESCROW_RELEASE / PLATFORM_FEE</w:t>',
  'PaymentTransactionType in Table 3.1'
);

replaceExact(
  '<w:t>DEPOSIT, WITHDRAWAL, REFUND, ESCROW_DEPOSIT, ESCROW_RELEASE</w:t>',
  '<w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>DEPOSIT, WITHDRAWAL, REFUND, ESCROW_DEPOSIT, ESCROW_RELEASE, PLATFORM_FEE</w:t>',
  'PaymentTransactionType in Section 3.3'
);

// 7. KnowledgeSourceType TICKET, TRANSACTION in Table 3.1 & Section 3.3
replaceExact(
  '<w:t>TUTOR, CLASS, FAQ, POLICY, SYSTEM_DOC</w:t>',
  '<w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>TUTOR, CLASS, FAQ, POLICY, SYSTEM_DOC, TICKET, TRANSACTION</w:t>',
  'KnowledgeSourceType in Table 3.1'
);

replaceExact(
  '<w:t>TUTOR, CLASS, FAQ, POLICY, SYSTEM_DOC</w:t>',
  '<w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>TUTOR, CLASS, FAQ, POLICY, SYSTEM_DOC, TICKET, TRANSACTION</w:t>',
  'KnowledgeSourceType in Section 3.3'
);

// 8. Serving Strategy private file endpoint
replaceExact(
  '<w:t>GET /api/files/certificate/{fileId}</w:t>',
  '<w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>GET /api/files/private/{fileId}</w:t>',
  'Serving Strategy private file endpoint'
);

// 9. Static uploads pattern in SecurityConfig
replaceExact(
  '* Static &amp; Framework: `/error`, `/uploads/public/**`, `/swagger-ui/**`, `/v3/api-docs/**`</w:t>',
  '* Static &amp; Framework: `/error`, `/uploads/**`, `/swagger-ui/**`, `/v3/api-docs/**`</w:t>',
  'Static uploads pattern in Section 4.2'
);

replaceExact(
  '"/error", "/uploads/public/**", "/api/home", "/api/home/announcements",',
  '"/error", "/uploads/**", "/api/home", "/api/home/announcements",',
  'Static uploads pattern in Section 4.7'
);

// 10. Migration Strategy note on V1 to V45
replaceExact(
  '<w:t>V{N}__{description}.sql / R__{description}.sql</w:t>',
  '<w:rPr><w:color w:val="2A2A2A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>V{N}__{description}.sql / R__{description}.sql (V1 to V45 and R__seed_catalog.sql active)</w:t>',
  'Migration Strategy V1 to V45'
);

// 11. Section 3.1 Overview Note for 82 JPA entities
const sec31BodyIdx = xml.indexOf('3.1 Entity Definitions', 200000);
if (sec31BodyIdx !== -1) {
  const pEndIdx = xml.indexOf('</w:p>', sec31BodyIdx) + 6;
  const noteXml = `<w:p w14:paraId="3A125E99" w14:textId="77777777" w:rsidR="008051FE" w:rsidRDefault="001F1075"><w:pPr><w:spacing w:before="120" w:after="120"/><w:rPr><w:i/><w:iCs/><w:color w:val="1A1A1A"/><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr></w:pPr><w:r><w:rPr><w:i/><w:iCs/><w:color w:val="1A1A1A"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:highlight w:val="yellow"/></w:rPr><w:t>*(Architecture Note: The production codebase implements 82 JPA entity tables mapped 1-1 to the MySQL database schema across all 12 modules, managed through Flyway migrations V1–V45. The section below details the core domain entities, with complete schema alignment across the 82 active tables).*</w:t></w:r></w:p>`;
  xml = xml.substring(0, pEndIdx) + noteXml + xml.substring(pEndIdx);
  console.log('[OK] Inserted: Section 3.1 82 Entities Architecture Note');
}

fs.writeFileSync(xmlPath, xml, 'utf8');
console.log('Saved modified TDS document.xml! New length:', xml.length);
