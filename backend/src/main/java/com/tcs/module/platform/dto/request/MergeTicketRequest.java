package com.tcs.module.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MergeTicketRequest {

    @NotNull(message = "Mã ticket đích cần gộp không được để trống")
    private Long targetTicketId;

    private String reason;
}
