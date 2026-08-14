package com.tcs.module.ai.service.provider;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.repository.WalletRepository;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiTutorFinanceContextProvider {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public List<AiSourceResponse> getTutorFinanceContext(String userRole, Long userId) {
        List<AiSourceResponse> results = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("#,###");

        if (userId != null && ("TUTOR".equals(userRole) || "TUTOR_CENTER".equals(userRole))) {
            Optional<Wallet> walletOpt = walletRepository.findByUser_UserId(userId);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                BigDecimal available = wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : BigDecimal.ZERO;
                BigDecimal frozen = wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO;

                String snippet = String.format(
                    "Thông tin tài chính cá nhân của bạn tại hệ thống TCS:\n" +
                    "- Số dư khả dụng trong ví: %s ₫\n" +
                    "- Số dư tạm giữ / đang xử lý rút: %s ₫\n" +
                    "- Trạng thái ví: %s\n" +
                    "- Hướng dẫn thao tác: Bạn có thể vào mục Quản lý Tài chính / Ví tiền (/finance) để thực hiện yêu cầu rút tiền về tài khoản ngân hàng hoặc theo dõi lịch sử giao dịch chi tiết.",
                    df.format(available), df.format(frozen), wallet.getStatus()
                );

                results.add(AiSourceResponse.builder()
                    .sourceId("TUTOR_FINANCE_" + userId)
                    .sourceType("TUTOR_FINANCE")
                    .title("Thông tin thu nhập & ví tiền cá nhân")
                    .snippet(snippet)
                    .finalScore(1.0)
                    .visibility("PRIVATE")
                    .build());
                return results;
            } else {
                String snippet = "Tài khoản của bạn hiện chưa phát sinh giao dịch nhận học phí hoặc rút tiền trên hệ thống TCS. Bạn có thể vào trang Quản lý Tài chính (/finance) để theo dõi và kết nối tài khoản ngân hàng.";
                results.add(AiSourceResponse.builder()
                    .sourceId("TUTOR_FINANCE_EMPTY")
                    .sourceType("TUTOR_FINANCE")
                    .title("Thông tin thu nhập cá nhân")
                    .snippet(snippet)
                    .finalScore(1.0)
                    .visibility("PRIVATE")
                    .build());
                return results;
            }
        }

        // General policy answer for general questions about tutor earnings
        String generalSnippet = 
            "Chính sách thu nhập & học phí gia sư tại TCS:\n" +
            "- Mức học phí theo giờ do gia sư tự thiết lập trong hồ sơ cá nhân (thường dao động từ 150.000đ - 500.000đ/buổi tùy theo cấp học, môn học và kinh nghiệm).\n" +
            "- Học viên thanh toán đặt cọc trước vào tài khoản Escrow (ký quỹ bảo đảm an toàn).\n" +
            "- Sau khi lớp học hoàn thành và được xác nhận, hệ thống tự động giải ngân học phí vào ví của gia sư (phí dịch vụ nền tảng là 10% trên mỗi giao dịch thành công).\n" +
            "- Gia sư có thể rút tiền từ ví TCS về tài khoản ngân hàng bất kỳ lúc nào tại trang Quản lý Tài chính (/finance).";

        results.add(AiSourceResponse.builder()
            .sourceId("TUTOR_EARNINGS_POLICY")
            .sourceType("POLICY")
            .title("Chính sách học phí & thu nhập gia sư TCS")
            .snippet(generalSnippet)
            .finalScore(1.0)
            .visibility("PUBLIC")
            .build());

        return results;
    }
}
