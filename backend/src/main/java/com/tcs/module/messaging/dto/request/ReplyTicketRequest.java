package com.tcs.module.messaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyTicketRequest {
    @NotBlank(message = "Nội dung phản hồi không được để trống")
    private String content;

    private String evidenceUrls;
}
