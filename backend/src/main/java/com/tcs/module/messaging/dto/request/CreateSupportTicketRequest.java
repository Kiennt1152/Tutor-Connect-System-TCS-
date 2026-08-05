package com.tcs.module.messaging.dto.request;

import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSupportTicketRequest {

    @NotNull(message = "Vui lòng chọn danh mục yêu cầu")
    private SupportTicketCategory category;

    @NotBlank(message = "Vui lòng nhập tiêu đề")
    @Size(max = 150, message = "Tiêu đề không được vượt quá 150 ký tự")
    private String subject;

    @NotBlank(message = "Vui lòng nhập mô tả chi tiết")
    @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    /** Người dùng có thể tự đề xuất mức độ ưu tiên, hệ thống sẽ tự nâng lên theo category nếu cần (auto-escalate). */
    private SupportTicketPriority priority;

    private Long targetClassId;

    @Size(max = 2000, message = "Danh sách bằng chứng không được vượt quá 2000 ký tự")
    private String evidenceUrls;
}
