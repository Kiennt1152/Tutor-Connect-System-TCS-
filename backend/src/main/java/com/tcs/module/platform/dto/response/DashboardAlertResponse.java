package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardAlertResponse {
    String type;
    String title;
    String message;
    String actionUrl;
}
