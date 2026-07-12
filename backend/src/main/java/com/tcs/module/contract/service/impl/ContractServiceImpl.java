package com.tcs.module.contract.service.impl;

import com.tcs.common.event.ContractSigned;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.contract.dto.request.CreateReviewRequest;
import com.tcs.module.contract.dto.request.SignContractRequest;
import com.tcs.module.contract.dto.response.ContractResponse;
import com.tcs.module.contract.dto.response.OtpSentResponse;
import com.tcs.module.contract.dto.response.ReviewResponse;
import com.tcs.module.contract.dto.response.SignatureStatusResponse;
import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.entity.ContractSignature;
import com.tcs.module.contract.enums.ContractStatus;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.contract.repository.ReviewRepository;
import com.tcs.module.contract.service.ContractService;
import com.tcs.module.contract.service.OtpService;
import com.tcs.module.finance.dto.EscrowLockCommand;
import com.tcs.module.finance.service.EscrowService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final AuthHelper authHelper;
    private final ContractRepository contractRepository;
    private final ContractSignatureRepository signatureRepository;
    private final ReviewRepository reviewRepository;
    private final ClassAssignmentRepository assignmentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final ClientRepository clientRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final OtpService otpService;
    private final EscrowService escrowService;
    private final ApplicationEventPublisher eventPublisher;

    // ===================== REVIEW =====================

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        if (request.getAssignmentId() == null || request.getRevieweeId() == null || request.getRating() == null) {
            throw new IllegalArgumentException("assignmentId, revieweeId và rating là bắt buộc");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating phải từ 1 đến 5");
        }
        User reviewer = userRepository.findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        User reviewee = userRepository.findById(request.getRevieweeId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người được đánh giá"));
        ClassAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        com.tcs.module.contract.entity.Review review = new com.tcs.module.contract.entity.Review();
        review.setAssignment(assignment);
        review.setTutoringClass(assignment.getApplication().getTutoringClass());
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setReviewType(request.getReviewType());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return toReviewResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForTutor(Long tutorUserId) {
        return reviewRepository.findByReviewee_UserId(tutorUserId).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    // ===================== CONTRACT GENERATION =====================

    @Override
    @Transactional
    public Contract generateForAssignment(Long assignmentId) {
        ClassAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công lớp"));

        if (contractRepository.findByAssignment_AssignmentId(assignmentId).isPresent()) {
            throw new IllegalArgumentException("Hợp đồng đã tồn tại cho phân công này");
        }

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

        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
        contract.setAssignment(assignment);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setTermsSummary(buildTermsSummary(assignment));
        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract generateForEnrollment(Long classStudentId) {
        ClassStudent cs = classStudentRepository.findById(classStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ghi danh"));

        if (contractRepository.findByClassStudent_ClassStudentId(classStudentId).isPresent()) {
            throw new IllegalArgumentException("Hợp đồng đã tồn tại cho ghi danh này");
        }

        Long currentUserId = authHelper.currentUserId();
        TutoringClass cls = cs.getTutoringClass();
        boolean isCenter = cls != null && cls.getCreator() != null
                && cls.getCreator().getUserId().equals(currentUserId);
        boolean isEnroller = cs.getEnrolledByUser() != null
                && cs.getEnrolledByUser().getUserId().equals(currentUserId);
        if (!isCenter && !isEnroller) {
            throw new ForbiddenException("Bạn không có quyền tạo hợp đồng cho ghi danh này");
        }

        Contract contract = new Contract();
        contract.setContractNo(generateContractNo());
        contract.setClassStudent(cs);
        contract.setStatus(ContractStatus.DRAFT);
        contract.setTermsSummary(buildCenterTermsSummary(cs));
        return contractRepository.save(contract);
    }

    // ===================== SIGNING =====================

    @Override
    @Transactional
    public void sign(Long contractId, String otp, Long signerUserId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));

        if (signatureRepository.existsByContract_ContractIdAndSigner_UserId(contractId, signerUserId)) {
            throw new IllegalArgumentException("Bạn đã ký hợp đồng này rồi");
        }

        if (!otpService.verifyOtp(contractId, signerUserId, otp)) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        User signer = userRepository.findById(signerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        ContractSignature signature = new ContractSignature();
        signature.setContract(contract);
        signature.setSigner(signer);
        signature.setSignedAt(LocalDateTime.now());
        signature.setSignatureData("OTP_VERIFIED:" + signer.getEmail() + ":"
                + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        signatureRepository.save(signature);

        if (isFullySigned(contractId)) {
            contract.setStatus(ContractStatus.SIGNED);
            contract.setSignedAt(LocalDateTime.now());
            contractRepository.save(contract);
            publishContractSigned(contract);
        }
    }

    private void publishContractSigned(Contract contract) {
        Long classId = null;
        Long payerUserId = null;
        Long beneficiaryUserId = null;
        BigDecimal amount = BigDecimal.ZERO;
        TutoringClass cls = null;
        Long assignmentId = null;
        Long classStudentId = null;

        if (contract.getAssignment() != null) {
            assignmentId = contract.getAssignment().getAssignmentId();
            TutorApplication app = contract.getAssignment().getApplication();
            if (app != null) {
                cls = app.getTutoringClass();
                if (cls != null) {
                    classId = cls.getClassId();
                    amount = cls.getTuitionFee() != null ? cls.getTuitionFee() : BigDecimal.ZERO;
                    payerUserId = cls.getCreator() != null ? cls.getCreator().getUserId() : null;
                }
            }
            beneficiaryUserId = contract.getAssignment().getTutor() != null
                    && contract.getAssignment().getTutor().getUser() != null
                    ? contract.getAssignment().getTutor().getUser().getUserId() : null;
        } else if (contract.getClassStudent() != null) {
            classStudentId = contract.getClassStudent().getClassStudentId();
            cls = contract.getClassStudent().getTutoringClass();
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
            try {
                EscrowLockCommand cmd = new EscrowLockCommand(
                        payerUserId, amount, assignmentId, classStudentId);
                escrowService.lock(cmd);
            } catch (Exception ex) {
                log.error("[Contract] Loi khi lock escrow cho contract={}: {}",
                        contract.getContractId(), ex.getMessage(), ex);
                throw ex;
            }
        } else {
            log.warn("[Contract] Skip lock escrow: payer={}, amount={}, assignmentId={}, classStudentId={}",
                    payerUserId, amount, assignmentId, classStudentId);
        }

        ContractSigned event = new ContractSigned(
                contract.getContractId(),
                classId,
                payerUserId,
                beneficiaryUserId,
                amount,
                assignmentId,
                classStudentId);
        eventPublisher.publishEvent(event);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFullySigned(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
        return signatureRepository.findByContract_ContractId(contractId).size() >= getRequiredSignersCount(contract);
    }

    // ===================== UC-44 API METHODS =====================

    @Transactional(readOnly = true)
    public ContractResponse getMyContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
        validateAccess(contract, authHelper.currentUserId());
        return toContractResponse(contract, authHelper.currentUserId());
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts() {
        Long userId = authHelper.currentUserId();
        var asTutor = contractRepository.findByAssignment_Tutor_UserId(userId);
        var asClassCreator = contractRepository.findByAssignment_ClassCreator_UserId(userId);
        var asClassStudent = contractRepository.findByClassStudent_UserId(userId);

        var allContracts = new java.util.LinkedHashSet<Contract>();
        allContracts.addAll(asTutor);
        allContracts.addAll(asClassCreator);
        allContracts.addAll(asClassStudent);

        return allContracts.stream()
                .map(c -> toContractResponse(c, userId))
                .toList();
    }

    @Transactional
    public OtpSentResponse sendSignOtp(Long contractId) {
        Long currentUserId = authHelper.currentUserId();
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
        validateAccess(contract, currentUserId);

        if (signatureRepository.existsByContract_ContractIdAndSigner_UserId(contractId, currentUserId)) {
            throw new IllegalArgumentException("Bạn đã ký hợp đồng này rồi");
        }

        String maskedEmail = otpService.generateAndSendOtp(contractId, currentUserId, contract.getContractNo());
        return OtpSentResponse.builder()
                .maskedEmail(maskedEmail)
                .message("Mã OTP đã được gửi tới email của bạn")
                .build();
    }

    @Transactional
    public ContractResponse signContract(Long contractId, SignContractRequest request) {
        Long currentUserId = authHelper.currentUserId();
        sign(contractId, request.getOtpCode(), currentUserId);
        return getMyContract(contractId);
    }

    @Transactional(readOnly = true)
    public SignatureStatusResponse getSignatureStatus(Long contractId) {
        Long currentUserId = authHelper.currentUserId();
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));
        validateAccess(contract, currentUserId);

        List<ContractSignature> signatures = signatureRepository.findByContract_ContractId(contractId);
        int required = getRequiredSignersCount(contract);

        List<SignatureStatusResponse.SignatureInfo> sigInfos = signatures.stream()
                .map(s -> SignatureStatusResponse.SignatureInfo.builder()
                        .signatureId(s.getSignatureId())
                        .signerUserId(s.getSigner().getUserId())
                        .signerName(getSignerName(s.getSigner()))
                        .signerRole(resolveSignerRole(s.getSigner().getUserId(), contract))
                        .signedAt(s.getSignedAt())
                        .isCurrentUser(s.getSigner().getUserId().equals(currentUserId))
                        .build())
                .toList();

        return SignatureStatusResponse.builder()
                .contractId(contractId)
                .contractNo(contract.getContractNo())
                .fullySigned(isFullySigned(contractId))
                .signedCount(signatures.size())
                .totalRequired(required)
                .signatures(sigInfos)
                .build();
    }

    // ===================== PRIVATE HELPERS =====================

    private void validateAccess(Contract contract, Long userId) {
        boolean hasAccess = false;

        if (contract.getAssignment() != null) {
            ClassAssignment a = contract.getAssignment();
            if (a.getTutor().getUser().getUserId().equals(userId)) hasAccess = true;
            if (a.getApplication() != null
                    && a.getApplication().getTutoringClass().getCreator().getUserId().equals(userId)) hasAccess = true;
        }

        if (contract.getClassStudent() != null) {
            ClassStudent cs = contract.getClassStudent();
            if (cs.getTutoringClass().getCreator().getUserId().equals(userId)) hasAccess = true;
            if (cs.getEnrolledByUser() != null
                    && cs.getEnrolledByUser().getUserId().equals(userId)) hasAccess = true;
        }

        if (!hasAccess) {
            throw new ForbiddenException("Bạn không có quyền truy cập hợp đồng này");
        }
    }

    private int getRequiredSignersCount(Contract contract) {
        return 2; // tutor + client (PRIVATE) or center + client (CENTER)
    }

    private String generateContractNo() {
        return "HD-" + System.currentTimeMillis();
    }

    private String buildTermsSummary(ClassAssignment assignment) {
        TutorApplication app = assignment.getApplication();
        if (app == null) {
            return "Hợp đồng gia sư - Lớp ID: " + assignment.getAssignmentId();
        }
        TutoringClass cls = app.getTutoringClass();
        return String.format("Hợp đồng gia sư: %s - Môn %s, %d buổi, phí %s VNĐ",
                cls.getTitle(),
                cls.getSubject() != null ? cls.getSubject().getSubjectName() : "N/A",
                cls.getNumberOfSessions(),
                cls.getTuitionFee());
    }

    private String buildCenterTermsSummary(ClassStudent cs) {
        TutoringClass cls = cs.getTutoringClass();
        return String.format("Hợp đồng trung tâm: %s - %s, %d buổi, phí %s VNĐ",
                cls.getTitle(),
                cs.getStudentName(),
                cls.getNumberOfSessions(),
                cls.getTuitionFee());
    }

    private String resolveSignerRole(Long signerUserId, Contract contract) {
        if (contract.getAssignment() != null) {
            if (contract.getAssignment().getTutor().getUser().getUserId().equals(signerUserId)) {
                return "Gia sư";
            }
            return "Phụ huynh";
        }
        if (contract.getClassStudent() != null) {
            if (contract.getClassStudent().getTutoringClass().getCreator().getUserId().equals(signerUserId)) {
                return "Trung tâm";
            }
            return "Phụ huynh";
        }
        return "Bên ký";
    }

    private String getSignerName(User signer) {
        return tutorRepository.findByUser_UserId(signer.getUserId())
                .map(Tutor::getFullName)
                .orElseGet(() -> clientRepository.findByUser_UserId(signer.getUserId())
                        .map(Client::getFullName)
                        .orElseGet(() -> tutorCenterRepository.findByUser_UserId(signer.getUserId())
                                .map(TutorCenter::getCompanyName)
                                .orElse(signer.getEmail())));
    }

    private String getCreatorFullName(User user) {
        return tutorRepository.findByUser_UserId(user.getUserId())
                .map(Tutor::getFullName)
                .orElseGet(() -> clientRepository.findByUser_UserId(user.getUserId())
                        .map(Client::getFullName)
                        .orElseGet(() -> tutorCenterRepository.findByUser_UserId(user.getUserId())
                                .map(TutorCenter::getCompanyName)
                                .orElse(user.getEmail())));
    }

    private ContractResponse toContractResponse(Contract contract, Long currentUserId) {
        ContractResponse.PartyInfo tutorInfo = null;
        ContractResponse.PartyInfo clientInfo = null;
        ContractResponse.PartyInfo centerInfo = null;
        Long classId = null;
        String classTitle = null;
        String classType = null;
        java.math.BigDecimal tuitionFee = null;
        String lessonMode = null;
        Integer numberOfSessions = null;

        if (contract.getAssignment() != null) {
            Tutor tutor = contract.getAssignment().getTutor();
            tutorInfo = ContractResponse.PartyInfo.builder()
                    .userId(tutor.getUser().getUserId())
                    .fullName(tutor.getFullName())
                    .email(tutor.getUser().getEmail())
                    .phone(tutor.getPhone())
                    .build();

            User creator = null;
            TutoringClass cls = null;
            if (contract.getAssignment().getApplication() != null) {
                cls = contract.getAssignment().getApplication().getTutoringClass();
                creator = cls.getCreator();
            } else {
                creator = tutor.getUser();
                cls = null;
            }

            if (creator != null) {
                clientInfo = ContractResponse.PartyInfo.builder()
                        .userId(creator.getUserId())
                        .fullName(getCreatorFullName(creator))
                        .email(creator.getEmail())
                        .build();
            }

            if (cls != null) {
                classId = cls.getClassId();
                classTitle = cls.getTitle();
                classType = cls.getClassType().name();
                tuitionFee = cls.getTuitionFee();
                lessonMode = cls.getLessonMode().name();
                numberOfSessions = cls.getNumberOfSessions();
            }
        } else if (contract.getClassStudent() != null) {
            TutoringClass cls = contract.getClassStudent().getTutoringClass();
            centerInfo = ContractResponse.PartyInfo.builder()
                    .userId(cls.getCreator().getUserId())
                    .fullName(getCreatorFullName(cls.getCreator()))
                    .email(cls.getCreator().getEmail())
                    .build();

            if (contract.getClassStudent().getEnrolledByUser() != null) {
                User enroller = contract.getClassStudent().getEnrolledByUser();
                clientInfo = ContractResponse.PartyInfo.builder()
                        .userId(enroller.getUserId())
                        .fullName(getCreatorFullName(enroller))
                        .email(enroller.getEmail())
                        .build();
            }

            classId = cls.getClassId();
            classTitle = cls.getTitle();
            classType = cls.getClassType().name();
            tuitionFee = cls.getTuitionFee();
            lessonMode = cls.getLessonMode().name();
            numberOfSessions = cls.getNumberOfSessions();
        }

        return ContractResponse.builder()
                .contractId(contract.getContractId())
                .contractNo(contract.getContractNo())
                .status(contract.getStatus())
                .termsSummary(contract.getTermsSummary())
                .contractFileUrl(contract.getContractFileUrl())
                .signedAt(contract.getSignedAt())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .classId(classId)
                .classTitle(classTitle)
                .classType(classType)
                .tuitionFee(tuitionFee)
                .lessonMode(lessonMode)
                .numberOfSessions(numberOfSessions)
                .tutor(tutorInfo)
                .client(clientInfo)
                .center(centerInfo)
                .build();
    }

    private ReviewResponse toReviewResponse(com.tcs.module.contract.entity.Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .assignmentId(review.getAssignment().getAssignmentId())
                .reviewerId(review.getReviewer().getUserId())
                .revieweeId(review.getReviewee().getUserId())
                .reviewType(review.getReviewType())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
