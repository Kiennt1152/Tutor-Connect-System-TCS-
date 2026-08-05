package com.tcs.module.platform.dto.request;

import com.tcs.module.contract.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerateReviewRequest {

    @NotNull(message = "Trạng thái kiểm duyệt là bắt buộc")
    private ReviewStatus status;
}
