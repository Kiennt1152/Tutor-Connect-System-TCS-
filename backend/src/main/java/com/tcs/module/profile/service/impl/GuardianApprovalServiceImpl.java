package com.tcs.module.profile.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.entity.PaymentTransaction;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.enums.PaymentTransactionStatus;
import com.tcs.module.finance.repository.PaymentTransactionRepository;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.profile.dto.response.GuardianApprovalResponse;
import com.tcs.module.profile.enums.GuardianApprovalActionType;
import com.tcs.module.profile.enums.GuardianApprovalStatus;
import com.tcs.module.profile.service.ClientLegalAccountService.LegalAccountContext;
import com.tcs.module.profile.service.GuardianApprovalService;
import com.tcs.module.profile.support.GuardianApprovalPayloadCodec;
import com.tcs.module.profile.support.GuardianApprovalPayloadCodec.Payload;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GuardianApprovalServiceImpl implements GuardianApprovalService {

    private final AuthHelper authHelper;
    private final NotificationRepository notificationRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final GuardianApprovalPayloadCodec payloadCodec;

    @Override
    @Transactional
    public GuardianApprovalResponse submitDepositApproval(
            LegalAccountContext legalContext, PaymentTransaction transaction) {
        Payload base = basePayload(legalContext);
        Payload payload = Payload.builder()
                .minorUserId(base.getMinorUserId())
                .minorName(base.getMinorName())
                .parentUserId(base.getParentUserId())
                .parentName(base.getParentName())
                .actionType(GuardianApprovalActionType.DEPOSIT)
                .status(GuardianApprovalStatus.PENDING)
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .paymentTransactionId(transaction.getTransactionId())
                .build();
        return createApprovalNotifications(payload, legalContext, transaction.getTransactionId());
    }

    @Override
    @Transactional
    public GuardianApprovalResponse submitContractApproval(
            LegalAccountContext legalContext, String tutorName, String subjectName, String contractReference) {
        Payload base = basePayload(legalContext);
        Payload payload = Payload.builder()
                .minorUserId(base.getMinorUserId())
                .minorName(base.getMinorName())
                .parentUserId(base.getParentUserId())
                .parentName(base.getParentName())
                .actionType(GuardianApprovalActionType.CONTRACT_SIGN)
                .status(GuardianApprovalStatus.PENDING)
                .tutorName(tutorName)
                .subjectName(subjectName)
                .contractReference(contractReference)
                .description(buildContractDescription(tutorName, subjectName, legalContext))
                .build();
        return createApprovalNotifications(payload, legalContext, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuardianApprovalResponse> getPendingApprovalsForParent() {
        return notificationRepository
                .findByUser_UserIdAndReferenceTypeOrderByCreatedAtDesc(
                        authHelper.currentUserId(), GuardianApprovalPayloadCodec.REF_PARENT)
                .stream()
                .map(this::safeToResponse)
                .filter(Objects::nonNull)
                .filter(response -> response.getStatus() == GuardianApprovalStatus.PENDING)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuardianApprovalResponse> getMySubmittedApprovals() {
        return notificationRepository
                .findByUser_UserIdAndReferenceTypeOrderByCreatedAtDesc(
                        authHelper.currentUserId(), GuardianApprovalPayloadCodec.REF_MINOR)
                .stream()
                .map(this::safeToResponse)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public GuardianApprovalResponse approve(Long approvalId) {
        return resolve(approvalId, true);
    }

    @Override
    @Transactional
    public GuardianApprovalResponse reject(Long approvalId) {
        return resolve(approvalId, false);
    }

    private GuardianApprovalResponse createApprovalNotifications(
            Payload payload, LegalAccountContext legalContext, Long paymentTransactionId) {
        User parentUser = requireUser(legalContext.getLegalUserId());
        User minorUser = requireUser(legalContext.getSessionUserId());

        Notification parentNotification = new Notification();
        parentNotification.setUser(parentUser);
        parentNotification.setType(NotificationType.PAYMENT);
        parentNotification.setTitle(buildTitle(payload));
        parentNotification.setContent(payloadCodec.encode(payload));
        parentNotification.setReferenceType(GuardianApprovalPayloadCodec.REF_PARENT);
        parentNotification.setReferenceId(paymentTransactionId);
        parentNotification.setStatus(NotificationStatus.SENT);
        parentNotification.setIsRead(false);
        Notification savedParent = notificationRepository.save(parentNotification);

        payload.setParentNotificationId(savedParent.getNotificationId());
        savedParent.setContent(payloadCodec.encode(payload));
        notificationRepository.save(savedParent);

        Notification minorNotification = new Notification();
        minorNotification.setUser(minorUser);
        minorNotification.setType(NotificationType.PAYMENT);
        minorNotification.setTitle("Đã gửi yêu cầu chờ phụ huynh xác nhận");
        minorNotification.setContent(payloadCodec.encode(payload));
        minorNotification.setReferenceType(GuardianApprovalPayloadCodec.REF_MINOR);
        minorNotification.setReferenceId(savedParent.getNotificationId());
        minorNotification.setStatus(NotificationStatus.SENT);
        minorNotification.setIsRead(false);
        notificationRepository.save(minorNotification);

        String emailSubject = "[TutorConnect] Yêu cầu xác nhận từ " + payload.getMinorName();
        String emailBody = buildTitle(payload)
                + "\n\nVui lòng đăng nhập tài khoản phụ huynh và mở trang Xác nhận phụ huynh để phê duyệt.";
        notificationDispatchService.notifyUserByEmail(parentUser, emailSubject, emailBody);

        return toResponse(savedParent, payload);
    }

    private GuardianApprovalResponse resolve(Long approvalId, boolean approved) {
        Notification parentNotification = notificationRepository
                .findByNotificationIdAndUser_UserId(approvalId, authHelper.currentUserId())
                .orElseThrow(() -> new ForbiddenException("Không tìm thấy yêu cầu chờ xác nhận hoặc bạn không có quyền"));

        Payload payload = payloadCodec.decode(parentNotification.getContent());
        if (payload.getStatus() != GuardianApprovalStatus.PENDING) {
            throw new IllegalArgumentException("Yêu cầu đã được xử lý trước đó");
        }

        if (payload.getActionType() == GuardianApprovalActionType.DEPOSIT) {
            finalizeDeposit(payload, approved);
        }

        payload.setStatus(approved ? GuardianApprovalStatus.APPROVED : GuardianApprovalStatus.REJECTED);
        payload.setResolvedAt(LocalDateTime.now());
        String encoded = payloadCodec.encode(payload);
        parentNotification.setContent(encoded);
        parentNotification.setIsRead(true);
        parentNotification.setReadAt(LocalDateTime.now());
        notificationRepository.save(parentNotification);

        syncMinorNotification(payload, encoded);
        notifyMinorResolved(payload, approved);
        return toResponse(parentNotification, payload);
    }

    private void syncMinorNotification(Payload payload, String encoded) {
        if (payload.getParentNotificationId() == null) {
            return;
        }
        notificationRepository
                .findByUser_UserIdAndReferenceTypeOrderByCreatedAtDesc(
                        payload.getMinorUserId(), GuardianApprovalPayloadCodec.REF_MINOR)
                .stream()
                .filter(notification -> payload.getParentNotificationId().equals(notification.getReferenceId()))
                .findFirst()
                .ifPresent(minorNotification -> {
                    minorNotification.setContent(encoded);
                    minorNotification.setTitle(
                            payload.getStatus() == GuardianApprovalStatus.APPROVED
                                    ? "Phụ huynh đã xác nhận yêu cầu"
                                    : "Phụ huynh đã từ chối yêu cầu");
                    notificationRepository.save(minorNotification);
                });
    }

    private void finalizeDeposit(Payload payload, boolean approved) {
        if (payload.getPaymentTransactionId() == null) {
            throw new ResourceNotFoundException("Không tìm thấy giao dịch liên kết");
        }
        PaymentTransaction transaction = paymentTransactionRepository
                .findById(payload.getPaymentTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch liên kết"));
        if (transaction.getStatus() != PaymentTransactionStatus.PENDING) {
            throw new IllegalArgumentException("Giao dịch đã được xử lý trước đó");
        }
        if (approved) {
            Wallet wallet = transaction.getWallet();
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(transaction.getAmount()));
            walletRepository.save(wallet);
            transaction.setStatus(PaymentTransactionStatus.SUCCESS);
            transaction.setProcessedAt(LocalDateTime.now());
        } else {
            transaction.setStatus(PaymentTransactionStatus.CANCELLED);
            transaction.setFailureReason("Phụ huynh từ chối xác nhận");
            transaction.setProcessedAt(LocalDateTime.now());
        }
        paymentTransactionRepository.save(transaction);
    }

    private void notifyMinorResolved(Payload payload, boolean approved) {
        User minorUser = requireUser(payload.getMinorUserId());
        String actionLabel =
                payload.getActionType() == GuardianApprovalActionType.DEPOSIT ? "Nạp tiền" : "Ký hợp đồng";
        String title = approved ? actionLabel + " đã được phụ huynh xác nhận" : actionLabel + " bị phụ huynh từ chối";
        String content = approved
                ? actionLabel + " (" + describePayload(payload) + ") đã có hiệu lực."
                : "Phụ huynh đã từ chối yêu cầu " + describePayload(payload) + ".";
        notificationDispatchService.notifyUser(
                minorUser, NotificationType.PAYMENT, title, content, GuardianApprovalPayloadCodec.REF_MINOR, payload.getParentNotificationId());
    }

    private Payload basePayload(LegalAccountContext legalContext) {
        return Payload.builder()
                .minorUserId(legalContext.getSessionUserId())
                .minorName(legalContext.getBeneficiaryMinorName())
                .parentUserId(legalContext.getLegalUserId())
                .parentName(legalContext.getLegalHolderName())
                .build();
    }

    private String buildTitle(Payload payload) {
        String actionLabel =
                payload.getActionType() == GuardianApprovalActionType.DEPOSIT ? "nạp tiền" : "ký hợp đồng";
        return "Yêu cầu xác nhận " + actionLabel + " từ " + payload.getMinorName();
    }

    private String describePayload(Payload payload) {
        if (payload.getActionType() == GuardianApprovalActionType.DEPOSIT) {
            return "nạp " + payload.getAmount() + " VND";
        }
        String base = "hợp đồng với gia sư " + payload.getTutorName();
        if (StringUtils.hasText(payload.getSubjectName())) {
            base += " (" + payload.getSubjectName() + ")";
        }
        return base;
    }

    private String buildContractDescription(String tutorName, String subjectName, LegalAccountContext legalContext) {
        String base = "Ký hợp đồng với gia sư " + tutorName;
        if (StringUtils.hasText(subjectName)) {
            base += " - " + subjectName;
        }
        if (StringUtils.hasText(legalContext.getBeneficiaryMinorName())) {
            base += " (hộ " + legalContext.getBeneficiaryMinorName() + ")";
        }
        return base;
    }

    private User requireUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private GuardianApprovalResponse safeToResponse(Notification notification) {
        try {
            return toResponse(notification, payloadCodec.decode(notification.getContent()));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private GuardianApprovalResponse toResponse(Notification notification, Payload payload) {
        return GuardianApprovalResponse.builder()
                .approvalId(notification.getNotificationId())
                .actionType(payload.getActionType())
                .status(payload.getStatus())
                .amount(payload.getAmount())
                .description(payload.getDescription())
                .tutorName(payload.getTutorName())
                .subjectName(payload.getSubjectName())
                .contractReference(payload.getContractReference())
                .paymentTransactionId(payload.getPaymentTransactionId())
                .minorUserId(payload.getMinorUserId())
                .minorName(payload.getMinorName())
                .parentUserId(payload.getParentUserId())
                .parentName(payload.getParentName())
                .createdAt(notification.getCreatedAt())
                .resolvedAt(payload.getResolvedAt())
                .build();
    }
}
