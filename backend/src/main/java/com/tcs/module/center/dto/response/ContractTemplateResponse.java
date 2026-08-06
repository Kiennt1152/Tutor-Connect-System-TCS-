package com.tcs.module.center.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Mẫu hợp đồng hiển thị cho trung tâm chọn/quản lý. */
@Getter
@Builder
public class ContractTemplateResponse {

    private Long templateId;
    private String name;
    private String content;
    /** RECRUITMENT (tuyển dụng/hợp tác) hoặc CLASS (học viên/dạy lớp). Mặc định CLASS. */
    private String contractType;
    private Boolean defaultTemplate;
    private String status;
    /** true = mẫu hệ thống dùng chung (không sửa được); false = mẫu của trung tâm. */
    private Boolean system;
}
