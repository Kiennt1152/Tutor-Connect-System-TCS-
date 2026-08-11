package com.tcs.module.messaging.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PreviewNotificationTemplateRequest {
    @NotBlank
    private String titleTemplate;

    @NotBlank
    private String contentTemplate;

    private Map<String, Object> variables;
}
