package com.tcs.module.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractViewResponse {

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
    private String paymentMethod;
    private String myRole; // CLIENT | TUTOR

    private String termsB;
}
