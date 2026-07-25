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
import java.util.LinkedHashMap;
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
        Long userId = authHelper.currentUserId();
        LinkedHashSet<Contract> contracts = new LinkedHashSet<>();
        contracts.addAll(contractRepository.findByAssignment_Tutor_UserId(userId));
        contracts.addAll(contractRepository.findByAssignment_ClassCreator_UserId(userId));
        contracts.addAll(contractRepository.findByClassStudent_UserId(userId));
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

        return ContractSignatureListResponse.builder()
                .contractId(contractId)
                .contractNo(contract.getContractNo())
                .hasAllSignatures(signed >= required)
                .signedCount(signed)
                .requiredSignatures(required)
                .signatures(signatures.stream().map(this::toSignatureResponse).toList())
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> sendOtp(Long contractId) {
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

        emailService.sendContractOtp(recipientEmail, otp, contract.getContractNo());

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
            publishContractSigned(contract);
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
        if (contract.getAssignment() != null) {
            ClassAssignment assignment = contract.getAssignment();
            if (assignment.getTutor() != null
                    && assignment.getTutor().getUser() != null
                    && assignment.getTutor().getUser().getUserId().equals(userId)) {
                return true;
            }
            TutorApplication application = assignment.getApplication();
            return application != null
                    && application.getTutoringClass() != null
                    && application.getTutoringClass().getCreator() != null
                    && application.getTutoringClass().getCreator().getUserId().equals(userId);
        }

        if (contract.getClassStudent() != null) {
            ClassStudent classStudent = contract.getClassStudent();
            TutoringClass tutoringClass = classStudent.getTutoringClass();
            boolean isCreator = tutoringClass != null
                    && tutoringClass.getCreator() != null
                    && tutoringClass.getCreator().getUserId().equals(userId);
            boolean isEnroller = classStudent.getEnrolledByUser() != null
                    && classStudent.getEnrolledByUser().getUserId().equals(userId);
            return isCreator || isEnroller;
        }

        return false;
    }

    private PartyRole resolvePartyRole(Contract contract) {
        Long currentUserId = authHelper.currentUserId();

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

        throw new ForbiddenException("Bạn không phải là bên liên quan đến hợp đồng này");
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
        return 2;
    }

    private ContractTemplate findActiveTemplate() {
        return contractTemplateRepository.findAll().stream()
                .filter(t -> t.getStatus() == ContractTemplateStatus.ACTIVE)
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
                amount = tutoringClass.getTuitionFee() != null ? tutoringClass.getTuitionFee() : BigDecimal.ZERO;
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
                amount = tutoringClass.getTuitionFee() != null ? tutoringClass.getTuitionFee() : BigDecimal.ZERO;
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
            escrowService.lock(new EscrowLockCommand(payerUserId, amount, assignmentId, classStudentId));
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
                .templateId(contract.getTemplate() != null ? contract.getTemplate().getTemplateId() : null)
                .templateName(contract.getTemplate() != null ? contract.getTemplate().getName() : null)
                .termsSummary(contract.getTermsSummary())
                .contractFileUrl(contract.getContractFileUrl())
                .signedAt(contract.getSignedAt())
                .expiresAt(contract.getExpiresAt())
                .confirmedAt(contract.getConfirmedAt())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt());

        fillContractPartiesAndClass(builder, contract);

        int required = getRequiredSignatureCount(contract);
        int signed = contractSignatureRepository.countSignedByContractId(contract.getContractId());
        builder.requiredSignatures(required)
                .signedCount(signed)
                .hasAllSignatures(signed >= required);

        return builder.build();
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

    private ContractSignatureResponse toSignatureResponse(ContractSignature signature) {
        Integer attempts = signature.getOtpAttempts() != null ? signature.getOtpAttempts() : 0;
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
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(0, at));
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
