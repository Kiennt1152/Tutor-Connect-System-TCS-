package com.tcs.module.center.dto.response;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/** Một yêu cầu nhờ gia sư phụ dạy thay để hiển thị cho gia sư và trung tâm. */
@Getter
@Builder
public class SubstitutionResponse {

    private Long classId;
    private String className;
    private LocalDate date;
    private String status; // PENDING | APPROVED | REJECTED
    private String reason;
    /** Gia sư chính (người xin dạy thay). */
    private Long mainTutorId;
    private String mainTutorName;
    /** Gia sư phụ được nhờ dạy thay. */
    private Long assistantTutorId;
    private String assistantTutorName;
}
