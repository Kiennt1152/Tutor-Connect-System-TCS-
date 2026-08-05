package com.tcs.module.platform.dto.request;

import com.tcs.module.platform.enums.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloseTicketRequest {

    /** Chỉ chấp nhận RESOLVED hoặc CLOSED. */
    @NotNull(message = "Trạng thái không được để trống")
    private SupportTicketStatus status;

    private String adminNotes;
}
