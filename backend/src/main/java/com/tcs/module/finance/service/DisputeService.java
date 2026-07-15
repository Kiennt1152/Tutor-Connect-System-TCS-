package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.response.DisputeResponse;

public interface DisputeService {

    DisputeResponse createDispute(CreateDisputeRequest request);

    DisputeResponse createClassIssue(CreateClassIssueRequest request);
}
