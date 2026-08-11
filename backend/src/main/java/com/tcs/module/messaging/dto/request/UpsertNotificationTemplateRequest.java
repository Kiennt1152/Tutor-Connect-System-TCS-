package com.tcs.module.messaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpsertNotificationTemplateRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_\\-]+", message = "Mã template chỉ gồm chữ, số, gạch ngang và gạch dưới.")
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 200)
    private String titleTemplate;

    @NotBlank
    private String contentTemplate;

    @NotBlank
    @Pattern(regexp = "IN_APP|EMAIL", message = "Kênh thông báo không hợp lệ.")
    private String channel;

    @Size(max = 500)
    private String description;

    private Boolean enabled;
}
