package com.tcs.module.platform.service;

import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.dto.response.PagePenaltyResponse;
import com.tcs.module.platform.dto.response.PenaltyResponse;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;

public interface PenaltyService {
    PagePenaltyResponse listPenalties(Long userId, UserPenaltyStatus status, UserPenaltyType type, int page, int size);
    PenaltyResponse issuePenalty(IssuePenaltyRequest request);
    PenaltyResponse revokePenalty(Long penaltyId, RevokePenaltyRequest request);
}
