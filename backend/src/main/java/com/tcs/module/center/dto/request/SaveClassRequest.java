package com.tcs.module.center.dto.request;

import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Dùng chung cho tạo mới và chỉnh sửa lớp học của Trung tâm gia sư (UC-14-B). */
@Getter
@Setter
public class SaveClassRequest {

    private String title;
    private String description;
    // Người dùng tự nhập tên (backend sẽ tìm-hoặc-tạo bản ghi danh mục tương ứng).
    private String categoryName;
    private String subjectName;
    private String gradeName;
    // Địa điểm theo mô hình 2 cấp: Tỉnh/Thành → Phường/Xã, kèm địa chỉ cụ thể tự nhập.
    private String provinceName;
    private String wardName;
    private String addressDetail;
    // (Cũ) địa chỉ tự nhập — giữ lại để tương thích, không bắt buộc.
    private String locationText;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private RecurringType recurringType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private Integer maxStudents;
    /** Số học sinh tối thiểu để kích hoạt lớp (tuỳ chọn; trống = cần ≥ 1). */
    private Integer minStudents;
    /** Loại lớp: EXTERNAL (yêu cầu ngoài, đã có học sinh) / SELF (trung tâm tự tạo). Mặc định SELF. */
    private String originType;
    /** Mẫu hợp đồng trung tâm chọn cho lớp (dùng khi sinh hợp đồng học viên). Tuỳ chọn. */
    private Long contractTemplateId;
    /** Nội dung điều khoản HĐ học viên center tự nhập/sửa khi tạo lớp (nạp sẵn từ mẫu). Tuỳ chọn. */
    private String contractContent;
    private List<ScheduleSlotRequest> schedule;
}
