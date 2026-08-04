package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Trung tâm từ chối một yêu cầu mở lớp của phụ huynh, kèm lý do. */
@Getter
@Setter
public class RejectClassRequestBody {

    private String reason;
}
