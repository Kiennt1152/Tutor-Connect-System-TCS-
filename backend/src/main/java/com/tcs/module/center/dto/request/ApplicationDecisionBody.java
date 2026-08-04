package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Trung tâm duyệt (HIRED) hoặc từ chối (REJECTED) một đơn ứng tuyển. */
@Getter
@Setter
public class ApplicationDecisionBody {

    private boolean approve;
}
