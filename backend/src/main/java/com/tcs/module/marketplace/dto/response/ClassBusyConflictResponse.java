package com.tcs.module.marketplace.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Các buổi của một lớp trùng thời gian bận của gia sư đang đăng nhập. */
@Getter
@Builder
public class ClassBusyConflictResponse {

    private Long classId;
    private int conflictCount;
    /** Tóm tắt để hiện thẳng lên giao diện, ví dụ "15/09 (12:00–12:30)". */
    private String summary;
    private List<BusyConflictResponse> conflicts;
}
