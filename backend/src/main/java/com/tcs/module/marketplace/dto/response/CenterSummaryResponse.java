package com.tcs.module.marketplace.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Thông tin gọn của một trung tâm đã xác minh, để phụ huynh chọn khi gửi yêu cầu mở lớp. */
@Getter
@Builder
public class CenterSummaryResponse {

    private Long centerId;
    private String companyName;
    private String description;
    private String address;
    private String phone;
    private String avatar;
}
