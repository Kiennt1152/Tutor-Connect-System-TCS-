package com.tcs.module.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatbotAskRequest {

    @NotBlank(message = "Vui lòng nhập câu hỏi")
    @Size(max = 500, message = "Câu hỏi không được vượt quá 500 ký tự")
    private String question;
}
