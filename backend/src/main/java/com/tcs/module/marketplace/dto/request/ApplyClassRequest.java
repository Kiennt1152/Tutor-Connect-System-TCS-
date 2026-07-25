package com.tcs.module.marketplace.dto.request;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyClassRequest {

    /**
     * Học phí đề xuất theo từng môn của lớp: key = subjectId (hoặc "other"), value = đ/giờ.
     * Gia sư phải điền đủ mọi môn của lớp.
     */
    private Map<String, BigDecimal> proposedRates;

    /** Mức chung — chỉ dùng cho client cũ; bỏ qua khi đã có {@link #proposedRates}. */
    private BigDecimal proposedRate;

    private String coverLetter;
}
