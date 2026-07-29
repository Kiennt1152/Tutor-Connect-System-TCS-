package com.tcs.module.platform.dto.request;

import com.tcs.module.profile.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpsertAnnouncementRequest {

    @NotBlank(message = "Tiêu đề là bắt buộc.")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự.")
    private String title;

    @NotBlank(message = "Nội dung là bắt buộc.")
    private String content;

    private UserRole targetRole;

    private Boolean active;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;
}
