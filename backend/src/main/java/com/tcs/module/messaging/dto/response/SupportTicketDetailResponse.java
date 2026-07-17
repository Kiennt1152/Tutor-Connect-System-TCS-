package com.tcs.module.messaging.dto.response;

import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SupportTicketDetailResponse {

    private Long ticketId;
    private Long userId;
    private Long targetClassId;
    private Long assignedAdminId;
    private SupportTicketCategory category;
    private String subject;
    private String description;
    private String evidenceUrls;
    private SupportTicketPriority priority;
    private SupportTicketStatus status;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TicketMessageResponse> messages;
}
