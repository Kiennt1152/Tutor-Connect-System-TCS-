import zipfile
import xml.etree.ElementTree as ET

def inspect_xlsx(path):
    with zipfile.ZipFile(path, 'r') as z:
        wb_xml = z.read('xl/workbook.xml')
        wb_tree = ET.fromstring(wb_xml)
        
        # Namespaces
        ns = {'main': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
        
        sheets = []
        for s in wb_tree.findall('main:sheets/main:sheet', ns):
            sheets.append(s.attrib['name'])
        
        print("Sheets found:", sheets)
        
        # Read shared strings if present
        shared_strings = []
        if 'xl/sharedStrings.xml' in z.namelist():
            ss_xml = z.read('xl/sharedStrings.xml')
            ss_tree = ET.fromstring(ss_xml)
            for si in ss_tree.findall('main:si', ns):
                text = "".join([t.text for t in si.findall('.//main:t', ns) if t.text])
                shared_strings.append(text)
        
        print(f"Total shared strings: {len(shared_strings)}")
        
        # Read sheet1
        # Let's inspect all sheet entries in xl/worksheets/
        for idx, sheet_name in enumerate(sheets, 1):
            sheet_file = f'xl/worksheets/sheet{idx}.xml'
            if sheet_file in z.namelist():
                sheet_xml = z.read(sheet_file)
                sheet_tree = ET.fromstring(sheet_xml)
                rows = sheet_tree.findall('.//main:row', ns)
                print(f"\n--- Sheet {idx}: {sheet_name} (Total rows: {len(rows)}) ---")
                
                for r in rows[:15]: # Print first 15 rows
                    row_num = r.attrib.get('r')
                    cells = []
                    for c in r.findall('main:c', ns):
                        t = c.attrib.get('t')
                        v = c.find('main:v', ns)
                        val = v.text if v is not None else ''
                        if t == 's' and val.isdigit():
                            val = shared_strings[int(val)]
                        cells.append(val)
                    if any(cells):
                        print(f"Row {row_num}: {' | '.join(cells[:8])}")

if __name__ == '__main__':
    inspect_xlsx('docs/Report_5.3_SystemTest.xlsx')
