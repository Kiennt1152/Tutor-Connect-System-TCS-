package com.tcs.module.finance.controller;

import com.tcs.module.finance.dto.request.AppealDisputeRequest;
import com.tcs.module.finance.dto.request.CreateClassIssueRequest;
import com.tcs.module.finance.dto.request.CreateDisputeRequest;
import com.tcs.module.finance.dto.request.ResolveDisputeRequest;
import com.tcs.module.finance.dto.request.SubmitDisputeEvidenceRequest;
import com.tcs.module.finance.dto.response.AdminDisputeReviewResponse;
import com.tcs.module.finance.dto.response.DisputeResponse;
import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.service.DisputeService;
import com.tcs.module.identity.dto.response.FileUploadResponse;
import com.tcs.module.identity.service.FileStorageService;
import com.tcs.security.AuthHelper;
import com.tcs.util.FileMagicDetector;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class DisputeController {

    private static final Set<String> EVIDENCE_IMAGE_TYPES = Set.of(
            FileMagicDetector.MIME_JPEG,
            FileMagicDetector.MIME_PNG,
            FileMagicDetector.MIME_WEBP
    );

    private final DisputeService disputeService;
    private final FileStorageService fileStorageService;
    private final AuthHelper authHelper;

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

    @PostMapping("/api/disputes/{disputeId}/evidence")
    public DisputeResponse submitAdditionalEvidence(
            @PathVariable Long disputeId,
            @RequestBody SubmitDisputeEvidenceRequest request) {
        return disputeService.submitAdditionalEvidence(disputeId, request);
    }

    @PostMapping(value = "/api/disputes/evidence/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse uploadEvidenceImage(@RequestParam("file") MultipartFile file) {
        validateEvidenceImage(file);
        return fileStorageService.uploadFile(file, authHelper.currentUserId());
    }

    @PostMapping("/api/disputes/{disputeId}/appeal")
    public AdminDisputeReviewResponse appealDispute(
            @PathVariable Long disputeId,
            @RequestBody AppealDisputeRequest request) {
        return disputeService.appealDispute(disputeId, request);
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

    private void validateEvidenceImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh bằng chứng không được để trống");
        }

        try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
            String detectedMime = FileMagicDetector.detect(bis);
            if (!EVIDENCE_IMAGE_TYPES.contains(detectedMime)) {
                throw new IllegalArgumentException("Bằng chứng chỉ chấp nhận ảnh JPG, PNG hoặc WEBP");
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc ảnh bằng chứng", e);
        }
    }
}
