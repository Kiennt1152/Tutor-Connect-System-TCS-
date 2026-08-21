package com.tcs.util;

/**
 * Tiện ích ghi CSV cho các chức năng xuất dữ liệu.
 *
 * <p>File xuất ra được mở bằng Excel nên phải xử lý hai thứ:
 * <ul>
 *   <li><b>Dấu tiếng Việt</b> — Excel chỉ đọc đúng UTF-8 khi file mở đầu bằng BOM
 *       ({@link #BOM}); thiếu nó thì "Nguyễn" thành "Nguyá»…n".</li>
 *   <li><b>Chèn công thức</b> — ô bắt đầu bằng {@code = + - @} bị Excel hiểu là công thức.
 *       Một học viên đặt tên {@code =HYPERLINK(...)} có thể biến file xuất thành bẫy cho
 *       người mở. {@link #escape} thêm dấu nháy đơn để vô hiệu hoá.</li>
 * </ul>
 */
public final class Csv {

    /** Byte order mark — bắt buộc đứng đầu file để Excel nhận UTF-8. */
    public static final String BOM = "﻿";

    private Csv() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace("\"", "\"\"");
        if (!clean.isEmpty() && "=+-@\t".indexOf(clean.charAt(0)) >= 0) {
            clean = "'" + clean;
        }
        if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
            return "\"" + clean + "\"";
        }
        return clean;
    }
}
