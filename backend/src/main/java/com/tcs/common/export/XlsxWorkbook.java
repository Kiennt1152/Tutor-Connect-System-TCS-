package com.tcs.common.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Bộ sinh file Excel (.xlsx) chỉ dùng thư viện chuẩn của JDK — không cần Apache POI.
 *
 * <p>File .xlsx thực chất là một gói ZIP chứa vài file XML theo chuẩn OOXML. Lớp này ghi đúng
 * bộ tối thiểu mà Excel/LibreOffice/Google Sheets cần: bảng kê nội dung, quan hệ giữa các phần,
 * workbook, định dạng ô và từng sheet. Chuỗi được ghi thẳng vào ô (inline string) nên không
 * cần bảng chuỗi dùng chung, đổi lại file to hơn một chút — chấp nhận được với vài nghìn dòng.
 *
 * <p>Dùng như sau:
 * <pre>
 *   XlsxWorkbook wb = new XlsxWorkbook();
 *   XlsxWorkbook.Sheet sheet = wb.addSheet("Học viên");
 *   sheet.header("STT", "Họ tên");
 *   sheet.row(1, "Nguyễn Văn A");
 *   byte[] bytes = wb.toBytes();
 * </pre>
 */
public final class XlsxWorkbook {

    /** Kiểu ô phần trăm: ghi số nhưng hiển thị kèm dấu %. */
    public record Percent(double value) {}

    /** Mốc ngày của Excel. Excel coi 1900 là năm nhuận nên mốc thật là 30/12/1899. */
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    /** Excel chỉ cho tên sheet tối đa 31 ký tự và cấm : \ / ? * [ ] */
    private static final int SHEET_NAME_MAX = 31;

    /** Giới hạn bề rộng cột tự tính, tránh một ô dài làm vỡ bố cục. */
    private static final int COL_WIDTH_MIN = 8;
    private static final int COL_WIDTH_MAX = 46;

    // Chỉ số kiểu ô, khớp thứ tự khai báo trong cellXfs của styles.xml.
    private static final int STYLE_DEFAULT = 0;
    private static final int STYLE_HEADER = 1;
    private static final int STYLE_DATE = 2;
    private static final int STYLE_PERCENT = 3;

    private final List<Sheet> sheets = new ArrayList<>();

    /** Thêm một sheet mới vào cuối workbook. */
    public Sheet addSheet(String name) {
        Sheet sheet = new Sheet(safeSheetName(name, sheets.size() + 1));
        sheets.add(sheet);
        return sheet;
    }

