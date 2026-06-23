// Update the HomeResponse class to include a welcomeMessage
package com.tcs.common.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HomeResponse {
    private String welcomeMessage;
}