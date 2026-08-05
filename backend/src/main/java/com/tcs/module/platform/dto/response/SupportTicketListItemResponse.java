package com.tcs.module.platform.dto.response;

import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SupportTicketListItemResponse {

    private Long ticketId;
    private Long userId;
    private String userEmail;
    private Long assignedAdminId;
    private String assignedAdminName;
    private SupportTicketCategory category;
    private String subject;
    private SupportTicketPriority priority;
    private SupportTicketStatus status;
    private LocalDateTime dueAt;
    private Boolean slaBreached;
    private Long responseSlaMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
