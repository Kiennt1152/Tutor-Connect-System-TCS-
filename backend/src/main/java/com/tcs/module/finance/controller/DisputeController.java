package com.tcs.module.finance.controller;

import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.request.ResolveDisputeRequest;
import com.tcs.module.finance.dto.response.AdminDisputeReviewResponse;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.service.DisputeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @GetMapping("/api/disputes")
    public List<AdminDisputeReviewResponse> listDisputes(@RequestParam(required = false) DisputeStatus status) {
        return disputeService.listDisputesForAdmin(status);
    }

    @GetMapping("/api/disputes/{disputeId}")
    public AdminDisputeReviewResponse getDispute(@PathVariable Long disputeId) {
        return disputeService.getDisputeForAdmin(disputeId);
    }

    @PostMapping("/api/disputes/{disputeId}/resolve")
    public AdminDisputeReviewResponse resolveDispute(
            @PathVariable Long disputeId,
            @RequestBody ResolveDisputeRequest request) {
        return disputeService.resolveDispute(disputeId, request);
    }

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
