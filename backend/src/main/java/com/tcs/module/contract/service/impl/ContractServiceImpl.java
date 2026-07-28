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
import com.tcs.module.contract.enums.PartyRole;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.repository.ContractTemplateRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
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
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Contract generateForAssignment(Long assignmentId) {
        ClassAssignment assignment = classAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        if (contractRepository.findByAssignment_AssignmentId(assignmentId).isPresent()) {
            throw new IllegalArgumentException("Hợp đồng đã tồn tại cho phân công này");
        }

        Long currentUserId = authHelper.currentUserId();
        if (!isAssignmentParty(assignment, currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền tạo hợp đồng cho phân công này");
        }

        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
        contract.setAssignment(assignment);
        contract.setTemplate(findActiveTemplate().orElse(null));
        contract.setStatus(ContractStatus.PENDING);
        contract.setSourceType(ContractSourceType.PRIVATE);
        contract.setExpiresAt(LocalDateTime.now().plusDays(CONTRACT_EXPIRY_DAYS));
        contract.setTermsSummary(buildTermsSummary(assignment, contract.getTemplate()));
        Contract saved = contractRepository.save(contract);
        initializeSignatureSlotsForAssignment(saved, assignment);
        return saved;
    }

    @Override
    @Transactional
    public Contract generateForEnrollment(Long classStudentId) {
        ClassStudent classStudent = classStudentRepository.findById(classStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh"));

        if (contractRepository.findByClassStudent_ClassStudentId(classStudentId).isPresent()) {
            throw new IllegalArgumentException("Hợp đồng đã tồn tại cho ghi danh này");
        }

        Long currentUserId = authHelper.currentUserId();
        if (!isEnrollmentParty(classStudent, currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền tạo hợp đồng cho ghi danh này");
        }
        if (classStudent.getEnrolledByUser() == null) {
            throw new IllegalArgumentException("Ghi danh chưa có tài khoản người thanh toán để ký hợp đồng");
        }

        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
        contract.setClassStudent(classStudent);
        contract.setTemplate(findActiveTemplate().orElse(null));
        contract.setStatus(ContractStatus.PENDING);
        contract.setSourceType(ContractSourceType.CENTER);
        contract.setExpiresAt(LocalDateTime.now().plusDays(CONTRACT_EXPIRY_DAYS));
        contract.setTermsSummary(buildCenterTermsSummary(classStudent, contract.getTemplate()));
        Contract saved = contractRepository.save(contract);
        initializeSignatureSlotsForEnrollment(saved, classStudent);
        return saved;
    }

    @Override
    @Transactional
    public ContractResponse generateContract(Long assignmentId) {
        return toContractResponse(generateForAssignment(assignmentId), authHelper.currentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContract(Long contractId) {
        return getMyContract(contractId);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getMyContract(Long contractId) {
        Contract contract = findContract(contractId);
        Long currentUserId = authHelper.currentUserId();
        validateAccess(contract, currentUserId);
        return toContractResponse(contract, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts() {
        Long userId = authHelper.currentUserId();
        LinkedHashSet<Contract> contracts = new LinkedHashSet<>();
        contracts.addAll(contractRepository.findByAssignment_Tutor_UserId(userId));
        contracts.addAll(contractRepository.findByAssignment_ClassCreator_UserId(userId));
        contracts.addAll(contractRepository.findByClassStudent_UserId(userId));
        return contracts.stream()
                .map(contract -> toContractResponse(contract, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSignatureListResponse getSignatures(Long contractId) {
        Contract contract = findContract(contractId);
        validateAccess(contract, authHelper.currentUserId());

        List<ContractSignatureResponse> signatures = contractSignatureRepository.findByContractId(contractId).stream()
                .map(this::toSignatureResponse)
                .toList();
        int required = getRequiredSignatureCount(contract);
        int signed = contractSignatureRepository.countSignedByContractId(contractId);

        return ContractSignatureListResponse.builder()
                .contractId(contractId)
                .contractNo(contract.getContractNo())
                .hasAllSignatures(signed >= required)
                .signedCount(signed)
                .requiredSignatures(required)
                .signatures(signatures)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SignatureStatusResponse getSignatureStatus(Long contractId) {
        Contract contract = findContract(contractId);
        Long currentUserId = authHelper.currentUserId();
        validateAccess(contract, currentUserId);

        List<SignatureStatusResponse.SignatureInfo> signatures = contractSignatureRepository
                .findByContractId(contractId).stream()
                .filter(signature -> signature.getSignatureStatus() == ContractSignatureStatus.SIGNED)
                .map(signature -> SignatureStatusResponse.SignatureInfo.builder()
                        .signatureId(signature.getSignatureId())
                        .signerUserId(signature.getSigner() != null ? signature.getSigner().getUserId() : null)
                        .signerName(signature.getSigner() != null ? getDisplayName(signature.getSigner()) : null)
                        .signerRole(resolvePartyLabel(signature.getPartyRole()))
                        .signedAt(signature.getSignedAt())
                        .isCurrentUser(signature.getSigner() != null
                                && signature.getSigner().getUserId().equals(currentUserId))
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
                .signatures(signatures)
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> sendOtp(Long contractId) {
        OtpSentResponse response = sendSignOtp(contractId);
        return Map.of(
                "message", response.getMessage(),
                "expiresInMinutes", OTP_EXPIRY_MINUTES,
                "maxAttempts", OTP_MAX_ATTEMPTS
        );
    }

    @Override
    @Transactional
    public OtpSentResponse sendSignOtp(Long contractId) {
        Contract contract = findContract(contractId);
        Long currentUserId = authHelper.currentUserId();
        validateAccess(contract, currentUserId);
        ensureSignable(contract);

        PartyRole role = resolvePartyRole(contract, currentUserId);
        ContractSignature signature = contractSignatureRepository
                .findByContractIdAndPartyRole(contractId, role)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt ký hợp đồng"));

        if (signature.getSignatureStatus() == ContractSignatureStatus.SIGNED) {
            throw new IllegalStateException("Bạn đã ký hợp đồng này rồi");
        }

        String otp = generateOtp();
        signature.setOtpCode(otp);
        signature.setOtpAttempts(0);
        signature.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        signature.setSignatureStatus(ContractSignatureStatus.PENDING);
        signature.setEmail(resolveCurrentPartyEmail(contract, role));
        contractSignatureRepository.save(signature);

        emailService.sendContractOtp(signature.getEmail(), otp, contract.getContractNo());
        return OtpSentResponse.builder()
                .maskedEmail(maskEmail(signature.getEmail()))
                .message("Mã OTP đã được gửi tới email của bạn")
                .expiresInMinutes(OTP_EXPIRY_MINUTES)
                .maxAttempts(OTP_MAX_ATTEMPTS)
                .build();
    }

    @Override
    @Transactional
    public ContractResponse signWithOtp(Long contractId, SignWithOtpRequest request) {
        if (request == null || request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new IllegalArgumentException("Mã OTP là bắt buộc");
        }
        Contract contract = findContract(contractId);
        sign(contractId, request.getOtpCode(), authHelper.currentUserId());
        return toContractResponse(contract, authHelper.currentUserId());
    }

    @Override
    @Transactional
    public ContractResponse signContract(Long contractId, SignContractRequest request) {
        SignWithOtpRequest otpRequest = new SignWithOtpRequest();
        otpRequest.setOtpCode(request != null ? request.getOtpCode() : null);
        return signWithOtp(contractId, otpRequest);
    }

    @Override
    @Transactional
    public void sign(Long contractId, String otp, Long signerUserId) {
        Long currentUserId = authHelper.currentUserId();
        if (!currentUserId.equals(signerUserId)) {
            throw new ForbiddenException("Không thể ký thay tài khoản khác");
        }

        Contract contract = findContract(contractId);
        validateAccess(contract, currentUserId);
        ensureSignable(contract);

        PartyRole role = resolvePartyRole(contract, currentUserId);
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
        if (signature.getOtpCode() == null || !signature.getOtpCode().equals(otp.trim())) {
            signature.setOtpAttempts(signature.getOtpAttempts() + 1);
            contractSignatureRepository.save(signature);
            int remaining = Math.max(0, OTP_MAX_ATTEMPTS - signature.getOtpAttempts());
            throw new IllegalArgumentException("Mã OTP không đúng. Còn " + remaining + " lần thử.");
        }

        User signer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        signature.setSigner(signer);
        signature.setSignatureStatus(ContractSignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now());
        signature.setSignatureData("OTP_VERIFIED:" + signer.getEmail() + ":"
                + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        signature.setOtpCode(null);
        signature.setOtpExpiresAt(null);
        contractSignatureRepository.save(signature);

        if (isFullySigned(contractId) && contract.getSignedAt() == null) {
            contract.setStatus(ContractStatus.SIGNED);
            contract.setSignedAt(LocalDateTime.now());
            contractRepository.save(contract);
            publishContractSigned(contract);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFullySigned(Long contractId) {
        Contract contract = findContract(contractId);
        return contractSignatureRepository.countSignedByContractId(contractId) >= getRequiredSignatureCount(contract);
    }

    private Contract findContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
    }

    private Optional<ContractTemplate> findActiveTemplate() {
        return contractTemplateRepository.findAll().stream()
                .filter(template -> template.getStatus() != null && "ACTIVE".equals(template.getStatus().name()))
                .findFirst();
    }

    private void ensureSignable(Contract contract) {
        if (contract.getStatus() != ContractStatus.PENDING && contract.getStatus() != ContractStatus.DRAFT) {
            throw new IllegalStateException("Hợp đồng không ở trạng thái chờ ký");
        }
    }

    private boolean isAssignmentParty(ClassAssignment assignment, Long userId) {
        if (assignment.getTutor() != null && assignment.getTutor().getUser() != null
                && userId.equals(assignment.getTutor().getUser().getUserId())) {
            return true;
        }
        TutoringClass cls = resolveClass(assignment);
        return cls != null && cls.getCreator() != null && userId.equals(cls.getCreator().getUserId());
    }

    private boolean isEnrollmentParty(ClassStudent classStudent, Long userId) {
        TutoringClass cls = classStudent.getTutoringClass();
        if (cls != null && cls.getCreator() != null && userId.equals(cls.getCreator().getUserId())) {
            return true;
        }
        return classStudent.getEnrolledByUser() != null
                && userId.equals(classStudent.getEnrolledByUser().getUserId());
    }

    private void validateAccess(Contract contract, Long userId) {
        boolean hasAccess = false;
        if (contract.getAssignment() != null) {
            hasAccess = isAssignmentParty(contract.getAssignment(), userId);
        }
        if (!hasAccess && contract.getClassStudent() != null) {
            hasAccess = isEnrollmentParty(contract.getClassStudent(), userId);
        }
        if (!hasAccess) {
            throw new ForbiddenException("Bạn không có quyền truy cập hợp đồng này");
        }
    }

    private PartyRole resolvePartyRole(Contract contract, Long userId) {
        if (contract.getAssignment() != null) {
            ClassAssignment assignment = contract.getAssignment();
            if (assignment.getTutor() != null && assignment.getTutor().getUser() != null
                    && userId.equals(assignment.getTutor().getUser().getUserId())) {
                return PartyRole.TUTOR;
            }
            TutoringClass cls = resolveClass(assignment);
            if (cls != null && cls.getCreator() != null && userId.equals(cls.getCreator().getUserId())) {
                return tutorCenterRepository.findByUser_UserId(userId).isPresent()
                        ? PartyRole.CENTER : PartyRole.CLIENT;
            }
        }
        if (contract.getClassStudent() != null) {
            ClassStudent classStudent = contract.getClassStudent();
            TutoringClass cls = classStudent.getTutoringClass();
            if (cls != null && cls.getCreator() != null && userId.equals(cls.getCreator().getUserId())) {
                return PartyRole.CENTER;
            }
            if (classStudent.getEnrolledByUser() != null
                    && userId.equals(classStudent.getEnrolledByUser().getUserId())) {
                return PartyRole.CLIENT;
            }
        }
        throw new ForbiddenException("Bạn không phải là bên liên quan đến hợp đồng này");
    }

    private int getRequiredSignatureCount(Contract contract) {
        return 2;
    }

    private void initializeSignatureSlotsForAssignment(Contract contract, ClassAssignment assignment) {
        Tutor tutor = assignment.getTutor();
        saveSignatureSlot(contract, PartyRole.TUTOR, tutor.getUser().getEmail());

        TutoringClass cls = resolveClass(assignment);
        if (cls == null || cls.getCreator() == null) {
            throw new IllegalArgumentException("Không xác định được người tạo lớp");
        }
        Long creatorUserId = cls.getCreator().getUserId();
        PartyRole creatorRole = tutorCenterRepository.findByUser_UserId(creatorUserId).isPresent()
                ? PartyRole.CENTER : PartyRole.CLIENT;
        saveSignatureSlot(contract, creatorRole, cls.getCreator().getEmail());
    }

    private void initializeSignatureSlotsForEnrollment(Contract contract, ClassStudent classStudent) {
        TutoringClass cls = classStudent.getTutoringClass();
        if (cls == null || cls.getCreator() == null || classStudent.getEnrolledByUser() == null) {
            throw new IllegalArgumentException("Không đủ thông tin bên ký hợp đồng trung tâm");
        }
        saveSignatureSlot(contract, PartyRole.CENTER, cls.getCreator().getEmail());
        saveSignatureSlot(contract, PartyRole.CLIENT, classStudent.getEnrolledByUser().getEmail());
    }

    private void saveSignatureSlot(Contract contract, PartyRole role, String email) {
        ContractSignature signature = new ContractSignature();
        signature.setContract(contract);
        signature.setPartyRole(role);
        signature.setEmail(email);
        signature.setSignatureStatus(ContractSignatureStatus.PENDING);
        signature.setOtpAttempts(0);
        contractSignatureRepository.save(signature);
    }

    private String resolveCurrentPartyEmail(Contract contract, PartyRole role) {
        if (contract.getAssignment() != null) {
            ClassAssignment assignment = contract.getAssignment();
            if (role == PartyRole.TUTOR) {
                return assignment.getTutor().getUser().getEmail();
            }
            TutoringClass cls = resolveClass(assignment);
            if (cls != null && cls.getCreator() != null) {
                return cls.getCreator().getEmail();
            }
        }
        if (contract.getClassStudent() != null) {
            ClassStudent classStudent = contract.getClassStudent();
            if (role == PartyRole.CENTER) {
                return classStudent.getTutoringClass().getCreator().getEmail();
            }
            if (classStudent.getEnrolledByUser() != null) {
                return classStudent.getEnrolledByUser().getEmail();
            }
        }
        throw new IllegalArgumentException("Không xác định được email bên ký");
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        return String.valueOf(SECURE_RANDOM.nextInt(9 * min) + min);
    }

    private String generateContractNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long todayCount = contractRepository.countTodayContracts() + 1;
        return String.format("TCS-%s-%04d", datePart, todayCount);
    }

    private String buildTermsSummary(ClassAssignment assignment, ContractTemplate template) {
        if (template != null && template.getContent() != null && !template.getContent().isBlank()) {
            return template.getContent();
        }
        TutoringClass cls = resolveClass(assignment);
        if (cls == null) {
            return "Hợp đồng gia sư - phân công #" + assignment.getAssignmentId();
        }
        return String.format("Hợp đồng gia sư: %s - %d buổi, học phí %s VNĐ",
                cls.getTitle(),
                cls.getNumberOfSessions(),
                cls.getTuitionFee());
    }

    private String buildCenterTermsSummary(ClassStudent classStudent, ContractTemplate template) {
        if (template != null && template.getContent() != null && !template.getContent().isBlank()) {
            return template.getContent();
        }
        TutoringClass cls = classStudent.getTutoringClass();
        return String.format("Hợp đồng trung tâm: %s - học viên %s, %d buổi, học phí %s VNĐ",
                cls.getTitle(),
                classStudent.getStudentName(),
                cls.getNumberOfSessions(),
                cls.getTuitionFee());
    }

    private TutoringClass resolveClass(ClassAssignment assignment) {
        TutorApplication application = assignment.getApplication();
        return application != null ? application.getTutoringClass() : null;
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
            TutoringClass cls = resolveClass(contract.getAssignment());
            if (cls != null) {
                classId = cls.getClassId();
                amount = cls.getTuitionFee() != null ? cls.getTuitionFee() : BigDecimal.ZERO;
                payerUserId = cls.getCreator() != null ? cls.getCreator().getUserId() : null;
            }
            beneficiaryUserId = contract.getAssignment().getTutor() != null
                    && contract.getAssignment().getTutor().getUser() != null
                    ? contract.getAssignment().getTutor().getUser().getUserId() : null;
        } else if (contract.getClassStudent() != null) {
            classStudentId = contract.getClassStudent().getClassStudentId();
            TutoringClass cls = contract.getClassStudent().getTutoringClass();
            if (cls != null) {
                classId = cls.getClassId();
                amount = cls.getTuitionFee() != null ? cls.getTuitionFee() : BigDecimal.ZERO;
                payerUserId = contract.getClassStudent().getEnrolledByUser() != null
                        ? contract.getClassStudent().getEnrolledByUser().getUserId() : null;
                beneficiaryUserId = cls.getCreator() != null ? cls.getCreator().getUserId() : null;
            }
        }

        if (payerUserId != null && amount.compareTo(BigDecimal.ZERO) > 0
                && (assignmentId != null || classStudentId != null)) {
            escrowService.lock(new EscrowLockCommand(payerUserId, amount, assignmentId, classStudentId));
        } else {
            log.warn("[Contract] Bỏ qua lock escrow: payer={}, amount={}, assignmentId={}, classStudentId={}",
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

    private ContractResponse toContractResponse(Contract contract, Long currentUserId) {
        ContractResponse.ContractResponseBuilder builder = ContractResponse.builder()
                .contractId(contract.getContractId())
                .contractNo(contract.getContractNo())
                .status(contract.getStatus())
                .sourceType(contract.getSourceType())
                .assignmentId(contract.getAssignment() != null ? contract.getAssignment().getAssignmentId() : null)
                .classStudentId(contract.getClassStudent() != null ? contract.getClassStudent().getClassStudentId() : null)
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
            fillAssignmentResponse(builder, contract.getAssignment());
        }
        if (contract.getClassStudent() != null) {
            fillEnrollmentResponse(builder, contract.getClassStudent());
        }

        int required = getRequiredSignatureCount(contract);
        int signed = contractSignatureRepository.countSignedByContractId(contract.getContractId());
        return builder.requiredSignatures(required)
                .signedCount(signed)
                .hasAllSignatures(signed >= required)
                .build();
    }

    private void fillAssignmentResponse(
            ContractResponse.ContractResponseBuilder builder,
            ClassAssignment assignment) {
        TutorApplication application = assignment.getApplication();
        TutoringClass cls = application != null ? application.getTutoringClass() : null;
        Tutor tutor = assignment.getTutor();

        if (cls != null) {
            builder.classId(cls.getClassId())
                    .classTitle(cls.getTitle())
                    .classType(cls.getClassType() != null ? cls.getClassType().name() : null)
                    .tuitionFee(cls.getTuitionFee())
                    .lessonMode(cls.getLessonMode() != null ? cls.getLessonMode().name() : null)
                    .numberOfSessions(cls.getNumberOfSessions());
        }
        if (tutor != null && tutor.getUser() != null) {
            builder.tutorId(tutor.getUser().getUserId())
                    .tutorName(tutor.getFullName())
                    .tutorEmail(tutor.getUser().getEmail())
                    .tutor(toPartyInfo(tutor.getUser(), tutor.getFullName(), tutor.getPhone()));
        }
        if (cls != null && cls.getCreator() != null) {
            fillCreatorResponse(builder, cls.getCreator());
        }
    }

    private void fillEnrollmentResponse(
            ContractResponse.ContractResponseBuilder builder,
            ClassStudent classStudent) {
        TutoringClass cls = classStudent.getTutoringClass();
        if (cls != null) {
            builder.classId(cls.getClassId())
                    .classTitle(cls.getTitle())
                    .classType(cls.getClassType() != null ? cls.getClassType().name() : null)
                    .tuitionFee(cls.getTuitionFee())
                    .lessonMode(cls.getLessonMode() != null ? cls.getLessonMode().name() : null)
                    .numberOfSessions(cls.getNumberOfSessions());
            if (cls.getCreator() != null) {
                fillCreatorResponse(builder, cls.getCreator());
            }
        }
        User payer = classStudent.getEnrolledByUser();
        if (payer != null) {
            String name = getDisplayName(payer);
            builder.clientId(clientRepository.findByUser_UserId(payer.getUserId()).map(Client::getClientId).orElse(null))
                    .clientName(name)
                    .clientEmail(payer.getEmail())
                    .client(toPartyInfo(payer, name, null));
        }
    }

    private void fillCreatorResponse(ContractResponse.ContractResponseBuilder builder, User creator) {
        Optional<TutorCenter> center = tutorCenterRepository.findByUser_UserId(creator.getUserId());
        if (center.isPresent()) {
            TutorCenter value = center.get();
            builder.centerId(value.getCenterId())
                    .centerName(value.getCompanyName())
                    .centerEmail(creator.getEmail())
                    .center(toPartyInfo(creator, value.getCompanyName(), value.getPhone()));
            return;
        }
        Optional<Client> client = clientRepository.findByUser_UserId(creator.getUserId());
        if (client.isPresent()) {
            Client value = client.get();
            builder.clientId(value.getClientId())
                    .clientName(value.getFullName())
                    .clientEmail(creator.getEmail())
                    .client(toPartyInfo(creator, value.getFullName(), value.getPhone()));
            return;
        }
        builder.clientName(creator.getEmail())
                .clientEmail(creator.getEmail())
                .client(toPartyInfo(creator, creator.getEmail(), null));
    }

    private ContractResponse.PartyInfo toPartyInfo(User user, String fullName, String phone) {
        return ContractResponse.PartyInfo.builder()
                .userId(user.getUserId())
                .fullName(fullName)
                .email(user.getEmail())
                .phone(phone)
                .build();
    }

    private ContractSignatureResponse toSignatureResponse(ContractSignature signature) {
        ContractSignatureResponse.ContractSignatureResponseBuilder builder = ContractSignatureResponse.builder()
                .signatureId(signature.getSignatureId())
                .partyRole(signature.getPartyRole())
                .partyLabel(resolvePartyLabel(signature.getPartyRole()))
                .signatureStatus(signature.getSignatureStatus())
                .signedAt(signature.getSignedAt())
                .otpExpiresAt(signature.getOtpExpiresAt())
                .remainingOtpAttempts(Math.max(0, OTP_MAX_ATTEMPTS - signature.getOtpAttempts()))
                .isOtpExpired(signature.getOtpExpiresAt() != null
                        && signature.getOtpExpiresAt().isBefore(LocalDateTime.now()));

        if (signature.getSigner() != null) {
            builder.signerId(signature.getSigner().getUserId())
                    .signerName(getDisplayName(signature.getSigner()))
                    .signerEmail(signature.getSigner().getEmail());
        } else {
            builder.signerEmail(signature.getEmail());
        }
        return builder.build();
    }

    private String resolvePartyLabel(PartyRole role) {
        if (role == null) return "Bên ký";
        return switch (role) {
            case CLIENT -> "Học viên / Phụ huynh";
            case TUTOR -> "Gia sư";
            case CENTER -> "Trung tâm";
        };
    }

    private String getDisplayName(User user) {
        return tutorRepository.findByUser_UserId(user.getUserId())
                .map(Tutor::getFullName)
                .orElseGet(() -> clientRepository.findByUser_UserId(user.getUserId())
                        .map(Client::getFullName)
                        .orElseGet(() -> tutorCenterRepository.findByUser_UserId(user.getUserId())
                                .map(TutorCenter::getCompanyName)
                                .orElse(user.getEmail())));
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
}
