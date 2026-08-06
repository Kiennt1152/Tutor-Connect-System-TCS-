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
    private Boolean defaultTemplate;
    private String status;
    /** true = mẫu hệ thống dùng chung (không sửa được); false = mẫu của trung tâm. */
    private Boolean system;
}
