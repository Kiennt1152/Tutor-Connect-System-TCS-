package com.tcs.module.contract.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.dto.request.SignWithOtpRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.ContractSignatureListResponse;
import com.tcs.module.contract.dto.response.ContractSignatureResponse;
import com.tcs.module.contract.dto.response.OtpSentResponse;
import com.tcs.module.contract.dto.response.SignatureStatusResponse;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.entity.ContractSignature;
import com.tcs.module.contract.entity.ContractTemplate;
import com.tcs.module.contract.enums.ContractSignatureStatus;
import com.tcs.module.contract.enums.ContractSourceType;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.notification.service.EmailService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int CONTRACT_EXPIRY_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ContractRepository contractRepository;
    private final ContractSignatureRepository contractSignatureRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final TutorRepository tutorRepository;
    private final ClientRepository clientRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final EmailService emailService;
    private final AuthHelper authHelper;

    // ─── VIEW CONTRACT (4.2) ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContract(Long contractId) {
        Contract contract = findContract(contractId);
        validateViewPermission(contract);
        return toContractResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts() {
        Long userId = authHelper.currentUserId();
        List<Contract> contracts = contractRepository.findContractsByUserId(userId);
        return contracts.stream().map(this::toContractResponse).toList();
    }

    // ─── SIGNATURES (4.4) ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ContractSignatureListResponse getSignatures(Long contractId) {
        Contract contract = findContract(contractId);
        validateViewPermission(contract);

        List<ContractSignature> signatures = contractSignatureRepository.findByContractId(contractId);
        List<ContractSignatureResponse> sigResponses = signatures.stream()
                .map(this::toSignatureResponse)
                .toList();

        int required = getRequiredSignatureCount(contract);
        int signed = (int) signatures.stream()
                .filter(s -> s.getSignatureStatus() == ContractSignatureStatus.SIGNED)
                .count();

        return ContractSignatureListResponse.builder()
                .contractId(contractId)
                .contractNo(contract.getContractNo())
                .hasAllSignatures(signed >= required)
                .signedCount(signed)
                .requiredSignatures(required)
                .signatures(sigResponses)
                .build();
    }

    // ─── SEND OTP (4.3 - step 1) ──────────────────────────────────────────────

    @Override
    @Transactional
    public Map<String, Object> sendOtp(Long contractId) {
        Contract contract = findContract(contractId);
        PartyRole role = resolvePartyRole(contract);

        if (contract.getStatus() != ContractStatus.PENDING) {
            throw new IllegalStateException("Hợp đồng không ở trạng thái chờ ký");
        }

        Optional<ContractSignature> existingOpt = contractSignatureRepository
                .findByContractIdAndPartyRole(contractId, role);

        if (existingOpt.isPresent()) {
            ContractSignature existing = existingOpt.get();
            if (existing.getSignatureStatus() == ContractSignatureStatus.SIGNED) {
                throw new IllegalStateException("Bạn đã ký hợp đồng này rồi");
            }
        }

        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        String recipientEmail = authHelper.requireAuthenticated().getEmail();

        ContractSignature signature = existingOpt.orElseGet(() -> {
            ContractSignature s = new ContractSignature();
            s.setContract(contract);
            s.setPartyRole(role);
            s.setSignatureStatus(ContractSignatureStatus.PENDING);
            s.setOtpAttempts(0);
            return s;
        });

        signature.setOtpCode(otp);
        signature.setOtpExpiresAt(expiresAt);
        signature.setEmail(recipientEmail);
        contractSignatureRepository.save(signature);

        sendOtpEmail(recipientEmail, otp, contract.getContractNo());

        return Map.of(
                "message", "Mã OTP đã được gửi đến email của bạn",
                "expiresInMinutes", OTP_EXPIRY_MINUTES,
                "maxAttempts", OTP_MAX_ATTEMPTS
        );
    }

    // ─── SIGN WITH OTP (4.3 - step 2) ───────────────────────────────────────

    @Override
    @Transactional
    public ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request) {
        if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new IllegalArgumentException("Mã OTP là bắt buộc");
        }

        Contract contract = findContract(contractId);
        PartyRole role = resolvePartyRole(contract);

        if (contract.getStatus() != ContractStatus.PENDING) {
            throw new IllegalStateException("Hợp đồng không ở trạng thái chờ ký");
        }

        ContractSignature signature = contractSignatureRepository
                .findByContractIdAndPartyRole(contractId, role)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa gửi mã OTP cho vai trò này"));

        if (signature.getSignatureStatus() == ContractSignatureStatus.SIGNED) {
            throw new IllegalStateException("Bạn đã ký hợp đồng này rồi");
        }

        if (signature.getOtpAttempts() >= OTP_MAX_ATTEMPTS) {
            signature.setSignatureStatus(ContractSignatureStatus.EXPIRED);
            contractSignatureRepository.save(signature);
            throw new IllegalStateException("Đã vượt quá số lần thử. Vui lòng yêu cầu mã mới.");
        }

        if (signature.getOtpExpiresAt() != null
                && signature.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            signature.setSignatureStatus(ContractSignatureStatus.EXPIRED);
            contractSignatureRepository.save(signature);
            throw new IllegalStateException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (!signature.getOtpCode().equals(request.getOtpCode().trim())) {
            signature.setOtpAttempts(signature.getOtpAttempts() + 1);
            contractSignatureRepository.save(signature);
            int remaining = OTP_MAX_ATTEMPTS - signature.getOtpAttempts();
            throw new IllegalArgumentException(
                    "Mã OTP không đúng. Còn " + remaining + " lần thử.");
        }

        Long userId = authHelper.currentUserId();
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now());
        signature.setSignatureData("OTP_VERIFIED:" + request.getOtpCode().trim());
        signature.setOtpCode(null);
        signature.setOtpExpiresAt(null);
        contractSignatureRepository.save(signature);

        int required = getRequiredSignatureCount(contract);
        int signed = contractSignatureRepository.countSignedByContractId(contractId);

        if (signed >= required) {
            contract.setStatus(ContractStatus.SIGNED);
            contract.setSignedAt(LocalDateTime.now());
            contractRepository.save(contract);
        }

        return toContractResponse(contract);
    }

    // ─── GENERATE CONTRACT (4.1) ────────────────────────────────────────────

    @Override
    @Transactional
    public ContractResponse generateContract(Long assignmentId) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        if (contractRepository.findByAssignmentId(assignmentId).isPresent()) {
            throw new IllegalStateException("Hợp đồng đã tồn tại cho phân công này");
        }

        ContractTemplate template = contractTemplateRepository.findAll().stream()
                .filter(t -> t.getStatus().name().equals("ACTIVE"))
                .findFirst()
                .orElse(null);

        String contractNo = generateContractNo();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(CONTRACT_EXPIRY_DAYS);

        Contract contract = new Contract();
        contract.setContractNo(contractNo);
        contract.setAssignment(assignment);
        contract.setTemplate(template);
        contract.setStatus(ContractStatus.PENDING);
        contract.setSourceType(ContractSourceType.PRIVATE);
        contract.setExpiresAt(expiresAt);
        if (template != null) {
            contract.setTermsSummary(template.getContent());
        }
        contract = contractRepository.save(contract);

        initializeSignatureSlots(contract, assignment);

        return toContractResponse(contract);
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────

    private Contract findContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
    }

    private void validateViewPermission(Contract contract) {
        Long currentUserId = authHelper.currentUserId();
        boolean isParty = isPartyOfContract(contract, currentUserId);
        if (!isParty) {
            throw new ForbiddenException("Bạn không có quyền xem hợp đồng này");
        }
    }

    private boolean isPartyOfContract(Contract contract, Long userId) {
        if (contract.getAssignment() == null) {
            return false;
        }
        TutorApplication application = contract.getAssignment().getApplication();
        if (application == null) {
            return false;
        }
        TutoringClass tutoringClass = application.getTutoringClass();
        if (tutoringClass == null || tutoringClass.getCreator() == null) {
            return false;
        }
        Long creatorUserId = tutoringClass.getCreator().getUserId();
        Long tutorUserId = application.getTutor().getUser().getUserId();
        return userId.equals(creatorUserId) || userId.equals(tutorUserId);
    }

    private PartyRole resolvePartyRole(Contract contract) {
        Long currentUserId = authHelper.currentUserId();
        TutorApplication application = contract.getAssignment().getApplication();
        TutoringClass tutoringClass = application.getTutoringClass();

        Long tutorUserId = application.getTutor().getUser().getUserId();
        if (currentUserId.equals(tutorUserId)) {
            return PartyRole.TUTOR;
        }

        if (tutoringClass.getCreator().getUserId().equals(currentUserId)) {
            return tutorCenterRepository.findByUser_UserId(currentUserId).isPresent()
                    ? PartyRole.CENTER : PartyRole.CLIENT;
        }

        throw new ForbiddenException("Bạn không phải là bên liên quan đến hợp đồng này");
    }

    private int getRequiredSignatureCount(Contract contract) {
        return 2;
    }

    private void initializeSignatureSlots(Contract contract, ClassAssignment assignment) {
        TutorApplication application = assignment.getApplication();
        TutoringClass tutoringClass = application.getTutoringClass();
        Long creatorUserId = tutoringClass.getCreator().getUserId();

        Tutor tutor = application.getTutor();
        ContractSignature tutorSig = new ContractSignature();
        tutorSig.setContract(contract);
        tutorSig.setPartyRole(PartyRole.TUTOR);
        tutorSig.setEmail(tutor.getUser().getEmail());
        tutorSig.setSignatureStatus(ContractSignatureStatus.PENDING);
        tutorSig.setOtpAttempts(0);
        contractSignatureRepository.save(tutorSig);

        PartyRole creatorRole = tutorCenterRepository.findByUser_UserId(creatorUserId).isPresent()
                ? PartyRole.CENTER : PartyRole.CLIENT;

        String creatorEmail;
        if (creatorRole == PartyRole.CENTER) {
            TutorCenter center = tutorCenterRepository.findByUser_UserId(creatorUserId).orElseThrow();
            creatorEmail = center.getUser().getEmail();
        } else {
            Client client = clientRepository.findByUser_UserId(creatorUserId).orElseThrow();
            creatorEmail = client.getUser().getEmail();
        }

        ContractSignature creatorSig = new ContractSignature();
        creatorSig.setContract(contract);
        creatorSig.setPartyRole(creatorRole);
        creatorSig.setEmail(creatorEmail);
        creatorSig.setSignatureStatus(ContractSignatureStatus.PENDING);
        creatorSig.setOtpAttempts(0);
        contractSignatureRepository.save(creatorSig);
    }

    private String generateOtp() {
        int otp = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    private String generateContractNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long todayCount = contractRepository.countTodayContracts() + 1;
        return String.format("TCS-%s-%04d", datePart, todayCount);
    }

    private void sendOtpEmail(String email, String otp, String contractNo) {
        emailService.sendContractOtp(email, otp, contractNo);
    }

    private ContractResponse toContractResponse(Contract contract) {
        ContractResponse.ContractResponseBuilder builder = ContractResponse.builder()
                .contractId(contract.getContractId())
                .contractNo(contract.getContractNo())
                .status(contract.getStatus())
                .sourceType(contract.getSourceType())
                .templateId(contract.getTemplate() != null ? contract.getTemplate().getTemplateId() : null)
                .templateName(contract.getTemplate() != null ? contract.getTemplate().getName() : null)
                .termsSummary(contract.getTermsSummary())
                .contractFileUrl(contract.getContractFileUrl())
                .signedAt(contract.getSignedAt())
                .expiresAt(contract.getExpiresAt())
                .confirmedAt(contract.getConfirmedAt())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt());

        if (contract.getAssignment() != null) {
            builder.assignmentId(contract.getAssignment().getAssignmentId());
            TutorApplication application = contract.getAssignment().getApplication();
            if (application != null) {
                TutoringClass tutoringClass = application.getTutoringClass();
                if (tutoringClass != null) {
                    builder.classId(tutoringClass.getClassId());

                    Tutor tutor = application.getTutor();
                    if (tutor != null) {
                        builder.tutorId(tutor.getUser().getUserId());
                        builder.tutorName(tutor.getFullName());
                        builder.tutorEmail(tutor.getUser().getEmail());
                    }

                    if (tutoringClass.getCreator() != null) {
                        Long creatorUserId = tutoringClass.getCreator().getUserId();
                        Optional<Client> clientOpt = clientRepository.findByUser_UserId(creatorUserId);
                        Optional<TutorCenter> centerOpt = tutorCenterRepository.findByUser_UserId(creatorUserId);

                        if (centerOpt.isPresent()) {
                            TutorCenter center = centerOpt.get();
                            builder.centerId(center.getCenterId());
                            builder.centerName(center.getCompanyName());
                            builder.centerEmail(center.getUser().getEmail());
                        } else if (clientOpt.isPresent()) {
                            Client client = clientOpt.get();
                            builder.clientId(client.getClientId());
                            builder.clientName(client.getFullName());
                            builder.clientEmail(client.getUser().getEmail());
                        }
                    }
                }
            }
        }

        int required = getRequiredSignatureCount(contract);
        int signed = contractSignatureRepository.countSignedByContractId(contract.getContractId());
        builder.requiredSignatures(required)
                .signedCount(signed)
                .hasAllSignatures(signed >= required);

        return builder.build();
    }

    private ContractSignatureResponse toSignatureResponse(ContractSignature sig) {
        ContractSignatureResponse.ContractSignatureResponseBuilder builder =
                ContractSignatureResponse.builder()
                        .signatureId(sig.getSignatureId())
                        .partyRole(sig.getPartyRole())
                        .partyLabel(switch (sig.getPartyRole()) {
                            case CLIENT -> "Học viên / Phụ huynh";
                            case TUTOR -> "Gia sư";
                            case CENTER -> "Trung tâm";
                        })
                        .signatureStatus(sig.getSignatureStatus())
                        .signedAt(sig.getSignedAt())
                        .otpExpiresAt(sig.getOtpExpiresAt())
                        .remainingOtpAttempts(OTP_MAX_ATTEMPTS - sig.getOtpAttempts());

        if (sig.getSigner() != null) {
            builder.signerId(sig.getSigner().getUserId())
                    .signerEmail(sig.getSigner().getEmail());
        } else if (sig.getEmail() != null) {
            builder.signerEmail(sig.getEmail());
        }

        // Resolve signer name
        String signerName = null;
        if (sig.getContract() != null && sig.getContract().getAssignment() != null) {
            TutorApplication application = sig.getContract().getAssignment().getApplication();
            if (application != null) {
                if (sig.getPartyRole() == PartyRole.TUTOR && application.getTutor() != null) {
                    signerName = application.getTutor().getFullName();
                } else if (application.getTutoringClass() != null && application.getTutoringClass().getCreator() != null) {
                    Long creatorUserId = application.getTutoringClass().getCreator().getUserId();
                    if (sig.getPartyRole() == PartyRole.CENTER) {
                        signerName = tutorCenterRepository.findByUser_UserId(creatorUserId).map(TutorCenter::getCompanyName).orElse(null);
                    } else if (sig.getPartyRole() == PartyRole.CLIENT) {
                        signerName = clientRepository.findByUser_UserId(creatorUserId).map(Client::getFullName).orElse(null);
                    }
                }
            }
        }
        builder.signerName(signerName);

        if (sig.getOtpExpiresAt() != null) {
            builder.isOtpExpired(sig.getOtpExpiresAt().isBefore(LocalDateTime.now()));
        }

        return builder.build();
    }


}
