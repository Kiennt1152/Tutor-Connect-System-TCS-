package com.tcs.module.ai.service;

import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DynamicFaqGenerationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DynamicFaqGenerationService.class);

    private final SupportTicketRepository supportTicketRepository;
    private final FaqEntryRepository faqEntryRepository;

    // =========================================================================
    // LUỒNG 6: TỰ ĐỘNG ĐỀ XUẤT BẢN NHÁP FAQ TỪ TICKET LẶP LẠI (UC-67)
    // =========================================================================

    // Luồng 6 - Tác vụ chạy ngầm lúc 02:00 AM hàng ngày quét ticket 7 ngày qua
    @Scheduled(cron = "0 0 2 * * *")
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(
        name = "dynamicFaqGeneration",
        lockAtMostFor = "2h",
        lockAtLeastFor = "10m"
    )
    @Transactional
    public void scheduledNightlyFaqGeneration() {
        log.info("[DynamicFaqGenerationService] Running scheduled nightly support ticket FAQ generation...");
        List<FaqEntry> generated = generateFaqsFromRecentTickets(7, 2);
        log.info("[DynamicFaqGenerationService] Generated {} draft FAQs for admin review.", generated.size());
    }

    // Luồng 6 - Thuật toán gom cụm chủ đề và tự động sinh FAQ nháp
    @Transactional
    public List<FaqEntry> generateFaqsFromRecentTickets(int daysBack, int minOccurrences) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        List<SupportTicket> recentTickets = supportTicketRepository.findByCreatedAtAfter(since);
        if (recentTickets == null || recentTickets.isEmpty()) {
            recentTickets = supportTicketRepository.findAll().stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                    .toList();
        }

        if (recentTickets.isEmpty()) {
            return List.of();
        }

        // Bước 1: Gom cụm các ticket theo category và từ khóa chủ đề chuẩn hóa (Cluster Key)
        Map<String, List<SupportTicket>> clusters = new HashMap<>();
        for (SupportTicket ticket : recentTickets) {
            String clusterKey = buildClusterKey(ticket);
            clusters.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(ticket);
        }

        List<FaqEntry> createdDrafts = new ArrayList<>();
        List<FaqEntry> existingFaqs = faqEntryRepository.findAll();

        // Bước 2: Duyệt qua từng cụm sự cố
        for (Map.Entry<String, List<SupportTicket>> entry : clusters.entrySet()) {
            List<SupportTicket> group = entry.getValue();
            // Chỉ xem xét vấn đề lặp lại >= minOccurrences (>= 2 lần)
            if (group.size() < minOccurrences) {
                continue;
            }

            SupportTicket sample = group.get(0);
            String candidateQuestion = buildCanonicalQuestion(sample);
            String candidateAnswer = buildCanonicalAnswer(sample, group.size());
            String category = sample.getCategory() != null ? sample.getCategory().name() : "GENERAL";

            // Bước 3: Kiểm tra đối chiếu không dấu chống tạo trùng lặp với kho FAQ hiện tại
            boolean alreadyExists = existingFaqs.stream().anyMatch(f ->
                f.getQuestion() != null &&
                VietnameseTextNormalizer.normalize(f.getQuestion().toLowerCase(Locale.ROOT))
                    .contains(VietnameseTextNormalizer.normalize(candidateQuestion.toLowerCase(Locale.ROOT)))
            );

            // Bước 4: Lưu bài viết FAQ mới dưới dạng DRAFT (published = false) chờ Admin duyệt
            if (!alreadyExists) {
                FaqEntry draft = new FaqEntry();
                draft.setQuestion(candidateQuestion);
                draft.setAnswer(candidateAnswer);
                draft.setCategory(category);
                draft.setSortOrder(99);
                draft.setPublished(false); // Trạng thái DRAFT chờ Admin duyệt trên trang quản trị

                FaqEntry saved = faqEntryRepository.save(draft);
                createdDrafts.add(saved);
                existingFaqs.add(saved);
                log.info("[DynamicFaqGenerationService] Created draft FAQ #{}: '{}' from {} recurring tickets",
                         saved.getFaqId(), candidateQuestion, group.size());
            }
        }

        return createdDrafts;
    }

    private String buildClusterKey(SupportTicket ticket) {
        String cat = ticket.getCategory() != null ? ticket.getCategory().name() : "GENERAL";
        String normSub = ticket.getSubject() != null
                ? VietnameseTextNormalizer.normalize(ticket.getSubject().toLowerCase(Locale.ROOT))
                : "";
        return cat + ":" + simplifySubject(normSub);
    }

    private String simplifySubject(String normSub) {
        if (normSub.contains("nap tien") || normSub.contains("vietqr") || normSub.contains("sepay")) return "TOPUP_ISSUE";
        if (normSub.contains("rut tien") || normSub.contains("ngan hang")) return "WITHDRAWAL_ISSUE";
        if (normSub.contains("hoan tien") || normSub.contains("huy lop")) return "REFUND_ISSUE";
        if (normSub.contains("cccd") || normSub.contains("xac minh") || normSub.contains("bang cap")) return "VERIFICATION_ISSUE";
        if (normSub.contains("hop dong") || normSub.contains("otp")) return "CONTRACT_ISSUE";
        if (normSub.contains("diem danh") || normSub.contains("doi lich") || normSub.contains("day thay")) return "ATTENDANCE_ISSUE";
        return normSub.length() > 20 ? normSub.substring(0, 20) : normSub;
    }

    private String buildCanonicalQuestion(SupportTicket sample) {
        String sub = sample.getSubject() != null ? sample.getSubject().trim() : "Hỗ trợ dịch vụ";
        if (!sub.endsWith("?")) {
            sub = sub + " như thế nào?";
        }
        return sub;
    }

    private String buildCanonicalAnswer(SupportTicket sample, int count) {
        SupportTicketCategory cat = sample.getCategory();
        if (cat == null) cat = SupportTicketCategory.INQUIRY;

        return switch (cat) {
            case INQUIRY -> "Hệ thống hỗ trợ thanh toán học phí tự động qua VietQR/SePay và lưu giữ trong tài khoản Escrow an toàn. Nếu giao dịch nạp tiền chưa ghi nhận sau 5 phút, vui lòng kiểm tra lại nội dung chuyển khoản hoặc liên hệ ban quản trị.";
            case DISPUTE -> "Khi có tranh chấp về buổi học hoặc lịch dạy, bạn có thể mở yêu cầu khiếu nại trong mục Hỗ trợ để ban quản trị tiếp nhận và trung gian hòa giải theo chính sách Escrow.";
            case SYSTEM_ERROR -> "Nếu gặp sự cố hệ thống hoặc lỗi kết nối, vui lòng kiểm tra đường truyền mạng hoặc tạo ticket để đội ngũ kỹ thuật TCS kiểm tra và xử lý.";
            case REPORT_USER -> "TCS luôn bảo vệ quyền lợi người dùng và xử lý nghiêm các hành vi lách sàn, vi phạm quy tắc cộng đồng hoặc gian lận.";
            case BUG_REPORT -> "Cảm ơn bạn đã đóng góp phản hồi về lỗi giao diện hoặc tính năng. Đội ngũ phát triển TCS sẽ khắc phục trong bản cập nhật sớm nhất.";
        };
    }
}
