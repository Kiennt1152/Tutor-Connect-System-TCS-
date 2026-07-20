package com.tcs.module.center.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Điểm danh hàng loạt cho một buổi học. */
@Getter
@Setter
public class BatchAttendanceRequest {

    private List<MarkAttendanceRequest> records;
}
