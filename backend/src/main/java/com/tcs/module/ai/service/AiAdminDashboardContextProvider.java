package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.FinancialFlowResponse;
import com.tcs.module.platform.dto.response.RiskSummaryResponse;
import com.tcs.module.platform.service.PlatformService;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAdminDashboardContextProvider {

    private final PlatformService platformService;

    public List<AiSourceResponse> getDashboardContext(String userRole) {
        List<AiSourceResponse> results = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("#,###");

        if ("PLATFORM_ADMIN".equals(userRole)) {
            try {
                LocalDate to = LocalDate.now();
                LocalDate from = to.minusDays(7); // Last 7 days overview
                DashboardResponse dashboard = platformService.getDashboard(from, to, "DAY");
                
                FinancialFlowResponse flow = dashboard.getFinancialFlow();
                RiskSummaryResponse risk = dashboard.getRiskSummary();

                BigDecimal moneyIn = flow != null && flow.getMoneyIn() != null ? flow.getMoneyIn() : BigDecimal.ZERO;
                BigDecimal moneyOut = flow != null && flow.getMoneyOut() != null ? flow.getMoneyOut() : BigDecimal.ZERO;
                BigDecimal netMovement = flow != null && flow.getNetMovement() != null ? flow.getNetMovement() : BigDecimal.ZERO;
                BigDecimal escrowHeld = flow != null && flow.getEscrowHeld() != null ? flow.getEscrowHeld() : BigDecimal.ZERO;
                BigDecimal platformFee = flow != null && flow.getPlatformFeeRevenue() != null ? flow.getPlatformFeeRevenue() : BigDecimal.ZERO;

                long overdueTickets = risk != null ? risk.getOverdueTickets() : 0;
                long openDisputes = risk != null ? risk.getOpenDisputes() : 0;
                long pendingRefunds = risk != null ? risk.getPendingRefunds() : 0;
                long unhandledReports = risk != null ? risk.getUnhandledReports() : 0;
                BigDecimal escrowExposure = risk != null && risk.getEscrowExposure() != null ? risk.getEscrowExposure() : BigDecimal.ZERO;

                String summaryText = String.format(
                    "Báo cáo Tổng quan Điều hành & Doanh thu Nền tảng TCS (7 ngày gần nhất):\n" +
                    "1. Dòng tiền & Doanh thu sàn:\n" +
                    "   - Tổng tiền vào (IN): +%s ₫ (Nạp ví & Đặt cọc ký quỹ)\n" +
                    "   - Tổng tiền ra (OUT): -%s ₫ (Rút tiền & Hoàn tiền)\n" +
                    "   - Dòng tiền ròng (Net Movement): %s ₫\n" +
                    "   - Tiền ký quỹ Escrow đang giữ: %s ₫\n" +
                    "   - Doanh thu phí nền tảng (10%%): %s ₫\n" +
                    "2. Rủi ro & Tác vụ vận hành:\n" +
                    "   - Phiếu hỗ trợ quá hạn SLA: %d phiếu\n" +
                    "   - Tranh chấp đang mở: %d vụ\n" +
                    "   - Yêu cầu hoàn tiền chờ duyệt: %d yêu cầu\n" +
                    "   - Báo cáo vi phạm / Lách sàn: %d báo cáo\n" +
                    "   - Tiền Escrow chịu rủi ro tranh chấp: %s ₫\n" +
                    "3. Hướng dẫn thao tác Admin:\n" +
                    "   - Truy cập Bảng điều khiển Quản trị (/platform) hoặc Báo cáo Phân tích (/platform/analytics).\n" +
                    "   - Chọn khoảng ngày (7 ngày, 30 ngày, 90 ngày hoặc tùy chọn), bấm 'Áp dụng' và chọn nút 'Xuất CSV' nếu cần tải file báo cáo.",
                    df.format(moneyIn), df.format(moneyOut), df.format(netMovement), df.format(escrowHeld), df.format(platformFee),
                    overdueTickets, openDisputes, pendingRefunds, unhandledReports, df.format(escrowExposure)
                );
                
                results.add(AiSourceResponse.builder()
                    .sourceId("DASHBOARD_SUMMARY")
                    .sourceType("ADMIN_DASHBOARD")
                    .title("Thống kê Vận hành & Doanh thu Quản trị viên (7 ngày qua)")
                    .snippet(summaryText)
                    .similarity(1.0)
                    .finalScore(1.0)
                    .visibility("ADMIN_ONLY")
                    .build());
            } catch (Exception e) {
                results.add(AiSourceResponse.builder()
                    .sourceId("DASHBOARD_ERROR")
                    .sourceType("ADMIN_DASHBOARD")
                    .title("Bảng điều khiển Quản trị")
                    .snippet("Hệ thống đang tổng hợp dữ liệu báo cáo. Bạn có thể vào trực tiếp trang Quản trị (/platform hoặc /platform/analytics) để xem chi tiết.")
                    .similarity(1.0)
                    .finalScore(1.0)
                    .visibility("ADMIN_ONLY")
                    .build());
            }
        } else {
            // Non-admin request
            String restrictedSnippet = "Báo cáo doanh thu và bảng điều khiển vận hành (Dashboard / Analytics) là khu vực bảo mật dành riêng cho Quản trị viên hệ thống (Platform Admin). Bạn vui lòng đăng nhập tài khoản có quyền Quản trị viên để tra cứu số liệu này.";
            results.add(AiSourceResponse.builder()
                .sourceId("ADMIN_RESTRICTED")
                .sourceType("POLICY")
                .title("Quy định Quyền truy cập Báo cáo Quản trị")
                .snippet(restrictedSnippet)
                .similarity(1.0)
                .finalScore(1.0)
                .visibility("PUBLIC")
                .build());
        }
        
        return results;
    }
}
