package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.request.AppealDisputeRequest;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.request.ResolveDisputeRequest;
import com.tcs.module.finance.dto.request.SubmitDisputeEvidenceRequest;
import com.tcs.module.finance.dto.response.AdminDisputeReviewResponse;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.dto.response.ParticipantDisputeResponse;
import com.tcs.module.finance.dto.request.WithdrawDisputeRequest;
import com.tcs.module.finance.enums.DisputeStatus;
import java.util.List;

public interface DisputeService {

    DisputeResponse createDispute(CreateDisputeRequest request);

    DisputeResponse createClassIssue(CreateClassIssueRequest request);

    List<AdminDisputeReviewResponse> listDisputesForAdmin(DisputeStatus status);

    AdminDisputeReviewResponse getDisputeForAdmin(Long disputeId);

    AdminDisputeReviewResponse resolveDispute(Long disputeId, ResolveDisputeRequest request);

    DisputeResponse submitAdditionalEvidence(Long disputeId, SubmitDisputeEvidenceRequest request);

    List<ParticipantDisputeResponse> listMyDisputes(Long classId);

    ParticipantDisputeResponse submitExplanation(Long disputeId, SubmitDisputeEvidenceRequest request);

    ParticipantDisputeResponse withdrawDispute(Long disputeId, WithdrawDisputeRequest request);

    boolean canReadDisputeEvidence(String fileUrl, Long userId);

    AdminDisputeReviewResponse appealDispute(Long disputeId, AppealDisputeRequest request);
}
