package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Trung tâm duyệt (HIRED) hoặc từ chối (REJECTED) một đơn ứng tuyển. */
@Getter
@Setter
public class ApplicationDecisionBody {

    private boolean approve;
    /** BF-03: mẫu hợp đồng (loại tuyển dụng) center chọn khi duyệt để gửi gia sư ký. Tuỳ chọn. */
    private Long contractTemplateId;
    /** BF-03: nội dung điều khoản center tự nhập/sửa khi duyệt (đã nạp sẵn từ mẫu). Tuỳ chọn. */
    private String contractContent;
}
