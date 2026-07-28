package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Dữ liệu tạo/sửa tin tuyển gia sư của trung tâm (FT-33). */
@Getter
@Setter
public class SaveRecruitmentPostRequest {

    /** Lớp cần tuyển gia sư (tuỳ chọn). Null = tin tuyển chung, không gắn lớp. */
    private Long classId;
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private Integer requiredExperience;
    private Integer maxPositions;
    /** Môn học nhập tự do — tìm-hoặc-tạo theo tên (giữ FK toàn vẹn mà không cần dropdown). */
    private String subjectName;
    /** Địa điểm làm việc (tuỳ chọn): cần cả tỉnh + địa chỉ cụ thể thì mới lưu. */
    private String provinceName;
    private String wardName;
    private String addressDetail;
}
