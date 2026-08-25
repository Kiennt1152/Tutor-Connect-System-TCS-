package com.tcs.module.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpsertSystemParameterRequest {

    @NotBlank(message = "Khóa tham số là bắt buộc.")
    @Size(max = 100, message = "Khóa tham số tối đa 100 ký tự.")
    private String paramKey;

    @NotBlank(message = "Giá trị tham số là bắt buộc.")
    private String paramValue;

    private String description;
}
