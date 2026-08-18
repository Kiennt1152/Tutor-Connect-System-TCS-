package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.TutorCertificateRepository;
import com.tcs.module.profile.repository.TutorEducationRepository;
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorRepository;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIndexerService {

    private final AiKnowledgeChunkRepository chunkRepository;
    private final FaqEntryRepository faqEntryRepository;
    private final TutorRepository tutorRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final TutorCertificateRepository certificateRepository;
    private final TutorEducationRepository educationRepository;
    private final TutorExperienceRepository experienceRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private java.time.LocalDateTime lastReindexTime = null;
    private static final int REINDEX_COOLDOWN_MINUTES = 1;

    @Transactional
    public Map<String, Integer> reindexAll() {
        if (lastReindexTime != null) {
            long minutesSince = java.time.Duration.between(lastReindexTime, java.time.LocalDateTime.now()).toMinutes();
            if (minutesSince < REINDEX_COOLDOWN_MINUTES) {
                long remaining = REINDEX_COOLDOWN_MINUTES - minutesSince;
                throw new com.tcs.exception.BusinessException(
                        "Vui lòng đợi " + (remaining <= 0 ? 1 : remaining) + " phút trước khi thực hiện đánh chỉ mục lại (Reindex)."
                );
            }
        }
        lastReindexTime = java.time.LocalDateTime.now();

        log.info("Starting AI Knowledge Reindexing...");
        Map<String, Integer> stats = new HashMap<>();
        stats.put("indexed", 0);
        stats.put("updated", 0);
        stats.put("unchanged", 0);
        stats.put("skipped", 0);
        stats.put("failed", 0);

        // 1. FAQ
        List<FaqEntry> faqs = faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc();
        for (FaqEntry faq : faqs) {
            String content = String.format(
                "FAQ Câu hỏi: %s\nTrả lời: %s\nChuyên mục: %s",
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getCategory() != null ? faq.getCategory() : "GENERAL"
            );
            Map<String, Object> meta = new HashMap<>();
            meta.put("category", faq.getCategory());
            meta.put("question", faq.getQuestion());
            meta.put("faqId", faq.getFaqId());
            indexChunk(KnowledgeSourceType.FAQ, String.valueOf(faq.getFaqId()), faq.getQuestion(), content, meta, stats);
        }

        // 2. Tutors (Only ACTIVE)
        List<Tutor> tutors = tutorRepository.findAll().stream()
                .filter(t -> t.getUser() != null && t.getUser().getStatus() == UserStatus.ACTIVE)
                .toList();
        for (Tutor tutor : tutors) {
            StringBuilder extraInfo = new StringBuilder();

            String content = String.format(
                    "Gia sư: %s\nGiới tính: %s\nKhu vực: %s\nHọc phí: %s\nRating: %s\nKinh nghiệm: %s năm\nTrạng thái xác minh: %s\nGiới thiệu: %s%s", 
                    tutor.getFullName(),
                    tutor.getGender() != null ? tutor.getGender().name() : "Không rõ",
                    tutor.getAddress() != null ? tutor.getAddress() : "Không rõ",
                    tutor.getHourlyRate() != null ? tutor.getHourlyRate() : "Thỏa thuận",
                    tutor.getRatingAvg() != null ? tutor.getRatingAvg() : "Chưa có",
                    tutor.getExperienceYears() != null ? tutor.getExperienceYears() : 0,
                    tutor.getVerificationStatus() == com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED ? "Đã xác minh" : "Chưa xác minh",
                    tutor.getBio() != null ? tutor.getBio() : "",
                    extraInfo.toString()
            );

            Map<String, Object> meta = new HashMap<>();
            meta.put("tutorId", tutor.getTutorId());
            meta.put("fullName", tutor.getFullName());
            meta.put("hourlyRate", tutor.getHourlyRate());
            meta.put("ratingAvg", tutor.getRatingAvg());
            meta.put("address", tutor.getAddress());
            meta.put("verified", tutor.getVerificationStatus() == com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED);
            meta.put("bio", tutor.getBio());

            indexChunk(KnowledgeSourceType.TUTOR, String.valueOf(tutor.getTutorId()), tutor.getFullName(), content, meta, stats);
        }

        // 3. Classes (Only OPEN)
        List<TutoringClass> classes = tutoringClassRepository.findByStatus(TutoringClassStatus.OPEN);
        for (TutoringClass c : classes) {
            String content = String.format(
                    "Lớp học: %s\nMôn học: %s\nKhối lớp: %s\nHọc phí: %s\nĐịa điểm: %s\nYêu cầu: %s\nMô tả chi tiết: %s",
                    c.getTitle(),
                    c.getSubject() != null ? c.getSubject().getSubjectName() : "Không rõ",
                    c.getGrade() != null ? c.getGrade().getGradeName() : "Không rõ",
                    c.getTuitionFee() != null ? c.getTuitionFee() : "Thỏa thuận",
                    c.getAddress() != null ? c.getAddress() : "Không rõ",
                    c.getLearningGoal() != null ? c.getLearningGoal() : "Không có",
                    c.getDescription() != null ? c.getDescription() : ""
            );

            Map<String, Object> meta = new HashMap<>();
            meta.put("classId", c.getClassId());
            meta.put("title", c.getTitle());
            meta.put("subject", c.getSubject() != null ? c.getSubject().getSubjectName() : "");
            meta.put("grade", c.getGrade() != null ? c.getGrade().getGradeName() : "");
            meta.put("tuitionFee", c.getTuitionFee());
            meta.put("address", c.getAddress());

            indexChunk(KnowledgeSourceType.CLASS, String.valueOf(c.getClassId()), c.getTitle(), content, meta, stats);
        }

        // 4. Core System Policies
        indexChunk(KnowledgeSourceType.POLICY, "POLICY_ESCROW_AND_FEES",
                "Chính sách Ký quỹ Escrow và Phí sàn 10%",
                "Quy định Ký quỹ Escrow và Phí nền tảng:\n" +
                "- Khi phụ huynh đồng ý thuê gia sư, số tiền học phí sẽ được nạp và tạm khóa trong tài khoản ký quỹ Escrow nhằm đảm bảo quyền lợi.\n" +
                "- Hệ thống chỉ giải ngân học phí cho gia sư sau khi học viên/phụ huynh xác nhận hoàn thành buổi học hoặc khóa học thành công.\n" +
                "- Phí sàn (Platform Fee): TCS thu 10% trên tổng giá trị hợp đồng khi giải ngân Escrow cho gia sư/trung tâm để duy trì hệ thống.",
                Map.of("category", "PAYMENT", "feeRate", 0.10, "tags", "escrow,phi_san,thanh_toan,sepay,nap_tien"), stats);

        indexChunk(KnowledgeSourceType.POLICY, "POLICY_WITHDRAWAL",
                "Chính sách Rút tiền từ ví TCS về tài khoản ngân hàng",
                "Quy định Rút tiền (Withdrawal):\n" +
                "- Người dùng (Gia sư, Trung tâm) có số dư khả dụng có thể gửi yêu cầu rút tiền tại /finance.\n" +
                "- Hệ thống xử lý lệnh rút tiền tự động hoặc qua duyệt của Admin trong vòng 12 - 24 giờ.\n" +
                "- Số tiền rút tối thiểu: 50.000 VNĐ. Tài khoản ngân hàng nhận tiền phải trùng khớp tên với thông tin xác minh danh tính.",
                Map.of("category", "PAYMENT", "tags", "rut_tien,ngan_hang,vi_tien,withdrawal"), stats);

        indexChunk(KnowledgeSourceType.POLICY, "POLICY_CONTRACT_OTP",
                "Quy định Hợp đồng dịch vụ điện tử 3 bên và Ký hợp đồng bằng mã OTP",
                "Quy trình ký Hợp đồng học tập điện tử:\n" +
                "- Khi phụ huynh và gia sư thống nhất mức phí và lịch học, hệ thống tạo Hợp đồng dịch vụ điện tử 3 bên (Phụ huynh - Gia sư - Sàn TCS) tại /contracts.\n" +
                "- Hai bên thực hiện ký hợp đồng trực tuyến bằng cách nhập mã xác thực OTP gửi về số điện thoại/email đăng ký.\n" +
                "- Hợp đồng có giá trị pháp lý ràng buộc về quyền lợi, nghĩa vụ, lịch dạy, mức học phí và điều khoản bồi thường khi vi phạm.",
                Map.of("category", "CONTRACT", "tags", "hop_dong,ky_otp,hop_dong_dien_tu,contracts"), stats);

        indexChunk(KnowledgeSourceType.POLICY, "POLICY_REFUND_AND_DISPUTE", 
                "Chính sách Hoàn tiền và Giải quyết tranh chấp",
                "Quy trình Hoàn tiền (Refund) và Giải quyết tranh chấp (Dispute):\n" +
                "- Hủy lớp học trước 24 giờ kể từ lịch học: Học viên được hoàn trả 100% số tiền đặt cọc trong Escrow về ví.\n" +
                "- Hủy lớp học trước 12 giờ: Học viên được hoàn 50% tiền cọc, 50% còn lại bồi thường cho gia sư.\n" +
                "- Sau khi lớp học đã bắt đầu hoặc phát sinh khiếu nại (gia sư vắng mặt không phép, dạy sai cam kết, gian lận): Các bên có quyền mở Tranh chấp (Dispute) tại /support/tickets. Admin sẽ xem xét chứng cứ trong 48 giờ và ra quyết định phân bổ 100% Escrow (hoàn tiền cho học viên, giải ngân cho gia sư, hoặc chia tỷ lệ bồi hoàn).",
                Map.of("category", "DISPUTE", "tags", "hoan_tien,tranh_chap,refund,dispute,khieu_nai"), stats);

        indexChunk(KnowledgeSourceType.POLICY, "POLICY_REPUTATION_AND_REVIEWS",
                "Cách tính Điểm uy tín gia sư và Đánh giá sau khóa học",
                "Hệ thống Đánh giá & Điểm uy tín (Reputation Score):\n" +
                "- Điểm uy tín khởi điểm của gia sư: 100 điểm.\n" +
                "- Cộng điểm uy tín: Hoàn thành lớp học đúng hạn (+5 điểm), nhận đánh giá 5 sao từ phụ huynh (+2 điểm), xác minh CCCD/bằng cấp đầy đủ (+10 điểm).\n" +
                "- Trừ điểm uy tín: Hủy lớp sát giờ không lý do chính đáng (-15 điểm), bị cảnh cáo vi phạm quy chế sàn (-20 điểm), bị xử thua tranh chấp (-30 điểm).\n" +
                "- Gia sư có điểm uy tín cao (>90 điểm) và rating >= 4.8 sẽ nhận huy hiệu 'Gia sư Uy tín' và được đẩy lên đầu trang tìm kiếm /tim-gia-su.",
                Map.of("category", "REPUTATION", "tags", "uy_tin,reputation,danh_gia,rating,diem_uy_tin"), stats);

        indexChunk(KnowledgeSourceType.POLICY, "POLICY_CIRCUMVENTION_PREVENTION", 
                "Chính sách Phòng chống Lách sàn và Xử phạt vi phạm",
                "Quy định Chống Lách sàn (Platform Circumvention):\n" +
                "- Nghiêm cấm mọi hành vi gia sư, trung tâm hoặc học viên chủ động gạ gẫm, chia sẻ thông tin liên lạc riêng (SĐT, Zalo, STK ngân hàng) nhằm giao dịch ngoài sàn để trốn phí nền tảng 10%.\n" +
                "- Hệ thống tự động phát hiện và nhận báo cáo vi phạm lách sàn tại /support/tickets (Report Category: PLATFORM_CIRCUMVENTION / FRAUD).\n" +
                "- Mức xử phạt: Cảnh cáo lần đầu (Warning), phạt trừ tiền ví, đình chỉ tài khoản tạm thời (Suspension) hoặc Khóa tài khoản vĩnh viễn (Ban) và phong tỏa số dư ví đối với các trường hợp cố tình tái phạm nghiêm trọng.",
                Map.of("category", "PENALTY", "tags", "lach_san,xu_phat,circumvention,gian_lan,to_cao"), stats);

        indexChunk(KnowledgeSourceType.POLICY, "POLICY_SUPPORT_TICKETS_SLA",
                "Chính sách Hỗ trợ khách hàng, Gửi Ticket khiếu nại và Cam kết SLA",
                "Quy định Tiếp nhận & Xử lý Ticket hỗ trợ tại /support/tickets:\n" +
                "- Người dùng có thể tạo Ticket để yêu cầu hỗ trợ tài khoản, nạp/rút tiền, báo cáo sự cố kỹ thuật hoặc tranh chấp lớp học.\n" +
                "- Cam kết thời gian phản hồi SLA:\n" +
                "  + Mức độ Khẩn cấp (CRITICAL - ví dụ lỗi nạp tiền, sự cố an toàn): Phản hồi trong vòng 2 - 4 giờ.\n" +
                "  + Mức độ Cao (HIGH - ví dụ tranh chấp lớp học): Phản hồi trong vòng 12 - 24 giờ.\n" +
                "  + Mức độ Thường (NORMAL - câu hỏi chung, góp ý): Phản hồi trong vòng 24 - 48 giờ làm việc.",
                Map.of("category", "TICKETS", "tags", "ticket,sla,ho_tro,cskh,khieu_nai"), stats);
        
        log.info("Finished AI Knowledge Reindexing. Stats: {}", stats);
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getKnowledgeStats() {
        Map<String, Object> stats = new HashMap<>();
        List<AiKnowledgeChunk> all = chunkRepository.findAll();
        stats.put("totalChunks", all.size());
        
        Map<String, Integer> bySourceType = new HashMap<>();
        int withoutEmbedding = 0;
        String lastIndexedAt = null;
        
        for (AiKnowledgeChunk c : all) {
            String type = c.getSourceType().name();
            bySourceType.put(type, bySourceType.getOrDefault(type, 0) + 1);
            if (c.getEmbeddingJson() == null || c.getEmbeddingJson().isBlank()) {
                withoutEmbedding++;
            }
            if (c.getLastIndexedAt() != null) {
                if (lastIndexedAt == null || c.getLastIndexedAt().toString().compareTo(lastIndexedAt) > 0) {
                    lastIndexedAt = c.getLastIndexedAt().toString();
                }
            }
        }
        
        stats.put("bySourceType", bySourceType);
        stats.put("withoutEmbedding", withoutEmbedding);
        stats.put("lastIndexedAt", lastIndexedAt);
        return stats;
    }

    private void indexChunk(KnowledgeSourceType type, String sourceId, String title, String content, Object metadata, Map<String, Integer> stats) {
        String hash = computeHash(content);
        Optional<AiKnowledgeChunk> opt = chunkRepository.findBySourceTypeAndSourceId(type, sourceId);
        
        boolean isNew = opt.isEmpty();
        AiKnowledgeChunk chunk = opt.orElseGet(() -> AiKnowledgeChunk.builder()
                .sourceType(type)
                .sourceId(sourceId)
                .embeddingModel("models/text-embedding-004")
                .active(true)
                .visibility("PUBLIC")
                .locale("vi")
                .build());

        if (!isNew && hash.equals(chunk.getContentHash()) && chunk.getEmbeddingJson() != null) {
            stats.put("unchanged", stats.get("unchanged") + 1);
            return;
        }

        chunk.setTitle(title);
        chunk.setContent(content);
        chunk.setContentHash(hash);
        chunk.setLastIndexedAt(java.time.LocalDateTime.now());
        chunk.setTokenCount(content.length() / 4);
        
        try {
            if (metadata != null) {
                chunk.setMetadataJson(objectMapper.writeValueAsString(metadata));
            }
            chunk.setQualityScore(calculateQualityScore(chunk));

            Optional<double[]> vectorOpt = embeddingService.getEmbedding(content);
            if (vectorOpt.isPresent()) {
                chunk.setEmbeddingJson(objectMapper.writeValueAsString(vectorOpt.get()));
                chunkRepository.save(chunk);
                if (isNew) {
                    stats.put("indexed", stats.get("indexed") + 1);
                } else {
                    stats.put("updated", stats.get("updated") + 1);
                }
            } else {
                log.warn("Embedding generation skipped for {} {}, saving chunk with text-only fallback.", type, sourceId);
                chunk.setEmbeddingJson(null);
                chunkRepository.save(chunk);
                stats.put("skipped", stats.get("skipped") + 1);
            }
        } catch (Exception e) {
            log.error("Error generating embedding for {} {}", type, sourceId, e);
            chunk.setEmbeddingJson(null);
            chunkRepository.save(chunk);
            stats.put("failed", stats.get("failed") + 1);
        }
    }

    private String computeHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public double calculateQualityScore(AiKnowledgeChunk chunk) {
        double score = 0.5; // Base score
        
        // Metadata richness & Verification status
        if (chunk.getMetadataJson() != null && !chunk.getMetadataJson().isBlank()) {
            score += 0.12;
            try {
                com.fasterxml.jackson.databind.JsonNode meta = objectMapper.readTree(chunk.getMetadataJson());
                boolean isVerified = meta.has("verified") && meta.get("verified").asBoolean(false);
                if (isVerified) {
                    score += 0.08; // High priority trust bonus
                    if (meta.has("ratingAvg") && meta.get("ratingAvg").asDouble(0.0) >= 4.8) {
                        score += 0.05; // Super tutor bonus
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // Content length is optimal (200-800 chars)
        int contentLen = chunk.getContent() != null ? chunk.getContent().length() : 0;
        if (contentLen >= 200 && contentLen <= 800) {
            score += 0.15;
        } else if (contentLen > 100) {
            score += 0.05;
        }
        
        // Has clear title
        if (chunk.getTitle() != null && chunk.getTitle().length() >= 10) {
            score += 0.05;
        }
        
        // Recent data (updated within 90 days)
        if (chunk.getSourceUpdatedAt() != null) {
            long daysSinceUpdate = java.time.Duration.between(chunk.getSourceUpdatedAt(), java.time.LocalDateTime.now()).toDays();
            if (daysSinceUpdate <= 90) {
                score += 0.05;
            }
        }
        
        return Math.min(1.0, score);
    }
}
