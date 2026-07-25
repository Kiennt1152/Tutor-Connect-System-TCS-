package com.tcs.module.platform.dto.request;

import com.tcs.module.platform.enums.AnnouncementAudience;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpsertAnnouncementRequest {

    @NotBlank(message = "Tiêu đề là bắt buộc.")
    private String title;

    @NotBlank(message = "Nội dung là bắt buộc.")
    private String content;

    private AnnouncementAudience audience;

    private Boolean published;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;
}
