package com.tcs.module.marketplace.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

/** Một yêu cầu đổi lịch / thêm buổi hiển thị cho cả hai bên (UC-36). */
@Getter
@Builder
public class RescheduleRequestResponse {

    private Long requestId;
    private Long classId;
    private String classTitle;

    /** RESCHEDULE | EXTRA */
    private String requestType;

    /** PENDING | APPROVED | REJECTED | CANCELLED */
    private String status;

    /** Buổi bị dời — null với yêu cầu thêm buổi. */
    private Long lessonId;
    private LocalDate oldDate;
    private LocalTime oldStartTime;
    private LocalTime oldEndTime;

    private LocalDate newDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;
    private String subjectName;

    private String reason;
    private String requestedByName;
    private LocalDateTime createdAt;

    private String decidedByName;
    private LocalDateTime decidedAt;
    private String decisionNote;

    /** true khi người đang đăng nhập là bên phải duyệt và yêu cầu còn chờ. */
    private boolean canDecide;

    /** true khi người đang đăng nhập là người gửi và yêu cầu còn chờ — cho phép thu hồi. */
    private boolean canCancel;
}
