package com.tcs.module.platform.dto.request;

import com.tcs.module.identity.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewVerificationRequest {

    @NotNull(message = "Trạng thái xác minh không được để trống")
    private VerificationStatus status;

    private String adminNotes;

    /**
     * Optimistic locking: giá trị updatedAt của hồ sơ lúc admin mở xem.
     * Nếu khác với DB (người khác vừa sửa) thì chặn ghi đè. Có thể null (bỏ qua kiểm tra).
     */
    private LocalDateTime expectedUpdatedAt;
}
