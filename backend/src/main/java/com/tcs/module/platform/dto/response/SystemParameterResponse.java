package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SystemParameterResponse {

    private Long parameterId;
    private String paramKey;
    private String paramValue;
    private String description;
}
