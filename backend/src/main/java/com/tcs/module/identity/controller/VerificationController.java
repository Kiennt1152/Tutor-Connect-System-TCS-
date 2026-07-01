package com.tcs.module.identity.controller;

import com.tcs.module.identity.dto.request.VerificationDecisionDto;
import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.dto.response.FileUploadResponse;
import com.tcs.module.identity.dto.response.VerificationResponse;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.service.FileStorageService;
import com.tcs.module.identity.service.VerificationService;
import com.tcs.security.AuthHelper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/identity/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final FileStorageService fileStorageService;
    private final AuthHelper authHelper;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = authHelper.currentUserId();
        return ResponseEntity.ok(fileStorageService.uploadFile(file, userId));
    }

    @PostMapping("/submit")
    public ResponseEntity<VerificationResponse> submitVerification(
            @Valid @RequestBody VerificationRequestDto request
    ) {
        return ResponseEntity.ok(verificationService.submitVerification(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<VerificationResponse>> getMyVerifications() {
        return ResponseEntity.ok(verificationService.getMyVerifications());
    }

    @GetMapping("/{verificationId}")
    public ResponseEntity<VerificationResponse> getVerificationById(@PathVariable Long verificationId) {
        return ResponseEntity.ok(verificationService.getVerificationById(verificationId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VerificationResponse>> getVerificationsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(verificationService.getVerificationsByUser(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<VerificationResponse>> getVerificationsByStatus(
            @PathVariable VerificationStatus status
    ) {
        return ResponseEntity.ok(verificationService.getVerificationsByStatus(status));
    }

    @PostMapping("/{verificationId}/start-review")
    public ResponseEntity<VerificationResponse> startReview(@PathVariable Long verificationId) {
        return ResponseEntity.ok(verificationService.startReview(verificationId));
    }

    @PostMapping("/{verificationId}/review")
    public ResponseEntity<VerificationResponse> reviewVerification(
            @PathVariable Long verificationId,
            @Valid @RequestBody VerificationDecisionDto decision
    ) {
        return ResponseEntity.ok(verificationService.reviewVerification(verificationId, decision));
    }

    @GetMapping("/moderation-queue")
    public ResponseEntity<List<VerificationResponse>> getModerationQueue() {
        return ResponseEntity.ok(verificationService.getModerationQueue());
    }
}