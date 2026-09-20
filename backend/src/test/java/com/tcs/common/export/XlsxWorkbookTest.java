package com.tcs.common.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

/**
 * Kiểm tra bộ sinh .xlsx viết tay: file phải là gói ZIP đúng cấu trúc OOXML, mọi phần XML phải
 * hợp lệ, và dữ liệu (chữ, số, ngày, phần trăm) phải nằm đúng ô. Nếu một trong các điểm này sai,
 * Excel sẽ báo "file bị hỏng" — nên các phép kiểm tra dưới đây thay cho việc mở thử bằng Excel.
 */
class XlsxWorkbookTest {

    /** Giải nén file .xlsx trong bộ nhớ thành map: đường dẫn phần -> nội dung. */
    private static Map<String, String> unzip(byte[] xlsx) throws IOException {
        Map<String, String> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                zip.transferTo(out);
                parts.put(entry.getName(), out.toString(StandardCharsets.UTF_8));
            }
        }
        return parts;
    }

    /** Ném ngoại lệ nếu chuỗi không phải XML hợp lệ. */
    private static void assertWellFormedXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            factory.newDocumentBuilder().parse(new InputSource(in));
        }
    }

    @Nested
    @DisplayName("Cấu trúc gói OOXML")
    class PackageStructure {

        @Test
        @DisplayName("Đủ các phần bắt buộc và mọi phần đều là XML hợp lệ")
        void containsRequiredPartsAndValidXml() throws Exception {
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("Học viên").header("STT", "Họ tên").row(1, "Nguyễn Văn A");

            Map<String, String> parts = unzip(workbook.toBytes());

            assertTrue(parts.containsKey("[Content_Types].xml"));
            assertTrue(parts.containsKey("_rels/.rels"));
            assertTrue(parts.containsKey("xl/workbook.xml"));
            assertTrue(parts.containsKey("xl/_rels/workbook.xml.rels"));
            assertTrue(parts.containsKey("xl/styles.xml"));
            assertTrue(parts.containsKey("xl/worksheets/sheet1.xml"));

            for (Map.Entry<String, String> part : parts.entrySet()) {
                assertWellFormedXml(part.getValue());
            }
        }

        @Test
        @DisplayName("Nhiều sheet: mỗi sheet có phần riêng, quan hệ riêng và khai báo kiểu ô không đụng id")
        void multipleSheetsGetOwnPartsAndRelationships() throws Exception {
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("Học viên").header("A").row("x");
            workbook.addSheet("Tổng hợp lớp").header("B").row("y");

            Map<String, String> parts = unzip(workbook.toBytes());

            assertTrue(parts.containsKey("xl/worksheets/sheet1.xml"));
            assertTrue(parts.containsKey("xl/worksheets/sheet2.xml"));

            String rels = parts.get("xl/_rels/workbook.xml.rels");
            assertTrue(rels.contains("Id=\"rId1\"") && rels.contains("worksheets/sheet1.xml"));
            assertTrue(rels.contains("Id=\"rId2\"") && rels.contains("worksheets/sheet2.xml"));
            // Kiểu ô phải nhận id kế tiếp (rId3), không được trùng id của sheet.
            assertTrue(rels.contains("Id=\"rId3\"") && rels.contains("styles.xml"));

            String contentTypes = parts.get("[Content_Types].xml");
            assertTrue(contentTypes.contains("/xl/worksheets/sheet1.xml"));
            assertTrue(contentTypes.contains("/xl/worksheets/sheet2.xml"));
        }

        @Test
        @DisplayName("Tên sheet quá dài hoặc chứa ký tự Excel cấm được cắt gọn lại")
        void sheetNameIsSanitised() throws Exception {
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("Lớp Toán 9 / nâng cao [thứ 3] : ca tối, cơ sở Cầu Giấy").row("x");

            String workbookXml = unzip(workbook.toBytes()).get("xl/workbook.xml");

            assertTrue(workbookXml.contains("<sheet name=\""));
            assertTrue(!workbookXml.contains("/") || workbookXml.indexOf("name=\"") > 0);
            String name = workbookXml.split("name=\"")[1].split("\"")[0];
            assertTrue(name.length() <= 31, "Tên sheet phải ≤ 31 ký tự, đang là: " + name);
            for (char forbidden : new char[] {':', '\\', '/', '?', '*', '[', ']'}) {
                assertTrue(name.indexOf(forbidden) < 0, "Còn ký tự cấm: " + forbidden);
            }
        }
    }

    @Nested
    @DisplayName("Nội dung ô")
    class CellContent {

        @Test
        @DisplayName("Chữ, số, ngày và phần trăm được ghi đúng kiểu ô")
        void writesEachValueTypeCorrectly() throws Exception {
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("Học viên")
                    .header("Tên", "Buổi", "Ngày", "Tỉ lệ")
                    .row("Trần Thị B", 12L, LocalDate.of(2026, 3, 1), new XlsxWorkbook.Percent(87.5));

            String sheet = unzip(workbook.toBytes()).get("xl/worksheets/sheet1.xml");

            // Tiêu đề: chuỗi ghi thẳng vào ô, dùng kiểu in đậm (s="1").
            assertTrue(sheet.contains("<c r=\"A1\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Tên"));
            // Chuỗi thường: không kèm kiểu.
            assertTrue(sheet.contains("<c r=\"A2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Trần Thị B"));
            // Số: chỉ có <v>, không có thuộc tính t.
            assertTrue(sheet.contains("<c r=\"B2\"><v>12</v></c>"));
            // Ngày 01/03/2026 -> số thứ tự 46082 của Excel, kèm kiểu định dạng ngày (s="2").
            assertTrue(sheet.contains("<c r=\"C2\" s=\"2\"><v>46082</v></c>"), sheet);
            // Phần trăm: kiểu s="3".
            assertTrue(sheet.contains("<c r=\"D2\" s=\"3\"><v>87.5</v></c>"));
        }

        @Test
        @DisplayName("Ô trống bị bỏ hẳn, không sinh thẻ rỗng")
        void nullCellsAreOmitted() throws Exception {
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("S").header("A", "B", "C").row("x", null, "z");

            String sheet = unzip(workbook.toBytes()).get("xl/worksheets/sheet1.xml");

            assertTrue(sheet.contains("r=\"A2\""));
            assertTrue(!sheet.contains("r=\"B2\""), "Ô null không được sinh thẻ <c>");
            assertTrue(sheet.contains("r=\"C2\""));
        }

        @Test
        @DisplayName("Ký tự đặc biệt và ký tự điều khiển không làm hỏng XML")
        void escapesSpecialCharacters() throws Exception {
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("S")
                    .header("Ghi chú")
                    .row("Lớp <Toán> & \"Lý\" 'A'  xuống\ndòng");

            Map<String, String> parts = unzip(workbook.toBytes());
            String sheet = parts.get("xl/worksheets/sheet1.xml");

            assertWellFormedXml(sheet);
            assertTrue(sheet.contains("&lt;Toán&gt;"));
            assertTrue(sheet.contains("&amp;"));
            assertTrue(sheet.contains("&quot;Lý&quot;"));
            assertTrue(!sheet.contains(""), "Ký tự điều khiển phải bị loại bỏ");
        }

        @Test
        @DisplayName("Cột thứ 27 trở đi được đánh đúng AA, AB")
        void columnReferencesPastZ() throws Exception {
            Object[] cells = new Object[28];
            for (int i = 0; i < cells.length; i++) {
                cells[i] = "c" + i;
            }
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("S").row(cells);

            String sheet = unzip(workbook.toBytes()).get("xl/worksheets/sheet1.xml");

            assertTrue(sheet.contains("r=\"Z1\""));
            assertTrue(sheet.contains("r=\"AA1\""));
            assertTrue(sheet.contains("r=\"AB1\""));
        }
    }

    @Nested
    @DisplayName("Tiện ích bảng tính")
    class SpreadsheetFeatures {

        @Test
        @DisplayName("Có dòng tiêu đề thì khoá dòng đầu và bật bộ lọc")
        void headerEnablesFreezePaneAndFilter() throws Exception {
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("S").header("A", "B").row(1, 2);

            String sheet = unzip(workbook.toBytes()).get("xl/worksheets/sheet1.xml");

            assertTrue(sheet.contains("<pane ySplit=\"1\""));
            assertTrue(sheet.contains("<autoFilter ref=\"A1:B1\"/>"));
            // autoFilter phải nằm sau sheetData, nếu không Excel từ chối mở file.
            assertTrue(sheet.indexOf("<autoFilter") > sheet.indexOf("</sheetData>"));
        }

        @Test
        @DisplayName("Không có tiêu đề thì không khoá dòng, không bật bộ lọc")
        void withoutHeaderThereIsNoFreezeOrFilter() throws Exception {
            XlsxWorkbook workbook = new XlsxWorkbook();
            workbook.addSheet("S").row(1, 2);

            String sheet = unzip(workbook.toBytes()).get("xl/worksheets/sheet1.xml");

            assertTrue(!sheet.contains("<pane "));
            assertTrue(!sheet.contains("<autoFilter"));
        }

        @Test
        @DisplayName("Workbook rỗng vẫn tạo được file mở được")
        void emptyWorkbookStillProducesValidFile() throws Exception {
            byte[] bytes = new XlsxWorkbook().toBytes();

            Map<String, String> parts = unzip(bytes);

            assertNotNull(parts.get("xl/worksheets/sheet1.xml"));
            assertWellFormedXml(parts.get("xl/worksheets/sheet1.xml"));
            assertEquals(1, parts.keySet().stream().filter(p -> p.startsWith("xl/worksheets/")).count());
        }
    }
}
