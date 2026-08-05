package com.tcs.module.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RespondTicketRequest {

    @NotBlank(message = "Vui lòng nhập nội dung phản hồi")
    @Size(max = 5000, message = "Nội dung phản hồi không được vượt quá 5000 ký tự")
    private String content;
}
