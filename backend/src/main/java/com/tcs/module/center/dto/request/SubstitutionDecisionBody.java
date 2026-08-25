package com.tcs.module.center.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Trung tâm duyệt/từ chối một yêu cầu nhờ gia sư phụ dạy thay. */
@Getter
@Setter
public class SubstitutionDecisionBody {

    private Long classId;
    private LocalDate date;
    private boolean approve;
}
