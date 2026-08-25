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

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_status", length = 20, nullable = false)
    private ContractSignatureStatus signatureStatus = ContractSignatureStatus.PENDING;

    public Long getSignatureId() { return signatureId; }
    public void setSignatureId(Long signatureId) { this.signatureId = signatureId; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public PartyRole getPartyRole() { return partyRole; }
    public void setPartyRole(PartyRole partyRole) { this.partyRole = partyRole; }
    public User getSigner() { return signer; }
    public void setSigner(User signer) { this.signer = signer; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public String getSignatureData() { return signatureData; }
    public void setSignatureData(String signatureData) { this.signatureData = signatureData; }
    public ContractSignatureStatus getSignatureStatus() { return signatureStatus; }
    public void setSignatureStatus(ContractSignatureStatus signatureStatus) { this.signatureStatus = signatureStatus; }
}
