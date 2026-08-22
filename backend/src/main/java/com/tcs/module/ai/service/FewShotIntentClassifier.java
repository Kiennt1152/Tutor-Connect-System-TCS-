package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class FewShotIntentClassifier {

    public record FewShotMatch(
        AiDomain domain,
        AiSubIntent subIntent,
        AiIntent legacyIntent,
        double similarityScore,
        String matchedExemplar
    ) {}

    private static final Map<AiSubIntent, List<String>> EXEMPLARS = new EnumMap<>(AiSubIntent.class);
    private static final Map<AiSubIntent, AiDomain> INTENT_DOMAINS = new EnumMap<>(AiSubIntent.class);
    private static final Map<AiSubIntent, AiIntent> LEGACY_MAPPINGS = new EnumMap<>(AiSubIntent.class);

    static {
        // 1. Marketplace
        register(AiSubIntent.FIND_TUTOR, AiDomain.MARKETPLACE, AiIntent.FIND_TUTOR, List.of(
            "Tìm gia sư Toán 12 tại Cầu Giấy",
            "Cần thầy dạy Hóa online luyện thi đại học",
            "Có giáo viên nào dạy Tiếng Anh giao tiếp cho người đi làm không",
            "Gia sư dạy kèm môn Lý lớp 10 học phí dưới 300k",
            "Tìm cô giáo dạy kèm tiểu học rèn chữ đẹp tại nhà",
            "Cần thuê gia sư IELTS 7.0 cấp tốc"
        ));

        register(AiSubIntent.FIND_CLASS, AiDomain.MARKETPLACE, AiIntent.FIND_CLASS, List.of(
            "Có lớp nào đang tuyển gia sư môn Toán không",
            "Tìm lớp dạy kèm Tiếng Anh buổi tối",
            "Danh sách lớp học mới đăng cần giáo viên",
            "Tôi muốn nhận lớp dạy kèm môn Sinh ở Ba Đình",
            "Có lớp luyện thi vào 10 nào đang mở không"
        ));

        register(AiSubIntent.CREATE_CLASS, AiDomain.MARKETPLACE, AiIntent.CREATE_CLASS, List.of(
            "Tôi muốn tạo một lớp học mới tìm gia sư",
            "Đăng tin tìm gia sư dạy cho con",
            "Mở lớp học kèm tại nhà",
            "Cách tạo yêu cầu lớp học trên hệ thống"
        ));

        register(AiSubIntent.APPLY_TO_CLASS, AiDomain.MARKETPLACE, AiIntent.FIND_CLASS, List.of(
            "Làm sao để ứng tuyển nhận lớp này",
            "Tôi muốn nộp đơn dạy lớp mã 105",
            "Cách gia sư apply vào lớp học của phụ huynh"
        ));

        // 2. Finance / Wallet / Escrow
        register(AiSubIntent.WALLET_VIEW, AiDomain.FINANCE_WALLET, AiIntent.PAYMENT_SUPPORT, List.of(
            "Số dư ví hiện tại của tôi là bao nhiêu",
            "Xem lịch sử giao dịch ví tiền",
            "Kiểm tra tiền trong ví TCS",
            "Tôi đã kiếm được bao nhiêu tiền tháng này"
        ));

        register(AiSubIntent.WALLET_TOPUP, AiDomain.FINANCE_WALLET, AiIntent.PAYMENT_SUPPORT, List.of(
            "Làm sao để nạp tiền vào ví",
            "Nạp tiền qua VietQR SePay như thế nào",
            "Cách chuyển khoản thanh toán học phí qua mã QR",
            "Hướng dẫn nạp ví TCS"
        ));

        register(AiSubIntent.WITHDRAWAL_REQUEST, AiDomain.FINANCE_WALLET, AiIntent.PAYMENT_SUPPORT, List.of(
            "Tôi muốn rút tiền về tài khoản ngân hàng",
            "Làm lệnh yêu cầu rút tiền từ ví",
            "Gia sư rút tiền học phí về Vietcombank",
            "Thời gian xử lý rút tiền mất bao lâu"
        ));

        register(AiSubIntent.PLATFORM_FEE_EXPLAIN, AiDomain.FINANCE_WALLET, AiIntent.PAYMENT_SUPPORT, List.of(
            "Phí nền tảng TCS là bao nhiêu",
            "Phí sàn 10% tính trên khoản nào",
            "Hệ thống có thu phí dịch vụ khi nhận lớp không",
            "Quy định về mức phí hoa hồng của sàn"
        ));

        register(AiSubIntent.ESCROW_EXPLAIN, AiDomain.FINANCE_WALLET, AiIntent.PAYMENT_SUPPORT, List.of(
            "Ký quỹ Escrow hoạt động như thế nào",
            "Tiền cọc học phí giữ trong Escrow có an toàn không",
            "Khi nào tiền học phí được giải ngân cho gia sư",
            "Chính sách giữ tiền tạm khóa Escrow"
        ));

        register(AiSubIntent.REFUND_POLICY, AiDomain.FINANCE_WALLET, AiIntent.PAYMENT_SUPPORT, List.of(
            "Chính sách hoàn tiền khi gia sư hủy lớp",
            "Làm sao để yêu cầu hoàn lại học phí đã nộp",
            "Hủy hợp đồng trước 24h có được hoàn 100% không",
            "Quy trình xử lý hoàn tiền của TCS"
        ));

        // 3. Contract & Review
        register(AiSubIntent.CONTRACT_SIGN_OTP, AiDomain.CONTRACT_REVIEW, AiIntent.PAYMENT_SUPPORT, List.of(
            "Hướng dẫn ký hợp đồng điện tử bằng mã OTP",
            "Mã OTP xác nhận ký hợp đồng gửi về đâu",
            "Cách phụ huynh và gia sư hoàn tất ký hợp đồng"
        ));

        // 4. Verification
        register(AiSubIntent.TUTOR_VERIFICATION_HELP, AiDomain.VERIFICATION, AiIntent.TUTOR_VERIFICATION, List.of(
            "Cách tải lên ảnh CCCD và bằng cấp để xác minh",
            "Hồ sơ gia sư cần những giấy tờ gì để được duyệt",
            "Thời gian duyệt xác minh hồ sơ mất bao lâu",
            "Tại sao hồ sơ xác minh của tôi bị từ chối"
        ));

        // 5. Support & Ticket
        register(AiSubIntent.SUPPORT_TICKET_CREATE, AiDomain.MESSAGING_TICKET, AiIntent.TICKET_SUPPORT, List.of(
            "Tôi muốn tạo một yêu cầu hỗ trợ",
            "Gửi ticket nhờ ban quản trị xử lý sự cố",
            "Báo cáo lỗi nạp tiền qua ticket"
        ));

        register(AiSubIntent.DISPUTE_OPEN_HELP, AiDomain.TRUST_SAFETY, AiIntent.TICKET_SUPPORT, List.of(
            "Cách mở khiếu nại tranh chấp với gia sư",
            "Gia sư không đến dạy đúng giờ làm sao khiếu nại",
            "Quy trình xử lý tranh chấp và cung cấp bằng chứng"
        ));

        // 6. Safety / Conversation
        register(AiSubIntent.GREETING, AiDomain.CONVERSATION_SAFETY, AiIntent.OUT_OF_SCOPE, List.of(
            "Xin chào bot", "Chào bạn", "Hello TCS", "Hi trợ lý", "Alo bot ơi"
        ));

        register(AiSubIntent.BOT_CAPABILITY_ASK, AiDomain.CONVERSATION_SAFETY, AiIntent.FAQ_SUPPORT, List.of(
            "Bạn có thể giúp gì cho tôi", "Bot làm được những việc gì", "Chức năng của trợ lý ảo TCS"
        ));

        register(AiSubIntent.OUT_OF_SCOPE, AiDomain.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, List.of(
            "Thời tiết Hà Nội hôm nay thế nào",
            "Giá vàng hôm nay bao nhiêu một lượng",
            "Viết cho tôi một bài thơ tình",
            "Thủ đô của nước Pháp là gì",
            "Cách nấu món phở bò truyền thống"
        ));
    }

    private static void register(AiSubIntent subIntent, AiDomain domain, AiIntent legacyIntent, List<String> exemplars) {
        EXEMPLARS.put(subIntent, exemplars);
        INTENT_DOMAINS.put(subIntent, domain);
        LEGACY_MAPPINGS.put(subIntent, legacyIntent);
    }

    public Optional<FewShotMatch> classifyWithExemplars(String query, double minSimilarityThreshold) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        String queryNorm = VietnameseTextNormalizer.normalize(query.toLowerCase(Locale.ROOT));
        Set<String> queryTokens = tokenize(queryNorm);
        if (queryTokens.isEmpty()) {
            return Optional.empty();
        }

        AiSubIntent bestSubIntent = null;
        String bestExemplar = null;
        double highestSimilarity = 0.0;

        for (Map.Entry<AiSubIntent, List<String>> entry : EXEMPLARS.entrySet()) {
            AiSubIntent subIntent = entry.getKey();
            for (String exemplar : entry.getValue()) {
                String exNorm = VietnameseTextNormalizer.normalize(exemplar.toLowerCase(Locale.ROOT));
                Set<String> exTokens = tokenize(exNorm);

                double sim = calculateJaccardSimilarity(queryTokens, exTokens);
                if (queryNorm.contains(exNorm) || exNorm.contains(queryNorm)) {
                    sim = Math.max(sim, 0.85);
                }

                if (sim > highestSimilarity) {
                    highestSimilarity = sim;
                    bestSubIntent = subIntent;
                    bestExemplar = exemplar;
                }
            }
        }

        if (bestSubIntent != null && highestSimilarity >= minSimilarityThreshold) {
            AiDomain domain = INTENT_DOMAINS.getOrDefault(bestSubIntent, AiDomain.CATALOG_FAQ);
            AiIntent legacy = LEGACY_MAPPINGS.getOrDefault(bestSubIntent, AiIntent.FAQ_SUPPORT);
            return Optional.of(new FewShotMatch(domain, bestSubIntent, legacy, highestSimilarity, bestExemplar));
        }

        return Optional.empty();
    }

    private double calculateJaccardSimilarity(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        String[] tokens = text.replaceAll("[^a-z0-9à-ỹ%\\s]", " ").trim().split("\\s+");
        Set<String> set = new HashSet<>();
        for (String t : tokens) {
            String token = t.trim();
            if (token.length() >= 2) {
                set.add(token);
            }
        }
        return set;
    }
}
