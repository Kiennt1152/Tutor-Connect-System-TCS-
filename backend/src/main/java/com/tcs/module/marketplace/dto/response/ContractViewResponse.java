package com.tcs.module.marketplace.dto.response;

import com.tcs.module.contract.dto.response.ContractResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractViewResponse {

    private Long contractId;
    private Long assignmentId;
    private Long classId;
    private String classTitle;
    private String detailsJson;
    private String gradeName;
    private String address;
    private String lessonMode;
    private LocalDate startDate;
    private LocalDate endDate;
    private long numberOfSessions;
    private List<String> subjectNames;
    private BigDecimal tuitionFee;
    private BigDecimal totalTuitionAmount;
    private BigDecimal escrowAmount;

    private String clientName;
    private String clientPhone;
    private String clientAddress;
    private LocalDate clientDob;
    private String clientCccd;

    private String tutorName;
    private String tutorPhone;
    private String tutorAddress;
    private LocalDate tutorDob;
    private String tutorCccd;

    private boolean tutorSigned;
    private boolean clientSigned;
    private LocalDateTime tutorSignedAt;
    private LocalDateTime clientSignedAt;
    private String paymentMethod;
    /**
     * Hạn 48 giờ để hai bên ký xong và client chuyển tiền ký quỹ. Quá hạn, hệ thống hủy hợp đồng
     * và mở lại lớp cho các gia sư đã ứng tuyển. Null = hợp đồng không còn đếm ngược.
     */
    private LocalDateTime matchDeadlineAt;
    private String myRole; // CLIENT | TUTOR
    private ContractResponse.EscrowPaymentInfo escrowPayment;
    private ContractResponse.RefundPayoutInfoView refundPayoutInfo;

    private String termsB;
}
