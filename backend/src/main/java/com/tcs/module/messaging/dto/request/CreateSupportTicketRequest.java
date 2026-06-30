package com.tcs.module.messaging.dto.request;

import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSupportTicketRequest {

    private SupportTicketCategory category;
    private String subject;
    private String description;
    private SupportTicketPriority priority;
    private Long targetClassId;
}
