package com.tcs.module.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpsertFaqRequest {

    @NotBlank(message = "Câu hỏi là bắt buộc.")
    private String question;

    @NotBlank(message = "Câu trả lời là bắt buộc.")
    private String answer;

    private String category;

    private Integer sortOrder;

    private Boolean published;
}
