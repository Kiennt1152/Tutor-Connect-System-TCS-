package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.dto.response.PagePenaltyResponse;
import com.tcs.module.platform.dto.response.PenaltyResponse;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.service.PenaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/penalties")
@RequiredArgsConstructor
public class PlatformPenaltyController {

    private final PenaltyService penaltyService;

    @GetMapping
    public PagePenaltyResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserPenaltyStatus status,
            @RequestParam(required = false) UserPenaltyType type,
            @RequestParam(required = false) Long userId) {
        return penaltyService.listPenalties(userId, status, type, page, size);
    }

    @PostMapping
    public PenaltyResponse issue(@Valid @RequestBody IssuePenaltyRequest request) {
        return penaltyService.issuePenalty(request);
    }

    @PatchMapping("/{penaltyId}/revoke")
    public PenaltyResponse revoke(
            @PathVariable Long penaltyId, @Valid @RequestBody RevokePenaltyRequest request) {
        return penaltyService.revokePenalty(penaltyId, request);
    }
}
