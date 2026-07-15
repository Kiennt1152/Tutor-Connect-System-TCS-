package com.tcs.module.finance.controller;

import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/api/disputes")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeResponse createDispute(@RequestBody CreateDisputeRequest request) {
        return disputeService.createDispute(request);
    }

    @PostMapping("/api/class-issues")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeResponse createClassIssue(@RequestBody CreateClassIssueRequest request) {
        return disputeService.createClassIssue(request);
    }
}
