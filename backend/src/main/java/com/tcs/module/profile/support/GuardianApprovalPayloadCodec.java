package com.tcs.module.profile.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcs.module.profile.enums.GuardianApprovalActionType;
import com.tcs.module.profile.enums.GuardianApprovalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class GuardianApprovalPayloadCodec {

    public static final String REF_PARENT = "GUARDIAN_APPROVAL";
    public static final String REF_MINOR = "GUARDIAN_APPROVAL_STATUS";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public String encode(Payload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể mã hóa yêu cầu phụ huynh", exception);
        }
    }

    public Payload decode(String content) {
        try {
            return objectMapper.readValue(content, Payload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Dữ liệu yêu cầu phụ huynh không hợp lệ", exception);
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private GuardianApprovalActionType actionType;
        private GuardianApprovalStatus status;
        private Long minorUserId;
        private String minorName;
        private Long parentUserId;
        private String parentName;
        private BigDecimal amount;
        private String description;
        private String tutorName;
        private String subjectName;
        private String contractReference;
        private Long paymentTransactionId;
        private Long parentNotificationId;
        private LocalDateTime resolvedAt;
    }
}
