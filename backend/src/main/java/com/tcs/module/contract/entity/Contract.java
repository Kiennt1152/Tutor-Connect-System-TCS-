package com.tcs.module.contract.entity;

import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.contract.enums.ContractSourceType;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "contract_no", length = 50, nullable = false, unique = true)
    private String contractNo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private ClassAssignment assignment;

    // Hop dong CENTER theo tung ghi danh (client <-> center).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_student_id", unique = true)
    private ClassStudent classStudent;

    // BF-03: thoa thuan hop tac center <-> gia su, gan voi don ung tuyen tuyen dung.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_application_id")
    private RecruitmentApplication recruitmentApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ContractTemplate template;

    @Column(name = "contract_file_url", length = 500)
    private String contractFileUrl;

    @Column(name = "terms_summary", columnDefinition = "TEXT")
    private String termsSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ContractStatus status = ContractStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20, nullable = false)
    private ContractSourceType sourceType = ContractSourceType.PRIVATE;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public ClassAssignment getAssignment() { return assignment; }
    public void setAssignment(ClassAssignment assignment) { this.assignment = assignment; }
    public ClassStudent getClassStudent() { return classStudent; }
    public void setClassStudent(ClassStudent classStudent) { this.classStudent = classStudent; }
    public RecruitmentApplication getRecruitmentApplication() { return recruitmentApplication; }
    public void setRecruitmentApplication(RecruitmentApplication recruitmentApplication) { this.recruitmentApplication = recruitmentApplication; }
    public ContractTemplate getTemplate() { return template; }
    public void setTemplate(ContractTemplate template) { this.template = template; }
    public String getContractFileUrl() { return contractFileUrl; }
    public void setContractFileUrl(String contractFileUrl) { this.contractFileUrl = contractFileUrl; }
    public String getTermsSummary() { return termsSummary; }
    public void setTermsSummary(String termsSummary) { this.termsSummary = termsSummary; }
    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }
    public ContractSourceType getSourceType() { return sourceType; }
    public void setSourceType(ContractSourceType sourceType) { this.sourceType = sourceType; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
