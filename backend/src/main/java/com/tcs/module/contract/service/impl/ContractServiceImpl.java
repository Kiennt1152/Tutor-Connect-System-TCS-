package com.tcs.module.contract.service.impl;

import com.tcs.common.event.ContractSigned;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.dto.request.SignContractRequest;
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
import com.tcs.module.contract.enums.ContractTemplateStatus;
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.service.ContractService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.BusinessException;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.ReplyReviewRequest;
import com.tcs.module.contract.dto.request.ReviewCriterionDto;
import com.tcs.module.contract.dto.request.SaveRefundPayoutRequest;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.ReviewableAssignmentResponse;
import com.tcs.module.contract.dto.response.TutorReputationResponse;
import com.tcs.module.contract.entity.ReputationHistory;
import com.tcs.module.contract.entity.Review;
import com.tcs.module.contract.enums.ReviewStatus;
import com.tcs.module.contract.enums.ReviewType;
import com.tcs.module.contract.repository.ReputationHistoryRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.finance.util.RefundPayoutInfoCodec;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.event.ClientReviewedClassEvent;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.profile.enums.UserRole;
import java.time.LocalDate;
import java.util.stream.Collectors;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.notification.service.EmailService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.common.event.CooperationContractSigned;
import com.tcs.common.event.StudentContractSigned;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.contract.enums.ContractTemplateStatus;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int CONTRACT_EXPIRY_DAYS = 7;
    /** BF-03: thỏa thuận hợp tác chưa ký sẽ hết hiệu lực sau 48 giờ. */
    private static final int COOPERATION_EXPIRY_HOURS = 48;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ESCROW_BANK_NAME = "TPBank";
    private static final String ESCROW_BANK_BIN = "970423";
    private static final String ESCROW_ACCOUNT_NUMBER = "02660559201";
    private static final String ESCROW_ACCOUNT_NAME = "TUTOR CONNECT SYSTEM";
    private static final String PRIVATE_ESCROW_REF_PREFIX = "ESCROW-A";
    private static final String CENTER_ESCROW_REF_PREFIX = "ESCROW-CS";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, Integer> DAY_CODE_TO_ISO = Map.of(
            "T2", 1, "T3", 2, "T4", 3, "T5", 4, "T6", 5, "T7", 6, "CN", 7);
    private static final String DEFAULT_ANONYMOUS_NAME = "Người dùng ẩn danh";
    private static final int REVIEW_REQUIRED_WITHIN_MONTHS = 1;
    private static final DateTimeFormatter DOC_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuthHelper authHelper;
    private final ContractRepository contractRepository;
    private final ContractSignatureRepository contractSignatureRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final ClientRepository clientRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final EmailService emailService;
    private final EscrowService escrowService;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ReviewRepository reviewRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final SystemParameterRepository systemParameterRepository;
    private final ReputationHistoryRepository reputationHistoryRepository;
    private final LessonRepository lessonRepository;
    private final LessonAttendanceRepository lessonAttendanceRepository;
    private final com.tcs.module.profile.service.CccdService cccdService;

    // ─── VIEW CONTRACT (4.2) ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContract(Long contractId) {
        return getMyContract(contractId);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getMyContract(Long contractId) {
        Contract contract = findContract(contractId);
        validateViewPermission(contract);
        return toContractResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts() {
        var principal = authHelper.requireAuthenticated();
        Long userId = principal.getUserId();
        LinkedHashSet<Contract> contracts = new LinkedHashSet<>();
        contracts.addAll(contractRepository.findContractsByUserId(userId));
        contracts.addAll(contractRepository.findBySignatureParty(userId, principal.getEmail()));
        contracts.addAll(contractRepository.findByAssignment_Tutor_UserId(userId));
        contracts.addAll(contractRepository.findByAssignment_ClassCreator_UserId(userId));
        contracts.addAll(contractRepository.findByClassStudent_UserId(userId));
        // BF-03: thỏa thuận hợp tác center–gia sư (gia sư ký, trung tâm theo dõi).
        contracts.addAll(contractRepository.findByRecruitmentApplication_Tutor_UserId(userId));
        contracts.addAll(contractRepository.findByRecruitmentApplication_CenterUser_UserId(userId));
        return contracts.stream().map(this::toContractResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSignatureListResponse getSignatures(Long contractId) {
        Contract contract = findContract(contractId);
        validateViewPermission(contract);

        List<ContractSignature> signatures = contractSignatureRepository.findByContractId(contractId);
        int required = getRequiredSignatureCount(contract);
        int signed = (int) signatures.stream()
                .filter(s -> s.getSignatureStatus() == ContractSignatureStatus.SIGNED)
                .count();

        Long viewerId = authHelper.currentUserId();
        PartyRole viewerRole = partyRoleOf(contract, viewerId);

        return ContractSignatureListResponse.builder()
                .contractId(contractId)
                .contractNo(contract.getContractNo())
                .hasAllSignatures(signed >= required)
                .fullySigned(signed >= required)
                .signedCount(signed)
                .requiredSignatures(required)
                .totalRequired(required)
                .signatures(signatures.stream()
                        .map(s -> toSignatureResponse(s, viewerId, viewerRole))
                        .toList())
                .build();
    }

    /** Vai trò của {@code userId} trong hợp đồng (không ném lỗi nếu không phải bên nào -> null). */
    private PartyRole partyRoleOf(Contract contract, Long userId) {
        if (userId == null) {
            return null;
        }
        if (contract.getRecruitmentApplication() != null) {
            RecruitmentApplication app = contract.getRecruitmentApplication();
            if (app.getTutor().getUser().getUserId().equals(userId)) {
                return PartyRole.TUTOR;
            }
            if (app.getRecruitmentPost().getCenter().getUser().getUserId().equals(userId)) {
                return PartyRole.CENTER;
            }
        }
        if (contract.getClassStudent() != null) {
            ClassStudent cs = contract.getClassStudent();
            if (cs.getEnrolledByUser() != null && cs.getEnrolledByUser().getUserId().equals(userId)) {
                return PartyRole.CLIENT;
            }
            TutoringClass cls = cs.getTutoringClass();
            if (cls != null && cls.getCreator() != null && cls.getCreator().getUserId().equals(userId)) {
                return tutorCenterRepository.findByUser_UserId(userId).isPresent()
                        ? PartyRole.CENTER : PartyRole.CLIENT;
            }
        }
        if (contract.getAssignment() != null) {
            ClassAssignment a = contract.getAssignment();
            if (a.getTutor() != null && a.getTutor().getUser() != null
                    && a.getTutor().getUser().getUserId().equals(userId)) {
                return PartyRole.TUTOR;
            }
            TutorApplication app = a.getApplication();
            if (app != null && app.getTutoringClass() != null && app.getTutoringClass().getCreator() != null
                    && app.getTutoringClass().getCreator().getUserId().equals(userId)) {
                return tutorCenterRepository.findByUser_UserId(userId).isPresent()
                        ? PartyRole.CENTER : PartyRole.CLIENT;
            }
        }
        return currentUserSignature(contract, userId)
                .map(ContractSignature::getPartyRole)
                .orElse(null);
    }

    @Override
    @Transactional
    public Map<String, Object> sendOtp(Long contractId) {
        assertSignerNotMinor();
        assertSignerCccdComplete();
        Contract contract = findContract(contractId);
        PartyRole role = resolvePartyRole(contract);

        if (contract.getStatus() != ContractStatus.PENDING && contract.getStatus() != ContractStatus.DRAFT) {
            throw new IllegalStateException("Hợp đồng không ở trạng thái chờ ký");
        }

        ContractSignature signature = contractSignatureRepository
                .findByContractIdAndPartyRole(contractId, role)
                .orElseGet(() -> createSignatureSlot(contract, role, authHelper.requireAuthenticated().getEmail()));

        if (signature.getSignatureStatus() == ContractSignatureStatus.SIGNED) {
            throw new IllegalStateException("Bạn đã ký hợp đồng này rồi");
        }

        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        String recipientEmail = authHelper.requireAuthenticated().getEmail();

        signature.setOtpCode(otp);
        signature.setOtpExpiresAt(expiresAt);
        signature.setOtpAttempts(0);
        signature.setEmail(recipientEmail);
        contractSignatureRepository.save(signature);

        // Gửi email OTP; nếu email lỗi (chưa cấu hình SMTP / App Password...) thì KHÔNG chặn quy trình
        // — OTP vẫn được lưu để ký. OTP luôn được ghi ra log để có thể lấy khi email không tới.
        try {
            emailService.sendContractOtp(recipientEmail, otp, contract.getContractNo());
        } catch (RuntimeException ex) {
            log.warn("Gui email OTP hop dong that bai (van tiep tuc, lay OTP tu log): {}", ex.getMessage());
        }
        log.info("[OTP HOP DONG] {} -> {} : {}", contract.getContractNo(), recipientEmail, otp);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Mã OTP đã được gửi đến email của bạn");
        body.put("maskedEmail", maskEmail(recipientEmail));
        body.put("expiresInMinutes", OTP_EXPIRY_MINUTES);
        body.put("maxAttempts", OTP_MAX_ATTEMPTS);
        return body;
    }

    @Override
    @Transactional
    public OtpSentResponse sendSignOtp(Long contractId) {
        Map<String, Object> result = sendOtp(contractId);
        return OtpSentResponse.builder()
                .maskedEmail(String.valueOf(result.get("maskedEmail")))
                .message(String.valueOf(result.get("message")))
                .build();
    }

    @Override
    @Transactional
    public ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request) {
        if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new IllegalArgumentException("Mã OTP là bắt buộc");
        }
        assertSignerNotMinor();
        assertSignerCccdComplete();

        Contract contract = findContract(contractId);
        PartyRole role = resolvePartyRole(contract);

        if (contract.getStatus() != ContractStatus.PENDING && contract.getStatus() != ContractStatus.DRAFT) {
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

        if (signature.getOtpExpiresAt() == null
                || signature.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            signature.setSignatureStatus(ContractSignatureStatus.EXPIRED);
            contractSignatureRepository.save(signature);
            throw new IllegalStateException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (!signature.getOtpCode().equals(request.getOtpCode().trim())) {
            signature.setOtpAttempts(signature.getOtpAttempts() + 1);
            contractSignatureRepository.save(signature);
            int remaining = Math.max(0, OTP_MAX_ATTEMPTS - signature.getOtpAttempts());
            throw new IllegalArgumentException("Mã OTP không đúng. Còn " + remaining + " lần thử.");
        }

        User signer = userRepository.findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        signature.setSigner(signer);
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now());
        signature.setSignatureData("OTP_VERIFIED:" + signer.getEmail() + ":"
                + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        signature.setOtpCode(null);
        signature.setOtpExpiresAt(null);
        contractSignatureRepository.save(signature);

        if (isFullySigned(contractId)) {
            contract.setStatus(ContractStatus.SIGNED);
            contract.setSignedAt(LocalDateTime.now());
            contract.setConfirmedAt(LocalDateTime.now());
            contractRepository.save(contract);
            // main: khóa escrow + phát ContractSigned (đánh giá/uy tín/tất toán).
            publishContractSigned(contract);
            // BF-03 bước 9-10: gia sư ký xong -> báo cho module center kích hoạt thành viên + đóng tin.
            if (contract.getRecruitmentApplication() != null) {
                eventPublisher.publishEvent(new CooperationContractSigned(
                        contract.getRecruitmentApplication().getRecruitmentAppId(),
                        contract.getContractId()));
            }
            // BF-04: học viên ký xong -> marketplace mở bước thanh toán escrow.
            if (contract.getClassStudent() != null) {
                eventPublisher.publishEvent(new StudentContractSigned(
                        contract.getClassStudent().getClassStudentId(),
                        contract.getContractId()));
            }
        }

        return toContractResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse signContract(Long contractId, SignContractRequest request) {
        SignWithOtpRequest otpRequest = new SignWithOtpRequest();
        otpRequest.setOtpCode(request.getOtpCode());
        return signWithOtp(contractId, otpRequest);
    }

    @Override
    @Transactional
    public ContractResponse saveRefundPayoutInfo(Long contractId, SaveRefundPayoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin tài khoản nhận hoàn tiền");
        }
        Contract contract = findContract(contractId);
        validateViewPermission(contract);
        if (resolvePartyRole(contract) != PartyRole.CLIENT) {
            throw new ForbiddenException("Chỉ phụ huynh/học viên thanh toán mới được cập nhật tài khoản nhận hoàn tiền");
        }
        if (!isFullySigned(contractId)) {
            throw new IllegalArgumentException("Vui lòng ký đủ hợp đồng trước khi nhập tài khoản nhận hoàn tiền");
        }

        RefundPayoutInfo payoutInfo = new RefundPayoutInfo(
                RefundPayoutInfoCodec.normalize(request.getBankName()),
                RefundPayoutInfoCodec.normalizeAccountNo(request.getAccountNo()),
                RefundPayoutInfoCodec.normalize(request.getAccountHolderName()));
        if (!RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ ngân hàng, số tài khoản và tên chủ tài khoản");
        }

        if (contract.getClassStudent() != null
                && contract.getClassStudent().getTutoringClass() != null
                && contract.getClassStudent().getTutoringClass().getClassType() == ClassType.CENTER) {
            ClassStudent classStudent = contract.getClassStudent();
            classStudent.setNotes(RefundPayoutInfoCodec.appendToReason(classStudent.getNotes(), payoutInfo));
            classStudentRepository.save(classStudent);
        } else if (contract.getAssignment() != null && contract.getSourceType() == ContractSourceType.PRIVATE) {
            ClassAssignment assignment = contract.getAssignment();
            assignment.setTermsB(RefundPayoutInfoCodec.appendToReason(assignment.getTermsB(), payoutInfo));
            classAssignmentRepository.save(assignment);
        } else {
            throw new IllegalArgumentException(
                    "Thông tin nhận hoàn tiền chỉ áp dụng cho hợp đồng lớp private hoặc ghi danh lớp trung tâm");
        }
        return toContractResponse(contract);
    }

    @Override
    @Transactional
    public void sign(Long contractId, String otp, Long signerUserId) {
        if (!authHelper.currentUserId().equals(signerUserId)) {
            throw new ForbiddenException("Không thể ký thay người dùng khác");
        }
        SignWithOtpRequest request = new SignWithOtpRequest();
        request.setOtpCode(otp);
        signWithOtp(contractId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFullySigned(Long contractId) {
        Contract contract = findContract(contractId);
        return contractSignatureRepository.countSignedByContractId(contractId) >= getRequiredSignatureCount(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public SignatureStatusResponse getSignatureStatus(Long contractId) {
        Contract contract = findContract(contractId);
        validateViewPermission(contract);
        Long currentUserId = authHelper.currentUserId();

        List<SignatureStatusResponse.SignatureInfo> signedSignatures =
                contractSignatureRepository.findByContractId(contractId).stream()
                        .filter(s -> s.getSignatureStatus() == ContractSignatureStatus.SIGNED)
                        .map(s -> SignatureStatusResponse.SignatureInfo.builder()
                                .signatureId(s.getSignatureId())
                                .signerUserId(s.getSigner() != null ? s.getSigner().getUserId() : null)
                                .signerName(resolveSignatureName(s))
                                .signerRole(partyLabel(s.getPartyRole()))
                                .signedAt(s.getSignedAt())
                                .isCurrentUser(s.getSigner() != null
                                        && s.getSigner().getUserId().equals(currentUserId))
                                .build())
                        .toList();

        int required = getRequiredSignatureCount(contract);
        int signed = contractSignatureRepository.countSignedByContractId(contractId);
        return SignatureStatusResponse.builder()
                .contractId(contractId)
                .contractNo(contract.getContractNo())
                .fullySigned(signed >= required)
                .signedCount(signed)
                .totalRequired(required)
                .signatures(signedSignatures)
                .build();
    }

    @Override
    @Transactional
    public ContractResponse generateContract(Long assignmentId) {
        return getMyContract(generateForAssignment(assignmentId).getContractId());
    }

    @Override
    @Transactional
    public Contract generateForAssignment(Long assignmentId) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        if (contractRepository.findByAssignment_AssignmentId(assignmentId).isPresent()) {
            throw new IllegalStateException("Hợp đồng đã tồn tại cho phân công này");
        }

        validateGenerateAssignmentPermission(assignment);

        ContractTemplate template = findActiveTemplate();
        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
        contract.setAssignment(assignment);
        contract.setStatus(ContractStatus.PENDING);
        contract.setSourceType(ContractSourceType.PRIVATE);
        contract.setTemplate(template);
        contract.setExpiresAt(LocalDateTime.now().plusDays(CONTRACT_EXPIRY_DAYS));
        contract.setTermsSummary(template != null ? template.getContent() : buildTermsSummary(assignment));
        contract = contractRepository.save(contract);

        initializeSignatureSlots(contract);
        return contract;
    }

    // ─── GENERATE COOPERATION CONTRACT (BF-03 bước 7) ───────────────────────
    @Override
    @Transactional
    public ContractResponse generateCooperationContract(
            Long recruitmentApplicationId, Long templateId, String editedTerms) {
        RecruitmentApplication app = recruitmentApplicationRepository.findById(recruitmentApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển"));

        if (contractRepository
                .findByRecruitmentApplication_RecruitmentAppId(recruitmentApplicationId)
                .isPresent()) {
            throw new IllegalStateException("Thỏa thuận hợp tác đã tồn tại cho đơn này");
        }

        ContractTemplate template = templateId != null
                ? contractTemplateRepository.findById(templateId).orElse(null)
                : null;
        // Nội dung điều khoản thô: ưu tiên nội dung center tự nhập khi duyệt -> mẫu -> mặc định.
        String rawTerms = firstNonBlank(editedTerms,
                template != null ? template.getContent() : null,
                "Điều 1. Gia sư đồng ý gia nhập đội ngũ gia sư của trung tâm và tuân thủ quy định của trung tâm.\n"
                        + "Điều 2. Hai bên hợp tác trên tinh thần thiện chí, trung thực.");

        Tutor tutor = app.getTutor();
        TutorCenter center = app.getRecruitmentPost().getCenter();

        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("tenGiaSu", tutor.getFullName());
        vars.put("tenTrungTam", center.getCompanyName());
        vars.put("ngayKy", LocalDate.now().format(DOC_DATE));

        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
        contract.setRecruitmentApplication(app);
        contract.setStatus(ContractStatus.PENDING);
        contract.setSourceType(ContractSourceType.CENTER);
        // BF-03: thỏa thuận hợp tác chỉ có hiệu lực 48 giờ để gia sư ký.
        contract.setExpiresAt(LocalDateTime.now().plusHours(COOPERATION_EXPIRY_HOURS));
        contract.setTemplate(template);
        // Lưu ĐIỀU KHOẢN đã render placeholder (đóng băng); BÊN A/BÊN B dựng động khi hiển thị.
        contract.setTermsSummary(renderPlaceholders(rawTerms, vars).trim());
        contract = contractRepository.save(contract);

        // BF-03 bước 8: trung tâm KÝ SẴN, gia sư ký bằng OTP.
        createSignedCenterSignature(contract, center.getUser());
        createPendingSignature(contract, PartyRole.TUTOR, tutor.getUser().getEmail());

        return toContractResponse(contract);
    }

    /**
     * BF-03 (exception): gia sư TỪ CHỐI thỏa thuận hợp tác chưa ký. Chấm dứt hợp đồng và đóng đơn
     * (WITHDRAWN) để trung tâm chọn ứng viên khác. Chỉ gia sư của đơn mới được từ chối.
     */
    @Override
    @Transactional
    public void declineCooperationContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
        RecruitmentApplication app = contract.getRecruitmentApplication();
        if (app == null) {
            throw new IllegalArgumentException("Đây không phải thỏa thuận hợp tác gia sư.");
        }
        if (contract.getStatus() != ContractStatus.PENDING) {
            throw new IllegalArgumentException("Thỏa thuận này không còn ở trạng thái chờ ký.");
        }
        Long currentUserId = authHelper.currentUserId();
        if (app.getTutor() == null || app.getTutor().getUser() == null
                || !app.getTutor().getUser().getUserId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền từ chối thỏa thuận này");
        }
        contract.setStatus(ContractStatus.TERMINATED);
        contractRepository.save(contract);
        app.setStatus(com.tcs.module.center.enums.RecruitmentApplicationStatus.WITHDRAWN);
        recruitmentApplicationRepository.save(app);
    }

    // ─── GENERATE STUDENT CONTRACT (BF-04 bước 7) ───────────────────────────
    @Override
    @Transactional
    public ContractResponse generateStudentContract(Long classStudentId) {
        ClassStudent cs = classStudentRepository.findById(classStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên trong lớp"));

        Optional<Contract> existing =
                contractRepository.findByClassStudent_ClassStudentId(classStudentId);
        if (existing.isPresent()) {
            return toContractResponse(existing.get());
        }

        TutoringClass cls = cs.getTutoringClass();
        Long classId = cls.getClassId();
        ContractTemplate template = resolveClassTemplate(classId);
        // Nội dung điều khoản thô: ưu tiên nội dung center nhập khi tạo lớp (classterms:{id}) -> mẫu -> mặc định.
        String rawTerms = firstNonBlank(
                findClassTerms(classId),
                template != null ? template.getContent() : null,
                "Điều 1. Học viên ghi danh và cam kết tham gia đầy đủ các buổi học của lớp.\n"
                        + "Điều 2. Trung tâm bảo đảm tổ chức lớp học theo đúng lịch và nội dung đã công bố.");

        String centerName = tutorCenterRepository
                .findByUser_UserId(cls.getCreator().getUserId())
                .map(TutorCenter::getCompanyName)
                .orElse(null);
        String studentName = cs.getStudentName();

        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("tenHocVien", studentName);
        vars.put("tenTrungTam", centerName);
        vars.put("tenLop", cls.getTitle());
        vars.put("monHoc", cls.getSubject() != null ? cls.getSubject().getSubjectName() : "");
        vars.put("hocPhi", formatMoney(cls.getTuitionFee()));
        vars.put("soBuoi", cls.getNumberOfSessions() != null ? String.valueOf(cls.getNumberOfSessions()) : "");
        vars.put("ngayBatDau", cls.getStartDate() != null ? cls.getStartDate().format(DOC_DATE) : "");
        vars.put("ngayKetThuc", cls.getEndDate() != null ? cls.getEndDate().format(DOC_DATE) : "");
        vars.put("ngayKy", LocalDate.now().format(DOC_DATE));

        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
        contract.setClassStudent(cs);
        contract.setStatus(ContractStatus.PENDING);
        contract.setSourceType(ContractSourceType.CENTER);
        contract.setExpiresAt(LocalDateTime.now().plusDays(CONTRACT_EXPIRY_DAYS));
        contract.setTemplate(template);
        // Lưu ĐIỀU KHOẢN đã render placeholder (đóng băng); BÊN A/BÊN B dựng động khi hiển thị.
        contract.setTermsSummary(renderPlaceholders(rawTerms, vars).trim());
        contract = contractRepository.save(contract);

        // BF-04 bước 7: trung tâm KÝ SẴN, người ghi danh (phụ huynh/học viên) ký bằng OTP.
        if (cls.getCreator() != null) {
            createSignedCenterSignature(contract, cls.getCreator());
        }
        createPendingSignature(contract, PartyRole.CLIENT,
                cs.getEnrolledByUser() != null ? cs.getEnrolledByUser().getEmail() : null);

        return toContractResponse(contract);
    }

    private ContractTemplate resolveClassTemplate(Long classId) {
        Long templateId = systemParameterRepository.findByParamKey("classtpl:" + classId)
                .map(p -> {
                    try {
                        return Long.valueOf(p.getParamValue().trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(null);
        if (templateId != null) {
            return contractTemplateRepository.findById(templateId).orElse(null);
        }
        // Fallback: mẫu ACTIVE đầu tiên KHÔNG phải loại tuyển dụng (tránh nhét nội dung tuyển dụng
        // gia sư vào hợp đồng học viên).
        return contractTemplateRepository.findByStatus(ContractTemplateStatus.ACTIVE)
                .stream()
                .filter(t -> !isRecruitmentTemplate(t.getTemplateId()))
                .findFirst().orElse(null);
    }

    /** Loại mẫu hợp đồng lưu ở system_parameters (tpltype:{id} -> RECRUITMENT|CLASS). */
    private boolean isRecruitmentTemplate(Long templateId) {
        return systemParameterRepository.findByParamKey("tpltype:" + templateId)
                .map(p -> "RECRUITMENT".equalsIgnoreCase(p.getParamValue()))
                .orElse(false);
    }

    /** Nội dung điều khoản center nhập khi tạo lớp (classterms:{classId}), nếu có. */
    private String findClassTerms(Long classId) {
        return systemParameterRepository.findByParamKey("classterms:" + classId)
                .map(SystemParameter::getParamValue)
                .orElse(null);
    }

    // ─── Dựng văn bản hợp đồng: khung chuẩn + tự điền dữ liệu (placeholder) + đóng băng ─────

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    /** Thay {{key}} bằng giá trị; token không có trong map được giữ nguyên để center biết mà bổ sung. */
    private String renderPlaceholders(String raw, Map<String, String> vars) {
        if (raw == null) {
            return "";
        }
        String out = raw;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    /**
     * Dựng văn bản hợp đồng hoàn chỉnh để hiển thị: Quốc hiệu + tiêu ngữ + tiêu đề + khối BÊN A
     * (thông tin trung tâm) + khối BÊN B (thông tin CCCD người ký) + điều khoản đã đóng băng.
     * BÊN A/BÊN B lấy động từ dữ liệu mới nhất để luôn khớp thông tin đã xác minh.
     */
    private String assembleDocument(Contract c) {
        String clauses = c.getTermsSummary() == null ? "" : c.getTermsSummary().trim();
        String title;
        String partyBLabel;
        TutorCenter center = null;
        Long signerUserId = null;
        String hocVien = null;

        if (c.getRecruitmentApplication() != null) {
            title = "HỢP ĐỒNG HỢP TÁC GIA SƯ";
            partyBLabel = "GIA SƯ (BÊN THỰC HIỆN)";
            RecruitmentApplication app = c.getRecruitmentApplication();
            center = app.getRecruitmentPost().getCenter();
            signerUserId = app.getTutor().getUser().getUserId();
        } else if (c.getClassStudent() != null) {
            title = "HỢP ĐỒNG GHI DANH HỌC VIÊN";
            partyBLabel = "NGƯỜI ĐẠI DIỆN KÝ (BÊN THỰC HIỆN)";
            ClassStudent cs = c.getClassStudent();
            hocVien = cs.getStudentName();
            if (cs.getTutoringClass() != null && cs.getTutoringClass().getCreator() != null) {
                center = tutorCenterRepository
                        .findByUser_UserId(cs.getTutoringClass().getCreator().getUserId())
                        .orElse(null);
            }
            signerUserId = cs.getEnrolledByUser() != null ? cs.getEnrolledByUser().getUserId() : null;
        } else {
            title = "HỢP ĐỒNG GIA SƯ";
            partyBLabel = "GIA SƯ (BÊN THỰC HIỆN)";
            ClassAssignment a = c.getAssignment();
            signerUserId = a != null && a.getTutor() != null && a.getTutor().getUser() != null
                    ? a.getTutor().getUser().getUserId() : null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\n");
        sb.append("Độc lập - Tự do - Hạnh phúc\n");
        sb.append("---------------o0o---------------\n\n");
        sb.append(title).append("\n");
        sb.append("Số: ").append(nz(c.getContractNo())).append("\n\n");
        if (center != null) {
            sb.append(centerBlock(center)).append("\n");
        }
        sb.append(partyBBlock(partyBLabel, signerUserId, hocVien)).append("\n");
        sb.append("Sau khi bàn bạc, hai bên thống nhất các điều khoản sau:\n\n");
        sb.append("ĐIỀU KHOẢN & NGHĨA VỤ:\n");
        sb.append(clauses.isBlank() ? "(Chưa có nội dung điều khoản)" : clauses);
        return sb.toString();
    }

    /** Khối BÊN A: thông tin trung tâm (hồ sơ + thông tin ký hợp đồng bổ sung). */
    private String centerBlock(TutorCenter center) {
        Map<String, String> extra = readCenterContractExtra(center.getCenterId());
        String email = center.getUser() != null ? center.getUser().getEmail() : null;
        StringBuilder sb = new StringBuilder();
        sb.append("BÊN A: ").append(nz(center.getCompanyName())).append("\n");
        sb.append("Trụ sở: ").append(nz(center.getAddress())).append("\n");
        sb.append("Điện thoại: ").append(nz(center.getPhone()));
        if (email != null && !email.isBlank()) {
            sb.append("   Mail: ").append(email);
        }
        String website = extra.get("website");
        if (website != null && !website.isBlank()) {
            sb.append("   Website: ").append(website);
        }
        sb.append("\n");
        // Người đại diện lấy từ CCCD người đại diện pháp luật (đã quét), không nhập tay.
        String rep = center.getUser() != null
                ? cccdService.getByUserId(center.getUser().getUserId()).getFullName()
                : null;
        String pos = extra.get("representativePosition");
        if ((rep != null && !rep.isBlank()) || (pos != null && !pos.isBlank())) {
            sb.append("Đại diện: ").append(nz(rep));
            if (pos != null && !pos.isBlank()) {
                sb.append("   Chức vụ: ").append(pos);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** Khối BÊN B: thông tin CCCD của người ký (lấy từ hồ sơ đã xác minh). */
    private String partyBBlock(String label, Long signerUserId, String hocVien) {
        StringBuilder sb = new StringBuilder();
        sb.append("BÊN B: (").append(label).append(")\n");
        if (hocVien != null && !hocVien.isBlank()) {
            sb.append("Học viên: ").append(hocVien).append("\n");
        }
        com.tcs.module.profile.dto.CccdInfoDto cccd =
                signerUserId != null ? cccdService.getByUserId(signerUserId) : null;
        if (cccd != null) {
            sb.append("Họ tên: ").append(nz(cccd.getFullName()))
                    .append("   Sinh ngày: ").append(nz(cccd.getDateOfBirth())).append("\n");
            sb.append("CCCD số: ").append(nz(cccd.getCccdNumber()))
                    .append("   Cấp ngày: ").append(nz(cccd.getIssueDate()))
                    .append("   Tại: ").append(nz(cccd.getIssuePlace())).append("\n");
            sb.append("Thường trú: ").append(nz(cccd.getPermanentAddress())).append("\n");
        }
        return sb.toString();
    }

    /** Thông tin ký hợp đồng bổ sung của trung tâm (centercontract:{centerId}). */
    private Map<String, String> readCenterContractExtra(Long centerId) {
        return systemParameterRepository.findByParamKey("centercontract:" + centerId)
                .map(p -> {
                    try {
                        return OBJECT_MAPPER.readValue(p.getParamValue(),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    } catch (Exception e) {
                        return new java.util.HashMap<String, String>();
                    }
                })
                .orElseGet(java.util.HashMap::new);
    }

    private String formatMoney(java.math.BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return java.text.NumberFormat.getInstance(java.util.Locale.of("vi", "VN")).format(amount) + " đ";
    }

    /** Ô ký của bên còn lại (gia sư / phụ huynh) — trạng thái CHỜ KÝ, ký bằng OTP. */
    private void createPendingSignature(Contract contract, PartyRole role, String email) {
        ContractSignature sig = new ContractSignature();
        sig.setContract(contract);
        sig.setPartyRole(role);
        sig.setEmail(email);
        sig.setSignatureStatus(ContractSignatureStatus.PENDING);
        sig.setOtpAttempts(0);
        contractSignatureRepository.save(sig);
    }

    /** Trung tâm KÝ SẴN khi tạo hợp đồng (đại diện trung tâm là người tạo/duyệt). */
    private void createSignedCenterSignature(Contract contract, User centerUser) {
        ContractSignature sig = new ContractSignature();
        sig.setContract(contract);
        sig.setPartyRole(PartyRole.CENTER);
        sig.setSigner(centerUser);
        sig.setEmail(centerUser != null ? centerUser.getEmail() : null);
        sig.setSignatureStatus(ContractSignatureStatus.SIGNED);
        sig.setSignedAt(LocalDateTime.now());
        sig.setSignatureData("CENTER_PRESIGNED:"
                + (centerUser != null ? centerUser.getEmail() : "")
                + ":" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        sig.setOtpAttempts(0);
        contractSignatureRepository.save(sig);
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────
    @Override
    @Transactional
    public Contract generateForEnrollment(Long classStudentId) {
        ClassStudent classStudent = classStudentRepository.findById(classStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh"));

        if (contractRepository.findByClassStudent_ClassStudentId(classStudentId).isPresent()) {
            throw new IllegalStateException("Hợp đồng đã tồn tại cho ghi danh này");
        }

        validateGenerateEnrollmentPermission(classStudent);

        ContractTemplate template = findActiveTemplate();
        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
        contract.setClassStudent(classStudent);
        contract.setStatus(ContractStatus.PENDING);
        contract.setSourceType(ContractSourceType.CENTER);
        contract.setTemplate(template);
        contract.setExpiresAt(LocalDateTime.now().plusDays(CONTRACT_EXPIRY_DAYS));
        contract.setTermsSummary(template != null ? template.getContent() : buildCenterTermsSummary(classStudent));
        contract = contractRepository.save(contract);

        initializeSignatureSlots(contract);
        return contract;
    }

    private Contract findContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
    }

    /** #2: Tài khoản dưới 18 tuổi không được tự ký — phụ huynh (tài khoản đã liên kết) ký thay. */
    private void assertSignerNotMinor() {
        clientRepository.findByUser_UserId(authHelper.currentUserId()).ifPresent(c -> {
            if (com.tcs.module.profile.util.AgeUtils.isMinor(c.getDateOfBirth())) {
                throw new IllegalStateException(
                        "Tài khoản dưới 18 tuổi không được ký hợp đồng. "
                                + "Phụ huynh (tài khoản đã liên kết) sẽ ký thay.");
            }
        });
    }

    /** Người ký phải hoàn thành thông tin CCCD (BÊN B) trong Hồ sơ trước khi ký. */
    private void assertSignerCccdComplete() {
        if (!cccdService.isComplete(authHelper.currentUserId())) {
            throw new IllegalStateException(
                    "Vui lòng hoàn thành thông tin CCCD trong Hồ sơ (đọc QR CCCD) trước khi ký hợp đồng.");
        }
    }

    private void validateViewPermission(Contract contract) {
        if (!isPartyOfContract(contract, authHelper.currentUserId())) {
            throw new ForbiddenException("Bạn không có quyền xem hợp đồng này");
        }
    }

    private void validateGenerateAssignmentPermission(ClassAssignment assignment) {
        Long currentUserId = authHelper.currentUserId();
        boolean isTutor = assignment.getTutor() != null
                && assignment.getTutor().getUser() != null
                && assignment.getTutor().getUser().getUserId().equals(currentUserId);
        boolean isClassCreator = assignment.getApplication() != null
                && assignment.getApplication().getTutoringClass() != null
                && assignment.getApplication().getTutoringClass().getCreator() != null
                && assignment.getApplication().getTutoringClass().getCreator().getUserId().equals(currentUserId);
        if (!isTutor && !isClassCreator) {
            throw new ForbiddenException("Bạn không có quyền tạo hợp đồng cho phân công này");
        }
    }

    private void validateGenerateEnrollmentPermission(ClassStudent classStudent) {
        Long currentUserId = authHelper.currentUserId();
        TutoringClass tutoringClass = classStudent.getTutoringClass();
        boolean isCenter = tutoringClass != null
                && tutoringClass.getCreator() != null
                && tutoringClass.getCreator().getUserId().equals(currentUserId);
        boolean isEnroller = classStudent.getEnrolledByUser() != null
                && classStudent.getEnrolledByUser().getUserId().equals(currentUserId);
        if (!isCenter && !isEnroller) {
            throw new ForbiddenException("Bạn không có quyền tạo hợp đồng cho ghi danh này");
        }
    }

    private boolean isPartyOfContract(Contract contract, Long userId) {
        // BF-03: thỏa thuận hợp tác center–gia sư (không gắn lớp/assignment).
        if (contract.getRecruitmentApplication() != null) {
            RecruitmentApplication app = contract.getRecruitmentApplication();
            if (app.getTutor().getUser().getUserId().equals(userId)
                    || app.getRecruitmentPost().getCenter().getUser().getUserId().equals(userId)) {
                return true;
            }
        }
        if (contract.getAssignment() != null) {
            ClassAssignment assignment = contract.getAssignment();
            if (assignment.getTutor() != null
                    && assignment.getTutor().getUser() != null
                    && assignment.getTutor().getUser().getUserId().equals(userId)) {
                return true;
            }
            TutorApplication application = assignment.getApplication();
            if (application != null
                    && application.getTutoringClass() != null
                    && application.getTutoringClass().getCreator() != null
                    && application.getTutoringClass().getCreator().getUserId().equals(userId)) {
                return true;
            }
        }

        if (contract.getClassStudent() != null) {
            ClassStudent classStudent = contract.getClassStudent();
            TutoringClass tutoringClass = classStudent.getTutoringClass();
            boolean isCreator = tutoringClass != null
                    && tutoringClass.getCreator() != null
                    && tutoringClass.getCreator().getUserId().equals(userId);
            boolean isEnroller = classStudent.getEnrolledByUser() != null
                    && classStudent.getEnrolledByUser().getUserId().equals(userId);
            if (isCreator || isEnroller) {
                return true;
            }
        }

        return currentUserSignature(contract, userId).isPresent();
    }

    private PartyRole resolvePartyRole(Contract contract) {
        Long currentUserId = authHelper.currentUserId();
        // BF-03: thỏa thuận hợp tác center–gia sư (không gắn lớp/assignment).
        if (contract.getRecruitmentApplication() != null) {
            RecruitmentApplication app = contract.getRecruitmentApplication();
            if (app.getTutor().getUser().getUserId().equals(currentUserId)) {
                return PartyRole.TUTOR;
            }
            if (app.getRecruitmentPost().getCenter().getUser().getUserId().equals(currentUserId)) {
                return PartyRole.CENTER;
            }
        }

        if (contract.getAssignment() != null) {
            ClassAssignment assignment = contract.getAssignment();
            if (assignment.getTutor() != null
                    && assignment.getTutor().getUser() != null
                    && assignment.getTutor().getUser().getUserId().equals(currentUserId)) {
                return PartyRole.TUTOR;
            }
            TutorApplication application = assignment.getApplication();
            if (application != null
                    && application.getTutoringClass() != null
                    && application.getTutoringClass().getCreator() != null
                    && application.getTutoringClass().getCreator().getUserId().equals(currentUserId)) {
                return tutorCenterRepository.findByUser_UserId(currentUserId).isPresent()
                        ? PartyRole.CENTER
                        : PartyRole.CLIENT;
            }
        }

        if (contract.getClassStudent() != null) {
            ClassStudent classStudent = contract.getClassStudent();
            TutoringClass tutoringClass = classStudent.getTutoringClass();
            if (classStudent.getEnrolledByUser() != null
                    && classStudent.getEnrolledByUser().getUserId().equals(currentUserId)) {
                return PartyRole.CLIENT;
            }
            if (tutoringClass != null
                    && tutoringClass.getCreator() != null
                    && tutoringClass.getCreator().getUserId().equals(currentUserId)) {
                return tutorCenterRepository.findByUser_UserId(currentUserId).isPresent()
                        ? PartyRole.CENTER
                        : PartyRole.CLIENT;
            }
        }

        return currentUserSignature(contract, currentUserId)
                .map(ContractSignature::getPartyRole)
                .orElseThrow(() -> new ForbiddenException("Bạn không phải là bên liên quan đến hợp đồng này"));
    }

    private Optional<ContractSignature> currentUserSignature(Contract contract, Long userId) {
        if (contract == null || contract.getContractId() == null || userId == null) {
            return Optional.empty();
        }
        String email = authHelper.requireAuthenticated().getEmail();
        return contractSignatureRepository.findByContractId(contract.getContractId()).stream()
                .filter(signature -> {
                    boolean signerMatches = signature.getSigner() != null
                            && signature.getSigner().getUserId() != null
                            && signature.getSigner().getUserId().equals(userId);
                    boolean emailMatches = StringUtils.hasText(email)
                            && StringUtils.hasText(signature.getEmail())
                            && signature.getEmail().trim().equalsIgnoreCase(email.trim());
                    return signerMatches || emailMatches;
                })
                .findFirst();
    }

    private ContractSignature createSignatureSlot(Contract contract, PartyRole role, String email) {
        ContractSignature signature = new ContractSignature();
        signature.setContract(contract);
        signature.setPartyRole(role);
        signature.setEmail(email);
        signature.setSignatureStatus(ContractSignatureStatus.PENDING);
        signature.setOtpAttempts(0);
        return signature;
    }

    private void initializeSignatureSlots(Contract contract) {
        if (contract.getAssignment() != null) {
            ClassAssignment assignment = contract.getAssignment();
            Tutor tutor = assignment.getTutor();
            if (tutor != null && tutor.getUser() != null) {
                contractSignatureRepository.save(createSignatureSlot(
                        contract, PartyRole.TUTOR, tutor.getUser().getEmail()));
            }

            TutoringClass tutoringClass = assignment.getApplication() != null
                    ? assignment.getApplication().getTutoringClass()
                    : null;
            if (tutoringClass != null && tutoringClass.getCreator() != null) {
                User creator = tutoringClass.getCreator();
                PartyRole creatorRole = tutorCenterRepository.findByUser_UserId(creator.getUserId()).isPresent()
                        ? PartyRole.CENTER
                        : PartyRole.CLIENT;
                contractSignatureRepository.save(createSignatureSlot(contract, creatorRole, creator.getEmail()));
            }
            return;
        }

        if (contract.getClassStudent() != null) {
            ClassStudent classStudent = contract.getClassStudent();
            TutoringClass tutoringClass = classStudent.getTutoringClass();
            if (tutoringClass != null && tutoringClass.getCreator() != null) {
                User creator = tutoringClass.getCreator();
                PartyRole creatorRole = tutorCenterRepository.findByUser_UserId(creator.getUserId()).isPresent()
                        ? PartyRole.CENTER
                        : PartyRole.CLIENT;
                contractSignatureRepository.save(createSignatureSlot(contract, creatorRole, creator.getEmail()));
            }
            if (classStudent.getEnrolledByUser() != null) {
                contractSignatureRepository.save(createSignatureSlot(
                        contract, PartyRole.CLIENT, classStudent.getEnrolledByUser().getEmail()));
            } else if (classStudent.getStudentEmail() != null) {
                contractSignatureRepository.save(createSignatureSlot(
                        contract, PartyRole.CLIENT, classStudent.getStudentEmail()));
            }
        }
    }

    private int getRequiredSignatureCount(Contract contract) {
        // Số chữ ký cần = số ô ký đã tạo cho hợp đồng (center ký sẵn + bên còn lại ký OTP).
        // Đếm theo ô ký thực tế -> đúng cho cả dữ liệu cũ (1 ô) lẫn mới (2 ô).
        int slots = contractSignatureRepository.findByContractId(contract.getContractId()).size();
        return slots > 0 ? slots : 2;
    }

    private ContractTemplate findActiveTemplate() {
        return contractTemplateRepository.findAll().stream()
                .filter(t -> t.getStatus() == ContractTemplateStatus.ACTIVE)
                .filter(t -> !isRecruitmentTemplate(t.getTemplateId()))
                .findFirst()
                .orElse(null);
    }

    private String buildTermsSummary(ClassAssignment assignment) {
        TutorApplication application = assignment.getApplication();
        if (application == null || application.getTutoringClass() == null) {
            return "Hợp đồng gia sư - Phân công #" + assignment.getAssignmentId();
        }
        TutoringClass tutoringClass = application.getTutoringClass();
        return String.format("Hợp đồng gia sư: %s - %d buổi, học phí %s VNĐ",
                tutoringClass.getTitle(),
                tutoringClass.getNumberOfSessions(),
                tutoringClass.getTuitionFee());
    }

    private String buildCenterTermsSummary(ClassStudent classStudent) {
        TutoringClass tutoringClass = classStudent.getTutoringClass();
        return String.format("Hợp đồng trung tâm: %s - học viên %s, %d buổi, học phí %s VNĐ",
                tutoringClass.getTitle(),
                classStudent.getStudentName(),
                tutoringClass.getNumberOfSessions(),
                tutoringClass.getTuitionFee());
    }

    private String generateOtp() {
        int otp = SECURE_RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH - 1) * 9)
                + (int) Math.pow(10, OTP_LENGTH - 1);
        return String.valueOf(otp);
    }

    private String generateContractNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long todayCount = contractRepository.countTodayContracts() + 1;
        return String.format("TCS-%s-%04d", datePart, todayCount);
    }

    private void publishContractSigned(Contract contract) {
        Long classId = null;
        Long payerUserId = null;
        Long beneficiaryUserId = null;
        BigDecimal amount = BigDecimal.ZERO;
        Long assignmentId = null;
        Long classStudentId = null;

        if (contract.getAssignment() != null) {
            assignmentId = contract.getAssignment().getAssignmentId();
            TutorApplication application = contract.getAssignment().getApplication();
            if (application != null && application.getTutoringClass() != null) {
                TutoringClass tutoringClass = application.getTutoringClass();
                classId = tutoringClass.getClassId();
                amount = resolvePrivateEscrowAmount(tutoringClass, contract.getAssignment());
                payerUserId = tutoringClass.getCreator() != null ? tutoringClass.getCreator().getUserId() : null;
            }
            beneficiaryUserId = contract.getAssignment().getTutor() != null
                    && contract.getAssignment().getTutor().getUser() != null
                    ? contract.getAssignment().getTutor().getUser().getUserId()
                    : null;
        } else if (contract.getClassStudent() != null) {
            classStudentId = contract.getClassStudent().getClassStudentId();
            TutoringClass tutoringClass = contract.getClassStudent().getTutoringClass();
            if (tutoringClass != null) {
                classId = tutoringClass.getClassId();
                amount = resolveCenterEscrowAmount(tutoringClass);
                payerUserId = contract.getClassStudent().getEnrolledByUser() != null
                        ? contract.getClassStudent().getEnrolledByUser().getUserId()
                        : null;
                beneficiaryUserId = tutoringClass.getCreator() != null
                        ? tutoringClass.getCreator().getUserId()
                        : null;
            }
        }

        if (payerUserId != null && amount.compareTo(BigDecimal.ZERO) > 0
                && (assignmentId != null || classStudentId != null)) {
            escrowService.preparePayment(new EscrowLockCommand(payerUserId, amount, assignmentId, classStudentId));
        } else {
            log.warn("[Contract] Bỏ qua khóa escrow: payer={}, amount={}, assignmentId={}, classStudentId={}",
                    payerUserId, amount, assignmentId, classStudentId);
        }

        eventPublisher.publishEvent(new ContractSigned(
                contract.getContractId(),
                classId,
                payerUserId,
                beneficiaryUserId,
                amount,
                assignmentId,
                classStudentId));
    }

    private BigDecimal resolvePrivateEscrowAmount(TutoringClass tutoringClass, ClassAssignment assignment) {
        BigDecimal totalAmount = resolvePrivateContractTotalAmount(tutoringClass, assignment);
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        int plannedMonths = plannedPrivateClassMonths(tutoringClass);
        if (plannedMonths <= 1) {
            return totalAmount.setScale(2, RoundingMode.HALF_UP);
        }
        return totalAmount.divide(BigDecimal.valueOf(plannedMonths), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolvePrivateContractTotalAmount(TutoringClass tutoringClass, ClassAssignment assignment) {
        BigDecimal totalAmount = resolvePrivateDealTotalAmount(tutoringClass, assignment);
        return totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalAmount.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private BigDecimal resolveCenterEscrowAmount(TutoringClass tutoringClass) {
        BigDecimal totalAmount = positiveAmount(tutoringClass.getBudget());
        if (totalAmount == null && tutoringClass.getTuitionFee() != null
                && tutoringClass.getNumberOfSessions() != null) {
            totalAmount = tutoringClass.getTuitionFee()
                    .multiply(BigDecimal.valueOf(tutoringClass.getNumberOfSessions()));
        }
        return totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalAmount.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private BigDecimal resolvePrivateDealTotalAmount(TutoringClass tutoringClass, ClassAssignment assignment) {
        if (tutoringClass == null || !StringUtils.hasText(tutoringClass.getDetailsJson())) {
            return null;
        }
        JsonNode root = readTree(filterDetailsToSubjects(tutoringClass.getDetailsJson(), acceptedSubjectKeys(assignment)));
        JsonNode subjectFeesNode = root.path("subjectFees");
        JsonNode slots = root.path("slots");
        if (subjectFeesNode == null || !subjectFeesNode.isObject() || slots == null || !slots.isArray()) {
            return null;
        }
        Map<String, BigDecimal> fees = readRates(subjectFeesNode.toString());
        if (fees == null || fees.isEmpty()) {
            return null;
        }
        int repeats = Math.max(1, patternRepeats(root));
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : fees.entrySet()) {
            BigDecimal fee = entry.getValue();
            if (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal hours = hoursPerRepeatForSubject(root, entry.getKey());
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total = total.add(fee.multiply(hours).multiply(BigDecimal.valueOf(repeats)));
        }
        return total.compareTo(BigDecimal.ZERO) > 0 ? total : null;
    }

    private Set<String> acceptedSubjectKeys(ClassAssignment assignment) {
        if (assignment == null || assignment.getApplication() == null) {
            return null;
        }
        Map<String, BigDecimal> rates = readRates(assignment.getApplication().getProposedRatesJson());
        if (rates == null || rates.isEmpty()) {
            return null;
        }
        return rates.keySet();
    }

    private String filterDetailsToSubjects(String detailsJson, Set<String> keptKeys) {
        if (keptKeys == null || keptKeys.isEmpty() || !StringUtils.hasText(detailsJson)) {
            return detailsJson;
        }
        JsonNode root = readTree(detailsJson);
        if (root == null || !root.isObject()) {
            return detailsJson;
        }
        com.fasterxml.jackson.databind.node.ObjectNode obj =
                (com.fasterxml.jackson.databind.node.ObjectNode) root;

        JsonNode ids = obj.get("subjectIds");
        if (ids != null && ids.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode keptIds = OBJECT_MAPPER.createArrayNode();
            for (JsonNode id : ids) {
                if (keptKeys.contains(id.asText())) {
                    keptIds.add(id);
                }
            }
            if (!keptIds.isEmpty()) {
                obj.set("subjectIds", keptIds);
            }
        }

        JsonNode feesNode = obj.get("subjectFees");
        if (feesNode != null && feesNode.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode fees = OBJECT_MAPPER.createObjectNode();
            for (String key : keptKeys) {
                if (feesNode.has(key)) {
                    fees.set(key, feesNode.get(key));
                }
            }
            obj.set("subjectFees", fees);
        }

        JsonNode slots = obj.get("slots");
        if (slots != null && slots.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode keptSlots = OBJECT_MAPPER.createArrayNode();
            for (JsonNode slot : slots) {
                String slotKey = slot.path("subjectId").asText("");
                if (slotKey.isEmpty() || keptKeys.contains(slotKey)) {
                    keptSlots.add(slot);
                }
            }
            obj.set("slots", keptSlots);
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception ignored) {
            return detailsJson;
        }
    }

    private JsonNode readTree(String json) {
        if (!StringUtils.hasText(json)) {
            return OBJECT_MAPPER.createObjectNode();
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception ignored) {
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    private Map<String, BigDecimal> readRates(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, BigDecimal>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private Set<Integer> studyWeeksOf(JsonNode form, int cycleWeeks) {
        Set<Integer> weeks = new LinkedHashSet<>();
        for (JsonNode w : form.path("studyWeeks")) {
            int v = w.asInt(0);
            if (v >= 1 && v <= cycleWeeks) {
                weeks.add(v);
            }
        }
        if (weeks.isEmpty()) {
            weeks.add(1);
        }
        return weeks;
    }

    private int durationCountOf(JsonNode form) {
        return Math.max(1, form.path("months").asInt(1));
    }

    private int totalMonthsOf(JsonNode form) {
        int n = durationCountOf(form);
        return "YEAR".equals(form.path("durationUnit").asText("MONTH")) ? n * 12 : n;
    }

    private int weeksForCycle(JsonNode form) {
        return switch (form.path("billingCycle").asText("MONTH")) {
            case "MONTH" -> totalMonthsOf(form) * 4;
            case "TERM" -> 12;
            case "QUARTER" -> 24;
            case "YEAR" -> 48;
            default -> 4;
        };
    }

    private int repeatWeeksOf(JsonNode form) {
        if (!"WEEKLY".equals(form.path("scheduleMode").asText("WEEKLY"))) {
            return 1;
        }
        int n = form.path("repeatEveryWeeks").asInt(1);
        return Math.min(4, Math.max(1, n));
    }

    private int patternRepeats(JsonNode form) {
        int weeks = weeksForCycle(form);
        int cycleWeeks = repeatWeeksOf(form);
        if (cycleWeeks <= 1) {
            return weeks;
        }
        Set<Integer> on = studyWeeksOf(form, cycleWeeks);
        int remainder = weeks % cycleWeeks;
        return (weeks / cycleWeeks) * on.size() + (int) on.stream().filter(w -> w <= remainder).count();
    }

    private BigDecimal hoursPerRepeatForSubject(JsonNode form, String subjectId) {
        JsonNode slots = form.path("slots");
        if (slots == null || !slots.isArray() || !StringUtils.hasText(subjectId)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode slot : slots) {
            if (!subjectId.equals(slot.path("subjectId").asText(""))) {
                continue;
            }
            total = total.add(slotHours(slot.path("start").asText(""), slot.path("end").asText("")));
        }
        return total;
    }

    private int plannedPrivateClassMonths(TutoringClass tutoringClass) {
        if (tutoringClass.getStartDate() == null || tutoringClass.getEndDate() == null) {
            return 1;
        }
        if (tutoringClass.getEndDate().isBefore(tutoringClass.getStartDate())) {
            return 1;
        }
        long inclusiveDays = ChronoUnit.DAYS.between(
                tutoringClass.getStartDate(),
                tutoringClass.getEndDate().plusDays(1));
        return Math.max(1, (int) Math.ceil(inclusiveDays / 30.0));
    }

    private BigDecimal positiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? amount : null;
    }

    private BigDecimal slotHours(String start, String end) {
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            return BigDecimal.ZERO;
        }
        try {
            LocalTime s = LocalTime.parse(start);
            LocalTime e = LocalTime.parse(end);
            int startMin = s.getHour() * 60 + s.getMinute();
            int endMin = e.getHour() * 60 + e.getMinute();
            int diff = endMin - startMin;
            return diff > 0 ? BigDecimal.valueOf(diff).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private ContractResponse toContractResponse(Contract contract) {
        ContractResponse.ContractResponseBuilder builder = ContractResponse.builder()
                .contractId(contract.getContractId())
                .contractNo(contract.getContractNo())
                .status(contract.getStatus())
                .sourceType(contract.getSourceType())
                .assignmentId(contract.getAssignment() != null ? contract.getAssignment().getAssignmentId() : null)
                .classStudentId(contract.getClassStudent() != null
                        ? contract.getClassStudent().getClassStudentId()
                        : null)
                .recruitmentApplicationId(contract.getRecruitmentApplication() != null
                        ? contract.getRecruitmentApplication().getRecruitmentAppId()
                        : null)
                .templateId(contract.getTemplate() != null ? contract.getTemplate().getTemplateId() : null)
                .templateName(contract.getTemplate() != null ? contract.getTemplate().getName() : null)
                .termsSummary(contract.getTermsSummary())
                .documentText(assembleDocument(contract))
                .contractFileUrl(contract.getContractFileUrl())
                .signedAt(contract.getSignedAt())
                .expiresAt(contract.getExpiresAt())
                .confirmedAt(contract.getConfirmedAt())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt());

        fillContractPartiesAndClass(builder, contract);

        // BF-03: hợp đồng hợp tác -> lấy thông tin gia sư + trung tâm từ đơn tuyển dụng.
        if (contract.getRecruitmentApplication() != null) {
            RecruitmentApplication app = contract.getRecruitmentApplication();
            Tutor tutor = app.getTutor();
            if (tutor != null) {
                builder.tutorId(tutor.getUser().getUserId());
                builder.tutorName(tutor.getFullName());
                builder.tutorEmail(tutor.getUser().getEmail());
            }
            RecruitmentPost post = app.getRecruitmentPost();
            if (post != null && post.getCenter() != null) {
                TutorCenter center = post.getCenter();
                builder.centerId(center.getCenterId());
                builder.centerName(center.getCompanyName());
                builder.centerEmail(center.getUser().getEmail());
            }
        }

        // BF-04: hợp đồng học viên -> lấy thông tin lớp + người ghi danh + trung tâm.
        if (contract.getClassStudent() != null) {
            ClassStudent cs = contract.getClassStudent();
            TutoringClass cls = cs.getTutoringClass();
            if (cls != null) {
                builder.classId(cls.getClassId());
                if (cls.getCreator() != null) {
                    tutorCenterRepository.findByUser_UserId(cls.getCreator().getUserId())
                            .ifPresent(center -> {
                                builder.centerId(center.getCenterId());
                                builder.centerName(center.getCompanyName());
                                builder.centerEmail(center.getUser().getEmail());
                            });
                }
            }
            if (cs.getEnrolledByUser() != null) {
                clientRepository.findByUser_UserId(cs.getEnrolledByUser().getUserId())
                        .ifPresent(client -> {
                            builder.clientId(client.getClientId());
                            builder.clientName(client.getFullName());
                            builder.clientEmail(client.getUser().getEmail());
                        });
            }
        }

        int required = getRequiredSignatureCount(contract);
        int signed = contractSignatureRepository.countSignedByContractId(contract.getContractId());
        builder.requiredSignatures(required)
                .signedCount(signed)
                .hasAllSignatures(signed >= required);
        RefundPolicy refundPolicy = resolveRefundPolicy(contract);
        builder.escrowPayment(toEscrowPaymentInfo(resolveContractEscrow(contract), resolveContractEscrowPayment(contract)))
                .refundPayoutInfo(toRefundPayoutInfoView(contract))
                .totalSessions(refundPolicy.totalSessions())
                .completedSessions(refundPolicy.completedSessions())
                .refundAllowed(refundPolicy.allowed())
                .refundBlockedReason(refundPolicy.blockedReason());

        return builder.build();
    }

    private ContractResponse.RefundPayoutInfoView toRefundPayoutInfoView(Contract contract) {
        if (contract == null) {
            return null;
        }
        RefundPayoutInfo payoutInfo = null;
        if (contract.getClassStudent() != null) {
            payoutInfo = RefundPayoutInfoCodec.parseFromReason(contract.getClassStudent().getNotes());
        } else if (contract.getAssignment() != null) {
            payoutInfo = RefundPayoutInfoCodec.parseFromReason(contract.getAssignment().getTermsB());
        }
        if (!RefundPayoutInfoCodec.hasCompletePayout(payoutInfo)) {
            return null;
        }
        return ContractResponse.RefundPayoutInfoView.builder()
                .bankName(RefundPayoutInfoCodec.normalize(payoutInfo.bankName()))
                .accountNoMasked(RefundPayoutInfoCodec.maskAccountNo(payoutInfo.accountNo()))
                .accountHolderName(RefundPayoutInfoCodec.normalize(payoutInfo.accountHolderName()))
                .build();
    }

    private RefundPolicy resolveRefundPolicy(Contract contract) {
        TutoringClass tutoringClass = resolveContractClass(contract);
        ClassStudent classStudent = contract.getClassStudent();
        if (tutoringClass == null
                || tutoringClass.getClassType() != ClassType.CENTER
                || classStudent == null) {
            return new RefundPolicy(null, null, true, null);
        }

        int totalSessions = totalSessions(tutoringClass);
        int completedSessions = completedCenterSessions(tutoringClass, classStudent);
        boolean allowed = totalSessions <= 0 || completedSessions * 2 <= totalSessions;
        return new RefundPolicy(
                totalSessions,
                completedSessions,
                allowed,
                allowed ? null : "Lớp trung tâm đã học quá 50% số buổi nên không thể yêu cầu hoàn tiền.");
    }

    private TutoringClass resolveContractClass(Contract contract) {
        if (contract.getClassStudent() != null) {
            return contract.getClassStudent().getTutoringClass();
        }
        if (contract.getAssignment() != null
                && contract.getAssignment().getApplication() != null) {
            return contract.getAssignment().getApplication().getTutoringClass();
        }
        return null;
    }

    private int totalSessions(TutoringClass tutoringClass) {
        Integer configuredSessions = tutoringClass.getNumberOfSessions();
        if (configuredSessions != null && configuredSessions > 0) {
            return configuredSessions;
        }
        return lessonRepository.findByTutoringClass_ClassId(tutoringClass.getClassId()).size();
    }

    private int completedCenterSessions(TutoringClass tutoringClass, ClassStudent classStudent) {
        List<Long> lessonIds = lessonRepository.findByTutoringClass_ClassId(tutoringClass.getClassId()).stream()
                .map(Lesson::getLessonId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (lessonIds.isEmpty() || classStudent.getClassStudentId() == null) {
            return 0;
        }
        return (int) lessonAttendanceRepository.findByLesson_LessonIdIn(lessonIds).stream()
                .filter(attendance -> attendance.getClassStudent() != null
                        && java.util.Objects.equals(
                        attendance.getClassStudent().getClassStudentId(),
                        classStudent.getClassStudentId()))
                .filter(attendance -> attendance.getStatus() == com.tcs.module.marketplace.enums.LessonAttendanceStatus.PRESENT)
                .count();
    }

    private record RefundPolicy(
            Integer totalSessions,
            Integer completedSessions,
            boolean allowed,
            String blockedReason) {
    }

    private EscrowTransaction resolveContractEscrow(Contract contract) {
        if (contract == null) {
            return null;
        }
        if (contract.getAssignment() != null && contract.getAssignment().getAssignmentId() != null) {
            return escrowTransactionRepository
                    .findByAssignment_AssignmentId(contract.getAssignment().getAssignmentId())
                    .orElse(null);
        }
        if (contract.getClassStudent() != null && contract.getClassStudent().getClassStudentId() != null) {
            return escrowTransactionRepository
                    .findByClassStudent_ClassStudentId(contract.getClassStudent().getClassStudentId())
                    .orElse(null);
        }
        return null;
    }

    private PaymentTransaction resolveContractEscrowPayment(Contract contract) {
        if (contract == null) {
            return null;
        }
        String reference = null;
        if (contract.getAssignment() != null && contract.getAssignment().getAssignmentId() != null) {
            reference = PRIVATE_ESCROW_REF_PREFIX + contract.getAssignment().getAssignmentId();
        } else if (contract.getClassStudent() != null && contract.getClassStudent().getClassStudentId() != null) {
            reference = CENTER_ESCROW_REF_PREFIX + contract.getClassStudent().getClassStudentId();
        }
        return StringUtils.hasText(reference)
                ? paymentTransactionRepository.findByReferenceCode(reference).orElse(null)
                : null;
    }

    private ContractResponse.EscrowPaymentInfo toEscrowPaymentInfo(EscrowTransaction escrow, PaymentTransaction fallbackPayment) {
        if (escrow == null && fallbackPayment == null) {
            return null;
        }

        PaymentTransaction payment = escrow != null ? escrow.getPayment() : fallbackPayment;
        BigDecimal amount = payment != null && payment.getAmount() != null
                ? payment.getAmount()
                : escrow != null ? escrow.getAmount() : null;
        String reference = payment != null ? payment.getReferenceCode() : null;
        return ContractResponse.EscrowPaymentInfo.builder()
                .escrowId(escrow != null ? escrow.getEscrowId() : null)
                .escrowStatus(escrow != null ? escrow.getStatus() : EscrowStatus.PENDING)
                .paymentTransactionId(payment != null ? payment.getTransactionId() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .amount(amount)
                .referenceCode(reference)
                .bankName(ESCROW_BANK_NAME)
                .bankBin(ESCROW_BANK_BIN)
                .accountNumber(ESCROW_ACCOUNT_NUMBER)
                .accountName(ESCROW_ACCOUNT_NAME)
                .transferContent(reference)
                .qrUrl(buildEscrowQrUrl(amount, reference))
                .depositedAt(escrow != null ? escrow.getDepositedAt() : null)
                .processedAt(payment != null ? payment.getProcessedAt() : null)
                .build();
    }

    private String buildEscrowQrUrl(BigDecimal amount, String transferContent) {
        if (amount == null || transferContent == null || transferContent.isBlank()) {
            return null;
        }
        return "https://img.vietqr.io/image/"
                + ESCROW_BANK_BIN
                + "-"
                + ESCROW_ACCOUNT_NUMBER
                + "-compact2.png"
                + "?amount="
                + amount.setScale(0, RoundingMode.DOWN).toPlainString()
                + "&addInfo="
                + urlEncode(transferContent)
                + "&accountName="
                + urlEncode(ESCROW_ACCOUNT_NAME);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void fillContractPartiesAndClass(
            ContractResponse.ContractResponseBuilder builder,
            Contract contract) {
        if (contract.getAssignment() != null) {
            ClassAssignment assignment = contract.getAssignment();
            Tutor tutor = assignment.getTutor();
            if (tutor != null && tutor.getUser() != null) {
                builder.tutorId(tutor.getUser().getUserId())
                        .tutorName(tutor.getFullName())
                        .tutorEmail(tutor.getUser().getEmail())
                        .tutor(ContractResponse.PartyInfo.builder()
                                .userId(tutor.getUser().getUserId())
                                .fullName(tutor.getFullName())
                                .email(tutor.getUser().getEmail())
                                .phone(tutor.getPhone())
                                .build());
            }

            TutoringClass tutoringClass = assignment.getApplication() != null
                    ? assignment.getApplication().getTutoringClass()
                    : null;
            if (tutoringClass != null) {
                fillClassFields(builder, tutoringClass);
                fillPrivateContractAmounts(builder, tutoringClass, assignment);
                fillCreatorParty(builder, tutoringClass.getCreator());
            }
            return;
        }

        if (contract.getClassStudent() != null) {
            ClassStudent classStudent = contract.getClassStudent();
            TutoringClass tutoringClass = classStudent.getTutoringClass();
            if (tutoringClass != null) {
                fillClassFields(builder, tutoringClass);
                fillCreatorParty(builder, tutoringClass.getCreator());
            }
            if (classStudent.getEnrolledByUser() != null) {
                fillClientParty(builder, classStudent.getEnrolledByUser());
            }
        }
    }

    private void fillClassFields(ContractResponse.ContractResponseBuilder builder, TutoringClass tutoringClass) {
        builder.classId(tutoringClass.getClassId())
                .classTitle(tutoringClass.getTitle())
                .classType(tutoringClass.getClassType() != null ? tutoringClass.getClassType().name() : null)
                .tuitionFee(tutoringClass.getTuitionFee())
                .lessonMode(tutoringClass.getLessonMode() != null ? tutoringClass.getLessonMode().name() : null)
                .numberOfSessions(tutoringClass.getNumberOfSessions());
    }

    private void fillPrivateContractAmounts(
            ContractResponse.ContractResponseBuilder builder,
            TutoringClass tutoringClass,
            ClassAssignment assignment) {
        BigDecimal totalTuitionAmount = resolvePrivateContractTotalAmount(tutoringClass, assignment);
        BigDecimal escrowAmount = resolvePrivateEscrowAmount(tutoringClass, assignment);
        builder.tuitionFee(totalTuitionAmount)
                .totalTuitionAmount(totalTuitionAmount)
                .escrowAmount(escrowAmount);
    }

    private void fillCreatorParty(ContractResponse.ContractResponseBuilder builder, User creator) {
        if (creator == null) {
            return;
        }
        Optional<TutorCenter> centerOpt = tutorCenterRepository.findByUser_UserId(creator.getUserId());
        if (centerOpt.isPresent()) {
            TutorCenter center = centerOpt.get();
            builder.centerId(center.getCenterId())
                    .centerName(center.getCompanyName())
                    .centerEmail(center.getUser().getEmail())
                    .center(ContractResponse.PartyInfo.builder()
                            .userId(center.getUser().getUserId())
                            .fullName(center.getCompanyName())
                            .email(center.getUser().getEmail())
                            .phone(center.getPhone())
                            .build());
            return;
        }
        fillClientParty(builder, creator);
    }

    private void fillClientParty(ContractResponse.ContractResponseBuilder builder, User user) {
        Client client = clientRepository.findByUser_UserId(user.getUserId()).orElse(null);
        String name = client != null ? client.getFullName() : user.getEmail();
        String phone = client != null ? client.getPhone() : user.getPhone();
        Long clientId = client != null ? client.getClientId() : user.getUserId();
        builder.clientId(clientId)
                .clientName(name)
                .clientEmail(user.getEmail())
                .client(ContractResponse.PartyInfo.builder()
                        .userId(user.getUserId())
                        .fullName(name)
                        .email(user.getEmail())
                        .phone(phone)
                        .build());
    }

    private ContractSignatureResponse toSignatureResponse(
            ContractSignature signature, Long viewerId, PartyRole viewerRole) {
        Integer attempts = signature.getOtpAttempts() != null ? signature.getOtpAttempts() : 0;
        // "Của tôi" = ô ký do chính người xem đã ký (khớp signer) HOẶC ô còn chờ ký thuộc đúng
        // vai trò của người xem (signer chưa có nhưng party trùng vai trò).
        boolean mine = (signature.getSigner() != null && viewerId != null
                        && signature.getSigner().getUserId().equals(viewerId))
                || (signature.getSigner() == null && viewerRole != null
                        && signature.getPartyRole() == viewerRole);
        ContractSignatureResponse.ContractSignatureResponseBuilder builder = ContractSignatureResponse.builder()
                .signatureId(signature.getSignatureId())
                .partyRole(signature.getPartyRole())
                .partyLabel(partyLabel(signature.getPartyRole()))
                .signatureStatus(signature.getSignatureStatus())
                .signedAt(signature.getSignedAt())
                .otpExpiresAt(signature.getOtpExpiresAt())
                .remainingOtpAttempts(Math.max(0, OTP_MAX_ATTEMPTS - attempts))
                .isOtpExpired(signature.getOtpExpiresAt() != null
                        && signature.getOtpExpiresAt().isBefore(LocalDateTime.now()))
                .isCurrentUser(mine)
                .signerName(resolveSignatureName(signature))
                .signerEmail(signature.getSigner() != null
                        ? signature.getSigner().getEmail()
                        : signature.getEmail());

        if (signature.getSigner() != null) {
            builder.signerId(signature.getSigner().getUserId());
        }
        return builder.build();
    }

    private String resolveSignatureName(ContractSignature signature) {
        if (signature.getSigner() != null) {
            return getUserDisplayName(signature.getSigner());
        }
        return partyLabel(signature.getPartyRole());
    }

    private String getUserDisplayName(User user) {
        return tutorRepository.findByUser_UserId(user.getUserId())
                .map(Tutor::getFullName)
                .orElseGet(() -> clientRepository.findByUser_UserId(user.getUserId())
                        .map(Client::getFullName)
                        .orElseGet(() -> tutorCenterRepository.findByUser_UserId(user.getUserId())
                                .map(TutorCenter::getCompanyName)
                                .orElse(user.getEmail())));
    }

    private String partyLabel(PartyRole role) {
        if (role == null) {
            return "Bên ký";
        }
        return switch (role) {
            case CLIENT -> "Học viên / Phụ huynh";
            case TUTOR -> "Gia sư";
            case CENTER -> "Trung tâm";
        };
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
    public ReviewResponse createReview(CreateReviewRequest request) {
        Long clientId = authHelper.requireRole(UserRole.CLIENT).getUserId();
        if (request == null || request.getAssignmentId() == null) {
            throw new IllegalArgumentException("Thiếu thông tin đánh giá");
        }

        ClassAssignment assignment = classAssignmentRepository
                .findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));
        TutoringClass tutoringClass = assignment.getApplication() != null
                ? assignment.getApplication().getTutoringClass()
                : null;
        if (tutoringClass == null || tutoringClass.getCreator() == null
                || !tutoringClass.getCreator().getUserId().equals(clientId)) {
            throw new BusinessException("Bạn chỉ có thể đánh giá lớp học của mình");
        }

        // Điều kiện: đã có buổi diễn ra và chưa vượt số lượt đánh giá cho phép.
        List<LocalDate> occurred = occurredLessonDates(tutoringClass.getClassId());
        if (occurred.isEmpty()) {
            throw new BusinessException("Chưa có buổi học nào diễn ra để đánh giá");
        }
        long submitted = reviewRepository.findByReviewer_UserId(clientId).stream()
                .filter(r -> r.getReviewType() == ReviewType.CLIENT_TO_TUTOR)
                .filter(r -> r.getAssignment().getAssignmentId().equals(assignment.getAssignmentId()))
                .count();
        if (submitted >= occurred.size()) {
            throw new BusinessException("Bạn đã đánh giá đủ số lượt cho các buổi đã học");
        }

        BigDecimal overallRating = resolveOverallRating(request);
        if (overallRating.compareTo(BigDecimal.ONE) < 0
                || overallRating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        }

        Tutor tutor = assignment.getTutor();
        User reviewer = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Review review = new Review();
        review.setAssignment(assignment);
        review.setTutoringClass(tutoringClass);
        review.setReviewer(reviewer);
        review.setReviewee(tutor.getUser());
        review.setReviewType(ReviewType.CLIENT_TO_TUTOR);
        review.setRating(overallRating);
        review.setComment(trimToNull(request.getComment()));
        review.setCriteriaJson(serializeCriteria(request.getCriteria()));
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        review.setAnonymous(anonymous);
        review.setDisplayName(anonymous ? trimToNull(request.getDisplayName()) : null);
        Review saved = reviewRepository.save(review);

        recomputeTutorReputation(tutor, tutor.getUser().getUserId());

        // Client đã đánh giá -> nếu gia sư đã yêu cầu hoàn thành, marketplace sẽ đóng lớp + giải ngân.
        eventPublisher.publishEvent(new ClientReviewedClassEvent(tutoringClass.getClassId()));
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasClientReviewedClass(Long classId) {
        return reviewRepository.existsByTutoringClass_ClassIdAndReviewType(
                classId, ReviewType.CLIENT_TO_TUTOR);
    }

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
