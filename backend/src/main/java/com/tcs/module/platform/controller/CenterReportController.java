package com.tcs.module.platform.controller;

import com.tcs.module.platform.dto.request.ResolveClassIssueRequest;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.service.PlatformService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/center/reports")
@RequiredArgsConstructor
public class CenterReportController {

    private final PlatformService platformService;

    @GetMapping
    public List<ReportResponse> listReports() {
        return platformService.listCenterReports();
    }

    @PatchMapping("/{reportId}/resolve")
    public ReportResponse resolveClassIssue(
            @PathVariable Long reportId,
            @RequestBody ResolveClassIssueRequest request) {
        return platformService.resolveCenterClassIssue(reportId, request);
    }
}
