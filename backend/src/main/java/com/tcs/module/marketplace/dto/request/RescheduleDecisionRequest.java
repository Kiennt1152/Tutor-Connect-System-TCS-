package com.tcs.module.marketplace.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Quyết định của bên còn lại với một yêu cầu đổi lịch/thêm buổi. */
@Getter
@Setter
public class RescheduleDecisionRequest {

    /** true = duyệt (áp dụng lịch mới), false = từ chối. */
    private Boolean approve;

    /** Ghi chú kèm quyết định — nhất là khi từ chối. */
    private String note;
}
