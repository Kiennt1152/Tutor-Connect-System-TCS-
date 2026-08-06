package com.tcs.module.contract.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.tcs.common.event.ContractSigned;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.BusinessException;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.request.ReviewCriterionDto;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.entity.ReputationHistory;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.contract.enums.ReviewType;
import com.tcs.module.contract.repository.ReputationHistoryRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.profile.enums.UserRole;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.notification.service.EmailService;
import com.tcs.module.platform.service.AuditLogService;
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
import java.time.temporal.ChronoUnit;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_ANONYMOUS_NAME = "Người dùng ẩn danh";
    private static final int REVIEW_REQUIRED_WITHIN_MONTHS = 1;

    private final ContractRepository contractRepository;
    private final ContractSignatureRepository contractSignatureRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final TutorRepository tutorRepository;
    private final ClientRepository clientRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final EmailService emailService;
    private final EscrowService escrowService;
    private final ApplicationEventPublisher eventPublisher;
    private final ReviewRepository reviewRepository;
    private final ReputationHistoryRepository reputationHistoryRepository;
    private final LessonRepository lessonRepository;
    private final AuthHelper authHelper;
    private final AuditLogService auditLogService;

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

        auditLogService.record(authHelper.currentUserId(), "SEND_CONTRACT_OTP", "Contract", contractId,
                null, Map.of("partyRole", role.name()));

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
            contract = contractRepository.save(contract);
            handleFullySignedContract(contract);
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

    private void handleFullySignedContract(Contract contract) {
        if (contract.getSourceType() != ContractSourceType.PRIVATE || contract.getAssignment() == null) {
            return;
        }

        ClassAssignment assignment = contract.getAssignment();
        TutorApplication application = assignment.getApplication();
        if (application == null || application.getTutoringClass() == null || application.getTutor() == null) {
            return;
        }

        TutoringClass tutoringClass = application.getTutoringClass();
        if (tutoringClass.getClassType() != ClassType.PRIVATE) {
            return;
        }

        User payer = tutoringClass.getCreator();
        Tutor tutor = application.getTutor();
        if (payer == null || payer.getUserId() == null || tutor.getUser() == null) {
            throw new BusinessException("Không xác định được bên thanh toán hoặc bên nhận tiền của hợp đồng");
        }

        Long payerUserId = payer.getUserId();
        if (clientRepository.findByUser_UserId(payerUserId).isEmpty()) {
            return;
        }

        BigDecimal managedEscrowAmount = firstManagedEscrowAmount(tutoringClass);
        escrowService.lock(new EscrowLockCommand(
                payerUserId,
                managedEscrowAmount,
                assignment.getAssignmentId(),
                null));

        eventPublisher.publishEvent(new ContractSigned(
                contract.getContractId(),
                tutoringClass.getClassId(),
                payerUserId,
                tutor.getUser().getUserId(),
                managedEscrowAmount,
                assignment.getAssignmentId(),
                null));
    }

    private BigDecimal firstManagedEscrowAmount(TutoringClass tutoringClass) {
        BigDecimal totalAmount = positiveAmount(tutoringClass.getBudget());
        if (totalAmount == null && tutoringClass.getTuitionFee() != null
                && tutoringClass.getNumberOfSessions() != null) {
            totalAmount = tutoringClass.getTuitionFee()
                    .multiply(BigDecimal.valueOf(tutoringClass.getNumberOfSessions()));
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Không xác định được học phí để khóa escrow");
        }

        int plannedMonths = plannedPrivateClassMonths(tutoringClass);
        if (plannedMonths <= 1) {
            return normalizeMoney(totalAmount);
        }
        return normalizeMoney(totalAmount.divide(BigDecimal.valueOf(plannedMonths), 2, RoundingMode.HALF_UP));
    }

    private int plannedPrivateClassMonths(TutoringClass tutoringClass) {
        JsonNode details = readClassDetails(tutoringClass.getDetailsJson());
        String scheduleMode = details.path("scheduleMode").asText("");
        if (!"CUSTOM".equalsIgnoreCase(scheduleMode)) {
            String billingCycle = details.path("billingCycle").asText("");
            if ("MONTH".equalsIgnoreCase(billingCycle)) {
                int months = Math.max(1, details.path("months").asInt(1));
                String durationUnit = details.path("durationUnit").asText("");
                return "YEAR".equalsIgnoreCase(durationUnit) ? months * 12 : months;
            }
            if ("TERM".equalsIgnoreCase(billingCycle)) {
                return 3;
            }
            if ("QUARTER".equalsIgnoreCase(billingCycle)) {
                return 6;
            }
            if ("YEAR".equalsIgnoreCase(billingCycle)) {
                return 12;
            }
        }
        return monthsBetween(tutoringClass.getStartDate(), tutoringClass.getEndDate());
    }

    private JsonNode readClassDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return OBJECT_MAPPER.createObjectNode();
        }
        try {
            return OBJECT_MAPPER.readTree(detailsJson);
        } catch (Exception e) {
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    private int monthsBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return 1;
        }
        long inclusiveDays = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1));
        return Math.max(1, (int) ((inclusiveDays + 30) / 31));
    }

    private BigDecimal positiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? amount : null;
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
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
                        .partyLabel(sig.getPartyRole() != null ? switch (sig.getPartyRole()) {
                            case CLIENT -> "Học viên / Phụ huynh";
                            case TUTOR -> "Gia sư";
                            case CENTER -> "Trung tâm";
                        } : "Người ký")
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


    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + email.substring(at);
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    // ===== Reviews & Reputation =====
    @Override
    @Transactional
    public ReviewResponse replyToReview(Long reviewId, ReplyReviewRequest request) {
        Long tutorUserId = authHelper.requireRole(UserRole.TUTOR).getUserId();

        Review review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        if (review.getReviewType() != ReviewType.CLIENT_TO_TUTOR
                || !review.getReviewee().getUserId().equals(tutorUserId)) {
            throw new BusinessException("Bạn chỉ có thể phản hồi đánh giá dành cho chính mình");
        }

        String reply = trimToNull(request.getReply());
        if (reply == null) {
            throw new IllegalArgumentException("Nội dung phản hồi không được để trống");
        }

        review.setTutorReply(reply);
        review.setTutorReplyAt(java.time.LocalDateTime.now());
        Review saved = reviewRepository.save(review);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, CreateReviewRequest request) {
        Long reviewerId = authHelper.requireRole(UserRole.CLIENT).getUserId();

        Review review = reviewRepository
                .findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        if (review.getReviewType() != ReviewType.CLIENT_TO_TUTOR
                || !review.getReviewer().getUserId().equals(reviewerId)) {
            throw new BusinessException("Bạn chỉ có thể chỉnh sửa đánh giá do chính mình gửi");
        }

        BigDecimal overallRating = resolveOverallRating(request);
        if (overallRating.compareTo(BigDecimal.ONE) < 0
                || overallRating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        }

        review.setRating(overallRating);
        review.setComment(trimToNull(request.getComment()));
        review.setCriteriaJson(serializeCriteria(request.getCriteria()));
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        review.setAnonymous(anonymous);
        review.setDisplayName(anonymous ? trimToNull(request.getDisplayName()) : null);
        Review saved = reviewRepository.save(review);

        Tutor tutor = review.getAssignment().getTutor();
        recomputeTutorReputation(tutor, review.getReviewee().getUserId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TutorReputationResponse getTutorReputation(Long tutorId) {
        Tutor tutor = tutorRepository
                .findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư"));
        return buildReputation(tutor);
    }

    @Override
    @Transactional(readOnly = true)
    public TutorReputationResponse getMyTutorReputation() {
        Long userId = authHelper.requireRole(UserRole.TUTOR).getUserId();
        Tutor tutor = tutorRepository
                .findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
        return buildReputation(tutor);
    }

    private TutorReputationResponse buildReputation(Tutor tutor) {
        Long tutorUserId = tutor.getUser().getUserId();

        List<Review> visible = reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                tutorUserId, ReviewType.CLIENT_TO_TUTOR, ReviewStatus.VISIBLE);

        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(star, 0);
        }
        for (Review r : visible) {
            int star = r.getRating().setScale(0, RoundingMode.HALF_UP).intValue();
            star = Math.min(5, Math.max(1, star));
            distribution.merge(star, 1, Integer::sum);
        }

        List<ReviewResponse> reviews = visible.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();

        return TutorReputationResponse.builder()
                .tutorId(tutor.getTutorId())
                .tutorUserId(tutorUserId)
                .fullName(tutor.getFullName())
                .avatar(tutor.getAvatar())
                .bio(tutor.getBio())
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(tutor.getHourlyRate())
                .verificationStatus(
                        tutor.getVerificationStatus() != null
                                ? tutor.getVerificationStatus().name()
                                : null)
                .ratingAvg(tutor.getRatingAvg() != null ? tutor.getRatingAvg() : BigDecimal.ZERO)
                .totalReviews(visible.size())
                .ratingDistribution(distribution)
                .criteriaAverages(criteriaAverages(visible))
                .reviews(reviews)
                .build();
    }

    private List<TutorReputationResponse.CriterionAverage> criteriaAverages(List<Review> reviews) {
        Map<String, int[]> sumCount = new LinkedHashMap<>();
        Map<String, String> questions = new LinkedHashMap<>();
        for (Review r : reviews) {
            if (r.getCriteriaJson() == null || r.getCriteriaJson().isBlank()) {
                continue;
            }
            List<ReviewCriterionDto> criteria;
            try {
                criteria = OBJECT_MAPPER.readValue(
                        r.getCriteriaJson(), new TypeReference<List<ReviewCriterionDto>>() {});
            } catch (Exception e) {
                continue;
            }
            for (ReviewCriterionDto c : criteria) {
                if (c.getCode() == null || c.getScore() == null) {
                    continue;
                }
                int[] acc = sumCount.computeIfAbsent(c.getCode(), k -> new int[2]);
                acc[0] += c.getScore();
                acc[1] += 1;
                questions.putIfAbsent(c.getCode(), c.getQuestion());
            }
        }
        return sumCount.entrySet().stream()
                .map(e -> TutorReputationResponse.CriterionAverage.builder()
                        .code(e.getKey())
                        .question(questions.get(e.getKey()))
                        .average(BigDecimal.valueOf((double) e.getValue()[0] / e.getValue()[1])
                                .setScale(1, RoundingMode.HALF_UP))
                        .count(e.getValue()[1])
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewableAssignmentResponse> getMyReviewableAssignments() {
        Long clientId = authHelper.requireRole(UserRole.CLIENT).getUserId();

        Map<Long, List<Review>> reviewsByAssignment =
                reviewRepository.findByReviewer_UserId(clientId).stream()
                        .filter(r -> r.getReviewType() == ReviewType.CLIENT_TO_TUTOR)
                        .collect(Collectors.groupingBy(r -> r.getAssignment().getAssignmentId()));

        return classAssignmentRepository
                .findByApplication_TutoringClass_Creator_UserId(clientId).stream()
                .filter(a -> a.getApplication() != null
                        && a.getApplication().getTutoringClass() != null)
                .map(a -> {
                    TutoringClass c = a.getApplication().getTutoringClass();
                    List<Review> reviews = reviewsByAssignment.getOrDefault(a.getAssignmentId(), List.of());
                    int submitted = reviews.size();

                    List<LocalDate> occurred = occurredLessonDates(c.getClassId());
                    boolean reviewable = submitted < occurred.size();

                    if (!reviewable && submitted == 0) {
                        return null;
                    }

                    boolean reviewOverdue = reviewable && isReviewOverdue(reviews, occurred);

                    Tutor tutor = a.getTutor();
                    Review latest = reviews.stream()
                            .max((x, y) -> x.getCreatedAt().compareTo(y.getCreatedAt()))
                            .orElse(null);
                    return ReviewableAssignmentResponse.builder()
                            .assignmentId(a.getAssignmentId())
                            .classId(c.getClassId())
                            .classTitle(c.getTitle())
                            .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                            .classStatus(c.getStatus().name())
                            .tutorUserId(tutor.getUser().getUserId())
                            .tutorName(tutor.getFullName())
                            .reviewed(submitted > 0)
                            .reviewsSubmitted(submitted)
                            .reviewable(reviewable)
                            .reviewOverdue(reviewOverdue)
                            .reviewId(latest != null ? latest.getReviewId() : null)
                            .rating(latest != null ? latest.getRating() : null)
                            .comment(latest != null ? latest.getComment() : null)
                            .criteriaJson(latest != null ? latest.getCriteriaJson() : null)
                            .anonymous(latest != null && latest.isAnonymous())
                            .reviewerDisplayName(latest != null ? resolveReviewerDisplayName(latest) : null)
                            .reviewedAt(latest != null ? latest.getCreatedAt() : null)
                            .tutorReply(latest != null ? latest.getTutorReply() : null)
                            .tutorReplyAt(latest != null ? latest.getTutorReplyAt() : null)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<LocalDate> occurredLessonDates(Long classId) {
        LocalDate today = LocalDate.now();
        return lessonRepository
                .findByTutoringClass_ClassIdOrderByLessonDateAscSequenceNoAsc(classId).stream()
                .filter(l -> !l.getLessonDate().isAfter(today))
                .filter(l -> l.getAttendanceStatus() != AttendanceStatus.ABSENT)
                .map(Lesson::getLessonDate)
                .sorted()
                .toList();
    }

    private boolean isReviewOverdue(List<Review> reviews, List<LocalDate> occurred) {
        LocalDate ref = reviews.isEmpty()
                ? (occurred.isEmpty() ? null : occurred.get(0))
                : reviews.stream()
                        .map(r -> r.getCreatedAt().toLocalDate())
                        .max((x, y) -> x.compareTo(y))
                        .orElse(null);
        return ref != null
                && !LocalDate.now().isBefore(ref.plusMonths(REVIEW_REQUIRED_WITHIN_MONTHS));
    }

    private BigDecimal resolveOverallRating(CreateReviewRequest request) {
        List<ReviewCriterionDto> criteria = request.getCriteria();
        if (criteria != null && !criteria.isEmpty()) {
            for (ReviewCriterionDto c : criteria) {
                if (c.getScore() == null || c.getScore() < 1 || c.getScore() > 5) {
                    throw new IllegalArgumentException("Mỗi tiêu chí phải được chấm từ 1 đến 5 sao");
                }
            }
            double average = criteria.stream().mapToInt(ReviewCriterionDto::getScore).average().orElse(0d);
            return BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        }
        if (request.getRating() == null) {
            throw new IllegalArgumentException("Vui lòng chấm điểm đánh giá");
        }
        return BigDecimal.valueOf(request.getRating()).setScale(1, RoundingMode.HALF_UP);
    }

    private String serializeCriteria(List<ReviewCriterionDto> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(criteria);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được dữ liệu tiêu chí đánh giá", e);
        }
    }

    @Override
    @Transactional
    public void recomputeReputationByTutorUser(Long tutorUserId) {
        tutorRepository
                .findByUser_UserId(tutorUserId)
                .ifPresent(tutor -> recomputeTutorReputation(tutor, tutorUserId));
    }

    private void recomputeTutorReputation(Tutor tutor, Long tutorUserId) {
        List<Review> visible = reviewRepository.findByReviewee_UserIdAndReviewTypeAndStatus(
                tutorUserId, ReviewType.CLIENT_TO_TUTOR, ReviewStatus.VISIBLE);
        double average = visible.isEmpty()
                ? 0d
                : visible.stream()
                        .mapToDouble(r -> r.getRating().doubleValue())
                        .average()
                        .orElse(0d);
        BigDecimal newScore = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
        BigDecimal oldScore = tutor.getRatingAvg() == null ? BigDecimal.ZERO : tutor.getRatingAvg();
        if (oldScore.compareTo(newScore) == 0) {
            return;
        }
        tutor.setRatingAvg(newScore);
        tutorRepository.save(tutor);

        ReputationHistory history = new ReputationHistory();
        history.setTutor(tutor);
        history.setOldScore(oldScore);
        history.setNewScore(newScore);
        history.setTriggerType("REVIEW");
        history.setReason("Cập nhật điểm trung bình sau đánh giá mới (" + visible.size() + " lượt)");
        reputationHistoryRepository.save(history);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReviewResponse toResponse(Review review) {
        TutoringClass reviewClass = review.getTutoringClass();
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .assignmentId(review.getAssignment().getAssignmentId())
                .reviewerId(review.getReviewer().getUserId())
                .revieweeId(review.getReviewee().getUserId())
                .reviewType(review.getReviewType())
                .rating(review.getRating())
                .comment(review.getComment())
                .tutorReply(review.getTutorReply())
                .tutorReplyAt(review.getTutorReplyAt())
                .criteriaJson(review.getCriteriaJson())
                .classTitle(reviewClass != null ? reviewClass.getTitle() : null)
                .subjectName(
                        reviewClass != null && reviewClass.getSubject() != null
                                ? reviewClass.getSubject().getSubjectName()
                                : null)
                .anonymous(review.isAnonymous())
                .reviewerDisplayName(resolveReviewerDisplayName(review))
                .createdAt(review.getCreatedAt())
                .build();
    }

    private String resolveReviewerDisplayName(Review review) {
        if (review.isAnonymous()) {
            String custom = trimToNull(review.getDisplayName());
            return custom != null ? custom : DEFAULT_ANONYMOUS_NAME;
        }
        return clientRepository
                .findByUser_UserId(review.getReviewer().getUserId())
                .map(Client::getFullName)
                .orElse(DEFAULT_ANONYMOUS_NAME);
    }
}
