package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.request.ReviewCircumventionRequest;
import com.tcs.module.platform.dto.response.CircumventionEventResponse;
import com.tcs.module.platform.dto.response.CircumventionConversationResponse;
import com.tcs.module.platform.dto.response.PageCircumventionEventResponse;
import com.tcs.module.platform.service.CircumventionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/circumvention-events")
@RequiredArgsConstructor
public class CircumventionController {
    private final CircumventionService service;

    @GetMapping
    public PageCircumventionEventResponse list(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    @PatchMapping("/{eventId}")
    public CircumventionEventResponse review(@PathVariable Long eventId,
            @Valid @RequestBody ReviewCircumventionRequest request) {
        return service.review(eventId, request);
    }

    @GetMapping("/{eventId}/conversation")
    public CircumventionConversationResponse getConversationEvidence(@PathVariable Long eventId) {
        return service.getConversationEvidence(eventId);
    }
}
