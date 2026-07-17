package com.tcs.module.platform.dto.request;

import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTicketRequest {

    /** Admin có thể phân loại lại category. Null = giữ nguyên. */
    private SupportTicketCategory category;

    /** Admin có thể nâng/hạ priority. Null = giữ nguyên. */
    private SupportTicketPriority priority;
}
