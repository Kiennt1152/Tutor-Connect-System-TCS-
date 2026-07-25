package com.tcs.module.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpsertSystemParameterRequest {

    @NotBlank(message = "Khóa tham số là bắt buộc.")
    private String paramKey;

    @NotBlank(message = "Giá trị tham số là bắt buộc.")
    private String paramValue;

    private String description;
}
