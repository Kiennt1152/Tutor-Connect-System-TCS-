package com.tcs.module.marketplace.dto.request;

import java.math.BigDecimal;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * Phụ huynh gửi yêu cầu mở lớp tới một trung tâm cụ thể (nguyện vọng ngắn gọn).
 * Trung tâm sẽ bổ sung chi tiết còn lại khi chấp nhận.
 */
@Getter
@Setter
public class ClassRequestCreateRequest {

    /** Môn học / danh mục mong muốn. */
    private Long categoryId;

    /** Nội dung nguyện vọng (lịch mong muốn, trình độ học sinh, địa điểm, ghi chú...). */
    private String note;

    /** Ngân sách mong muốn (tuỳ chọn). */
    private BigDecimal desiredBudget;

    /** Nguyên payload form "tìm gia sư" của phụ huynh (JSON) — môn, lịch, địa điểm… (tuỳ chọn). */
    private String detailsJson;

    /** Tài khoản nhận hoàn tiền/hoàn phí nếu yêu cầu không được hoàn thành. */
    private RefundPayoutInfo refundPayoutInfo;
}