    /** Đóng gói toàn bộ workbook thành mảng byte của file .xlsx. */
    public byte[] toBytes() {
        if (sheets.isEmpty()) {
            addSheet("Sheet1");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            write(zip, "[Content_Types].xml", contentTypesXml());
            write(zip, "_rels/.rels", rootRelsXml());
            write(zip, "xl/workbook.xml", workbookXml());
            write(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            write(zip, "xl/styles.xml", stylesXml());
            for (int i = 0; i < sheets.size(); i++) {
                write(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheets.get(i).toXml(i == 0));
            }
        } catch (IOException e) {
            // Ghi vào bộ nhớ nên thực tế không xảy ra; bọc lại để nơi gọi không phải bắt IOException.
            throw new UncheckedIOException("Không tạo được file Excel", e);
        }
        return out.toByteArray();
    }

    // ===================================================================== Sheet

    /** Một trang tính: gồm dòng tiêu đề (tuỳ chọn) và các dòng dữ liệu. */
    public static final class Sheet {

        private final String name;
        private final List<Object[]> rows = new ArrayList<>();
        private boolean hasHeader;

        private Sheet(String name) {
            this.name = name;
        }

        /** Dòng tiêu đề — in đậm, nền cam, khoá dòng và bật bộ lọc. Gọi trước mọi row(). */
        public Sheet header(Object... cells) {
            if (!rows.isEmpty()) {
                throw new IllegalStateException("Dòng tiêu đề phải được thêm trước dữ liệu");
            }
            hasHeader = true;
            rows.add(cells);
            return this;
        }

        /**
         * Thêm một dòng dữ liệu. Kiểu ô suy ra từ giá trị: {@link Number} ghi thành số,
         * {@link LocalDate}/{@link LocalDateTime} ghi thành ngày, {@link Percent} ghi kèm %,
         * {@code null} để ô trống, còn lại ghi thành chữ.
         */
        public Sheet row(Object... cells) {
            rows.add(cells);
            return this;
        }

        /** Thêm một dòng trống để tách khối nội dung. */
        public Sheet blankRow() {
            rows.add(new Object[0]);
            return this;
        }

        private String toXml(boolean firstSheet) {
            int columnCount = 0;
            for (Object[] row : rows) {
                columnCount = Math.max(columnCount, row.length);
            }

            StringBuilder xml = new StringBuilder(1024 + rows.size() * 128);
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                    .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");

            if (columnCount > 0 && !rows.isEmpty()) {
                xml.append("<dimension ref=\"A1:")
                        .append(columnRef(columnCount)).append(rows.size())
                        .append("\"/>");
            }

            // Khoá dòng tiêu đề để cuộn xuống vẫn thấy tên cột.
            xml.append("<sheetViews><sheetView workbookViewId=\"0\"");
            if (firstSheet) {
                xml.append(" tabSelected=\"1\"");
            }
            xml.append(">");
            if (hasHeader) {
                xml.append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>");
            }
            xml.append("</sheetView></sheetViews>");
            xml.append("<sheetFormatPr defaultRowHeight=\"15\"/>");

            appendColumnWidths(xml, columnCount);

            xml.append("<sheetData>");
            for (int r = 0; r < rows.size(); r++) {
                Object[] row = rows.get(r);
                int rowNumber = r + 1;
                xml.append("<row r=\"").append(rowNumber).append("\"");
                if (r == 0 && hasHeader) {
                    xml.append(" ht=\"22\" customHeight=\"1\"");
                }
                xml.append(">");
                for (int c = 0; c < row.length; c++) {
                    appendCell(xml, columnRef(c + 1) + rowNumber, row[c], r == 0 && hasHeader);
                }
                xml.append("</row>");
            }
            xml.append("</sheetData>");

            // autoFilter phải đứng sau sheetData theo lược đồ OOXML.
            if (hasHeader && columnCount > 0) {
                xml.append("<autoFilter ref=\"A1:").append(columnRef(columnCount)).append("1\"/>");
            }
            xml.append("</worksheet>");
            return xml.toString();
        }

        /** Bề rộng cột tính theo ô dài nhất của cột đó, có chặn trên/dưới. */
        private void appendColumnWidths(StringBuilder xml, int columnCount) {
            if (columnCount == 0) {
                return;
            }
            xml.append("<cols>");
            for (int c = 0; c < columnCount; c++) {
                int longest = 0;
                for (Object[] row : rows) {
                    if (c < row.length) {
                        longest = Math.max(longest, displayLength(row[c]));
                    }
                }
                int width = Math.min(COL_WIDTH_MAX, Math.max(COL_WIDTH_MIN, longest + 2));
                xml.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                        .append("\" width=\"").append(width).append("\" customWidth=\"1\"/>");
            }
            xml.append("</cols>");
        }

        private int displayLength(Object value) {
            if (value == null) {
                return 0;
            }
            if (value instanceof LocalDate || value instanceof LocalDateTime) {
                return 10;
            }
            if (value instanceof Percent) {
                return 6;
            }
            return String.valueOf(value).length();
        }

        private void appendCell(StringBuilder xml, String ref, Object value, boolean headerRow) {
            if (value == null) {
                return; // ô trống: bỏ hẳn thẻ <c> cho file gọn
            }
            if (headerRow) {
                appendInlineString(xml, ref, STYLE_HEADER, String.valueOf(value));
                return;
            }
            if (value instanceof Percent percent) {
                appendNumber(xml, ref, STYLE_PERCENT, String.valueOf(percent.value()));
                return;
            }
            if (value instanceof Number number) {
                appendNumber(xml, ref, STYLE_DEFAULT, String.valueOf(number));
                return;
            }
            if (value instanceof LocalDate date) {
                appendNumber(xml, ref, STYLE_DATE, String.valueOf(toSerial(date)));
                return;
            }
            if (value instanceof LocalDateTime dateTime) {
                appendNumber(xml, ref, STYLE_DATE, String.valueOf(toSerial(dateTime.toLocalDate())));
                return;
            }
            appendInlineString(xml, ref, STYLE_DEFAULT, String.valueOf(value));
        }

        private void appendNumber(StringBuilder xml, String ref, int style, String value) {
            xml.append("<c r=\"").append(ref).append("\"");
            if (style != STYLE_DEFAULT) {
                xml.append(" s=\"").append(style).append("\"");
            }
            xml.append("><v>").append(value).append("</v></c>");
        }

        private void appendInlineString(StringBuilder xml, String ref, int style, String value) {
            xml.append("<c r=\"").append(ref).append("\"");
            if (style != STYLE_DEFAULT) {
                xml.append(" s=\"").append(style).append("\"");
            }
            xml.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(escape(value))
                    .append("</t></is></c>");
        }

        private long toSerial(LocalDate date) {
            return ChronoUnit.DAYS.between(EXCEL_EPOCH, date);
        }
    }

    // ================================================================= Các phần XML cố định

    private String contentTypesXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=")
                .append("\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=")
                .append("\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=")
                .append("\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 0; i < sheets.size(); i++) {
            xml.append("<Override PartName=\"/xl/worksheets/sheet").append(i + 1).append(".xml\" ContentType=")
                    .append("\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return xml.append("</Types>").toString();
    }

    private String rootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/"
                + "relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String workbookXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                .append("<sheets>");
        for (int i = 0; i < sheets.size(); i++) {
            xml.append("<sheet name=\"").append(escape(sheets.get(i).name))
                    .append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return xml.append("</sheets></workbook>").toString();
    }

    private String workbookRelsXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < sheets.size(); i++) {
            xml.append("<Relationship Id=\"rId").append(i + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\"")
                    .append(" Target=\"worksheets/sheet").append(i + 1).append(".xml\"/>");
        }
        // Kiểu ô nhận id kế tiếp để không đụng id của các sheet.
        xml.append("<Relationship Id=\"rId").append(sheets.size() + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\"")
                .append(" Target=\"styles.xml\"/>");
        return xml.append("</Relationships>").toString();
    }

