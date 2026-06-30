package com.tcs.module.identity.service.impl;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.dto.request.VerificationDecisionDto;
import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.mapper.VerificationMapper;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.VerificationService;
import com.tcs.module.profile.repository.MediaFileRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final VerificationMapper verificationMapper;

    @Override
    @Transactional
    public VerificationResponse submitVerification(Long userId, VerificationRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!canResubmit(userId, request.getVerificationType())) {
            throw new BusinessException("A verification request is already pending or approved for this type");
        }

        if (request.getVerificationType() != VerificationType.TUTOR_PROFILE) {
            throw new BusinessException("Only TUTOR_PROFILE verification is supported in this phase");
        }

        if (request.getDocuments() == null || request.getDocuments().isEmpty()) {
            throw new IllegalArgumentException("At least one document is required");
        }

        VerificationRequest verification = new VerificationRequest();
        verification.setUser(user);
        verification.setVerificationType(request.getVerificationType());
        verification.setStatus(VerificationStatus.SUBMITTED);
        verification.setSubmittedAt(LocalDateTime.now());

        VerificationRequest saved = verificationRequestRepository.save(verification);

        for (VerificationRequestDto.DocumentUpload docUpload : request.getDocuments()) {
            var fileOpt = mediaFileRepository.findById(docUpload.getFileId());
            if (fileOpt.isEmpty()) {
                throw new ResourceNotFoundException("File not found: " + docUpload.getFileId());
            }

            VerificationDocument doc = new VerificationDocument();
            doc.setVerificationRequest(saved);
            doc.setFile(fileOpt.get());
            doc.setDocumentType(docUpload.getDocumentType());
            verificationDocumentRepository.save(doc);
        }

        log.info("Verification submitted: userId={}, type={}, verificationId={}",
                userId, request.getVerificationType(), saved.getVerificationId());

        return getVerificationById(saved.getVerificationId());
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationResponse getVerificationById(Long verificationId) {
        VerificationRequest request = verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found: " + verificationId));

        List<VerificationDocument> docs = verificationDocumentRepository
                .findByVerificationRequest_VerificationId(verificationId);

        return verificationMapper.toResponse(request, docs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> getVerificationsByUser(Long userId) {
        return verificationRequestRepository.findByUser_UserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(v -> {
                    List<VerificationDocument> docs = verificationDocumentRepository
                            .findByVerificationRequest_VerificationId(v.getVerificationId());
                    return verificationMapper.toResponse(v, docs);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> getVerificationsByStatus(VerificationStatus status) {
        return verificationRequestRepository.findByStatusOrderBySubmittedAtAsc(status)
                .stream()
                .map(v -> {
                    List<VerificationDocument> docs = verificationDocumentRepository
                            .findByVerificationRequest_VerificationId(v.getVerificationId());
                    return verificationMapper.toResponse(v, docs);
                })
                .toList();
    }

    @Override
    @Transactional
    public VerificationResponse reviewVerification(Long verificationId, Long adminId, VerificationDecisionDto decision) {
        VerificationRequest verification = verificationRequestRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found: " + verificationId));

        if (verification.getStatus() != VerificationStatus.SUBMITTED) {
            throw new BusinessException("Verification is not in SUBMITTED status");
        }

        verification.setReviewedBy(adminId);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setAdminNotes(decision.getNote());

        if ("APPROVE".equalsIgnoreCase(decision.getDecision())) {
            verification.setStatus(VerificationStatus.VERIFIED);
        } else if ("REJECT".equalsIgnoreCase(decision.getDecision())) {
            verification.setStatus(VerificationStatus.REJECTED);
            verification.setRejectionReason(decision.getNote());
        } else {
            throw new IllegalArgumentException("Decision must be APPROVE or REJECT");
        }

        VerificationRequest saved = verificationRequestRepository.save(verification);

        log.info("Verification reviewed: verificationId={}, adminId={}, decision={}",
                verificationId, adminId, decision.getDecision());

        List<VerificationDocument> docs = verificationDocumentRepository
                .findByVerificationRequest_VerificationId(verificationId);

        return verificationMapper.toResponse(saved, docs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> getModerationQueue() {
        return getVerificationsByStatus(VerificationStatus.SUBMITTED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canResubmit(Long userId, VerificationType verificationType) {
        return !verificationRequestRepository.existsByUser_UserIdAndVerificationTypeAndStatusIn(
                userId,
                verificationType,
                List.of(VerificationStatus.SUBMITTED, VerificationStatus.UNDER_REVIEW, VerificationStatus.VERIFIED)
        );
    }
}
