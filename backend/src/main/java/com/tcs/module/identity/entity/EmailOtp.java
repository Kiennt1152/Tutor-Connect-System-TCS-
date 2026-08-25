package com.tcs.module.identity.entity;

import com.tcs.module.identity.enums.OtpPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Ma OTP xac thuc email khi dang ky (UC-01, BR-UC01-04). Gan theo email
 * vi tai khoan chua duoc tao tai thoi diem gui ma.
 */
@Entity
@Table(name = "email_otps")
@Getter
@Setter
@NoArgsConstructor
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Long otpId;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "code", length = 10, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 30, nullable = false)
    private OtpPurpose purpose = OtpPurpose.REGISTRATION;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
