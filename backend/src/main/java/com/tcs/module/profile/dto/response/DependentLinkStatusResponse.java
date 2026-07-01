package com.tcs.module.profile.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DependentLinkStatusResponse {

    /** CLIENT chưa cung cấp ngày sinh — cần bổ sung trước khi xác định luồng. */
    private boolean dateOfBirthMissing;

    /** Tài khoản thuộc học sinh dưới 18 tuổi. */
    private boolean minorAccount;

    /** Học sinh vị thành niên bắt buộc liên kết phụ huynh. */
    private boolean guardianRequired;

    /** Đã liên kết ít nhất một phụ huynh (chỉ áp dụng khi minorAccount). */
    private boolean guardianLinked;

    /** Phụ huynh/người lớn có thể thêm hồ sơ con (tùy chọn). */
    private boolean childrenLinkOptional;

    /** Số hồ sơ con đã liên kết (chỉ áp dụng khi người lớn). */
    private int linkedChildrenCount;

    /** Được phép tiếp tục thanh toán / tạo hợp đồng. */
    private boolean canProceedToPayment;

    /** Thanh toán & thủ tục pháp lý chuyển sang tài khoản phụ huynh (học sinh vị thành niên). */
    private boolean legalProceduresDelegatedToParent;

    /** Học sinh vị thành niên: thao tác cần phụ huynh xác nhận sau khi thực hiện. */
    private boolean parentApprovalRequired;

    private Long legalAccountUserId;
    private String legalAccountHolderName;
    private String legalAccountEmail;
}
