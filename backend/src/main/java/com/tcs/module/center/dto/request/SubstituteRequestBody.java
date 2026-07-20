package com.tcs.module.center.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Gia sư chính nhờ gia sư phụ dạy thay buổi {@code date} (báo ốm/bận, vẫn dạy đúng hôm đó). */
@Getter
@Setter
public class SubstituteRequestBody {

    private LocalDate date;
    private String reason;
}