    /**
     * Bảng định dạng: font thường/đậm, nền cam thương hiệu cho tiêu đề, định dạng ngày dd/mm/yyyy
     * và định dạng phần trăm. Thứ tự các {@code xf} trong cellXfs chính là chỉ số STYLE_* ở trên.
     */
    private String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<numFmts count=\"2\">"
                + "<numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy\"/>"
                + "<numFmt numFmtId=\"165\" formatCode=\"0.0&quot;%&quot;\"/>"
                + "</numFmts>"
                + "<fonts count=\"2\">"
                + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
                + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
                + "</fonts>"
                + "<fills count=\"3\">"
                + "<fill><patternFill patternType=\"none\"/></fill>"
                + "<fill><patternFill patternType=\"gray125\"/></fill>"
                + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEA580C\"/>"
                + "<bgColor indexed=\"64\"/></patternFill></fill>"
                + "</fills>"
                + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                + "<cellXfs count=\"4\">"
                + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
                + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\""
                + " applyFill=\"1\" applyAlignment=\"1\">"
                + "<alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
                + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
                + "</cellXfs>"
                + "</styleSheet>";
    }

    // ===================================================================== Tiện ích

    private void write(ZipOutputStream zip, String path, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Cột 1 -> A, 26 -> Z, 27 -> AA ... */
    private static String columnRef(int index) {
        StringBuilder ref = new StringBuilder();
        int n = index;
        while (n > 0) {
            int rem = (n - 1) % 26;
            ref.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return ref.toString();
    }

    /**
     * Cắt tên sheet cho hợp lệ: bỏ ký tự Excel cấm, giới hạn 31 ký tự, rỗng thì đặt tên mặc định.
     */
    private static String safeSheetName(String name, int index) {
        if (name == null || name.isBlank()) {
            return "Sheet" + index;
        }
        String cleaned = name.replaceAll("[:\\\\/?*\\[\\]]", " ").trim();
        if (cleaned.isEmpty()) {
            return "Sheet" + index;
        }
        return cleaned.length() > SHEET_NAME_MAX ? cleaned.substring(0, SHEET_NAME_MAX) : cleaned;
    }

    /**
     * Thoát ký tự XML. Ngoài 5 ký tự đặc biệt còn phải loại ký tự điều khiển: dữ liệu người dùng
     * nhập có thể lẫn ký tự lạ, lọt vào sẽ làm Excel báo file hỏng.
     */
    private static String escape(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    boolean allowed = ch == '\t' || ch == '\n' || ch == '\r' || ch >= 0x20;
                    if (allowed) {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}
