package com.tcs.module.identity.service;

import com.tcs.module.identity.dto.request.VerificationDecisionDto;
import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import java.util.List;

public interface VerificationService {

    VerificationResponse submitVerification(Long userId, VerificationRequestDto request);

    VerificationResponse getVerificationById(Long verificationId);

    List<VerificationResponse> getVerificationsByUser(Long userId);

    List<VerificationResponse> getVerificationsByStatus(VerificationStatus status);

    VerificationResponse reviewVerification(Long verificationId, Long adminId, VerificationDecisionDto decision);

    List<VerificationResponse> getModerationQueue();

    boolean canResubmit(Long userId, VerificationType verificationType);
}
