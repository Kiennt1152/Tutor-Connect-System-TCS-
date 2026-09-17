package com.tcs.common.export;

/**
 * Một file được sinh ra để tải về: tên file, kiểu nội dung và dữ liệu.
 * Tách riêng để tầng service quyết định tên file (theo dữ liệu) còn controller chỉ việc trả ra.
 */
public record ExportFile(String filename, String contentType, byte[] content) {

    public static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static ExportFile xlsx(String filename, byte[] content) {
        return new ExportFile(filename, XLSX_CONTENT_TYPE, content);
    }
}
