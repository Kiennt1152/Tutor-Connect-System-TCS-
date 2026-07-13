package com.tcs.module.contract.entity;

import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contract_signatures")
@Getter
@Setter
@NoArgsConstructor
public class ContractSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signature_id")
    private Long signatureId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_role", length = 20, nullable = false)
    private PartyRole partyRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id")
    private User signer;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_attempts", nullable = false)
    private Integer otpAttempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_status", length = 20, nullable = false)
    private ContractSignatureStatus signatureStatus = ContractSignatureStatus.PENDING;
}
