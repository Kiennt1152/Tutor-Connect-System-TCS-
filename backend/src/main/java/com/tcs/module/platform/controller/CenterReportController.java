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

/**
 * ====================================================================================================
 * [UC-46] BÁO CÁO & ĐỐI SOÁT TÀI CHÍNH TRUNG TÂM (CENTER REPORT CONTROLLER)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Cung cấp API thống kê doanh thu lớp học và đối soát hoa hồng cho các trung tâm gia sư.
 * 2. Tính toán phí yêu cầu dịch vụ sàn (Center Request Fee) dựa trên thỏa thuận nhượng quyền.
 * 3. Xuất bảng kê chi tiết các khoản thanh toán phục vụ công tác kế toán định kỳ.
 * * @author Nguyễn Tiến Anh (tienanh6677)
 */
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
