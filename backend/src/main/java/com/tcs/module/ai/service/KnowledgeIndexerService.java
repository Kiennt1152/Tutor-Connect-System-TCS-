package com.tcs.module.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.identity.enums.UserStatus;
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
    private final FaqEntryRepository faqRepository;
    private final TutorRepository tutorRepository;
    private final TutoringClassRepository classRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private java.time.LocalDateTime lastReindexTime = null;
    private static final int REINDEX_COOLDOWN_MINUTES = 5;

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
        List<FaqEntry> faqs = faqRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc();
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
            String content = String.format(
                    "Gia sư: %s\nKhu vực: %s\nHọc phí: %s\nRating: %s\nKinh nghiệm: %s năm\nTrạng thái xác minh: %s\nGiới thiệu: %s", 
                    tutor.getFullName(),
                    tutor.getAddress() != null ? tutor.getAddress() : "Không rõ",
                    tutor.getHourlyRate() != null ? tutor.getHourlyRate() : "Thỏa thuận",
                    tutor.getRatingAvg() != null ? tutor.getRatingAvg() : "Chưa có",
                    tutor.getExperienceYears() != null ? tutor.getExperienceYears() : 0,
                    tutor.getVerificationStatus() == com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED ? "Đã xác minh" : "Chưa xác minh",
                    tutor.getBio() != null ? tutor.getBio() : "");
            
            Map<String, Object> meta = new HashMap<>();
            meta.put("area", tutor.getAddress());
            meta.put("fee", tutor.getHourlyRate());
            meta.put("ratingAvg", tutor.getRatingAvg());
            meta.put("experienceYears", tutor.getExperienceYears());
            meta.put("verified", tutor.getVerificationStatus() == com.tcs.module.profile.enums.ProfileVerificationStatus.VERIFIED);
            
            indexChunk(KnowledgeSourceType.TUTOR, String.valueOf(tutor.getTutorId()), tutor.getFullName(), content, meta, stats);
        }

        // 3. Classes (Only OPEN)
        List<TutoringClass> classes = classRepository.findByStatus(TutoringClassStatus.OPEN);
        for (TutoringClass cls : classes) {
            String content = String.format(
                    "Lớp học: %s\nMôn học: %s\nKhu vực: %s\nHọc phí: %s\nTrạng thái: %s\nMô tả: %s", 
                    cls.getTitle(),
                    cls.getSubject() != null ? cls.getSubject().getSubjectName() : "Không rõ",
                    cls.getAddress() != null ? cls.getAddress() : "Online",
                    cls.getTuitionFee() != null ? cls.getTuitionFee() : "Thỏa thuận",
                    cls.getStatus(),
                    cls.getDescription() != null ? cls.getDescription() : "");
            
            Map<String, Object> meta = new HashMap<>();
            meta.put("subject", cls.getSubject() != null ? cls.getSubject().getSubjectName() : "");
            meta.put("area", cls.getAddress());
            meta.put("fee", cls.getTuitionFee());
            meta.put("status", cls.getStatus());
            
            indexChunk(KnowledgeSourceType.CLASS, String.valueOf(cls.getClassId()), cls.getTitle(), content, meta, stats);
        }

        // 4. System Documents (SYSTEM_DOC)
        indexChunk(KnowledgeSourceType.SYSTEM_DOC, "SYSTEM_OVERVIEW", 
                "Giới thiệu hệ thống Tutor Connect System (TCS)",
                "Tutor Connect System (TCS) là nền tảng công nghệ kết nối trực tiếp, thông minh và an toàn giữa phụ huynh/học viên có nhu cầu học kèm và gia sư hoặc trung tâm gia sư uy tín. TCS tích hợp thanh toán ký quỹ Escrow bảo vệ tài chính 2 bên, tự động phân luồng xử lý tranh chấp và hỗ trợ học tập qua Trợ lý AI.",
                Map.of("category", "GENERAL", "tags", "tcs,he_thong,gioi_thieu"), stats);

        indexChunk(KnowledgeSourceType.SYSTEM_DOC, "SYSTEM_ROLES", 
                "Các vai trò và quyền hạn trong hệ thống TCS",
                "Hệ thống TCS hỗ trợ 4 nhóm vai trò chính:\n" +
                "1. Phụ huynh / Học viên (CLIENT): Đăng tin tuyển gia sư, tìm kiếm gia sư theo môn học/khu vực/giá, nạp tiền ví, đặt cọc ký quỹ Escrow, đánh giá lớp học và gửi khiếu nại tranh chấp.\n" +
                "2. Gia sư (TUTOR): Đăng ký hồ sơ, tải bằng cấp chứng chỉ để xác minh (VERIFIED), ứng tuyển nhận lớp, dạy học và nhận tiền giải ngân từ Escrow về ví sau khi hoàn thành.\n" +
                "3. Trung tâm gia sư (TUTOR_CENTER): Đăng ký giấy phép kinh doanh, quản lý danh sách gia sư trực thuộc và phân phối lớp học.\n" +
                "4. Quản trị viên (PLATFORM_ADMIN): Kiểm duyệt hồ sơ xác minh, xử lý báo cáo vi phạm, giải quyết tranh chấp Escrow, ra quyết định phạt vi phạm và giám sát chỉ số vận hành toàn sàn.",
                Map.of("category", "ROLES", "tags", "vai_tro,phu_huynh,gia_su,admin"), stats);

        // 5. System Policies (POLICY)
        indexChunk(KnowledgeSourceType.POLICY, "POLICY_ESCROW_AND_FEES", 
                "Chính sách Ký quỹ Escrow và Phí nền tảng 10%",
                "Chính sách Ký quỹ và Phí sàn TCS:\n" +
                "- Khi phụ huynh đồng ý thuê gia sư, số tiền học phí sẽ được nạp và tạm khóa trong tài khoản ký quỹ (Escrow Transaction) của hệ thống TCS nhằm bảo vệ quyền lợi cả 2 bên.\n" +
                "- Nền tảng thu phí hoa hồng 10% (Platform Fee Rate = 10%) trên mỗi giao dịch lớp học thành công. Phí này được tự động khấu trừ khi Admin hoặc hệ thống giải ngân từ Escrow cho Gia sư.\n" +
                "- Gia sư chỉ nhận được tiền giải ngân sau khi buổi học hoặc khóa học được xác nhận hoàn tất thỏa đáng.",
                Map.of("category", "PAYMENT", "feeRate", 0.10, "tags", "escrow,phi_san,thanh_toan"), stats);

        indexChunk(KnowledgeSourceType.POLICY, "POLICY_REFUND_AND_DISPUTE", 
                "Chính sách Hoàn tiền và Giải quyết tranh chấp",
                "Quy trình Hoàn tiền (Refund) và Giải quyết tranh chấp (Dispute):\n" +
                "- Hủy lớp học trước 24 giờ kể từ lịch học: Học viên được hoàn trả 100% số tiền đặt cọc trong Escrow về ví.\n" +
                "- Hủy lớp học trước 12 giờ: Học viên được hoàn 50% tiền cọc, 50% còn lại bồi thường cho gia sư.\n" +
                "- Sau khi lớp học đã bắt đầu hoặc phát sinh khiếu nại (gia sư vắng mặt không phép, dạy sai cam kết, gian lận): Các bên có quyền mở Tranh chấp (Dispute). Admin sẽ xem xét chứng cứ và ra quyết định phân bổ 100% Escrow (hoàn tiền cho học viên, giải ngân cho gia sư, hoặc chia tỷ lệ).",
                Map.of("category", "DISPUTE", "tags", "hoan_tien,tranh_chap,refund,dispute"), stats);

        indexChunk(KnowledgeSourceType.POLICY, "POLICY_CIRCUMVENTION_PREVENTION", 
                "Chính sách Phòng chống Lách sàn và Xử phạt vi phạm",
                "Quy định Chống Lách sàn (Platform Circumvention):\n" +
                "- Nghiêm cấm mọi hành vi gia sư, trung tâm hoặc học viên chủ động gạ gẫm, chia sẻ thông tin liên lạc riêng (SĐT, Zalo, STK ngân hàng) nhằm giao dịch ngoài sàn để trốn phí nền tảng 10%.\n" +
                "- Hệ thống tự động phát hiện và nhận báo cáo vi phạm lách sàn (Report Category: PLATFORM_CIRCUMVENTION / FRAUD).\n" +
                "- Mức xử phạt: Cảnh cáo lần đầu (Warning), phạt trừ tiền ví, đình chỉ tài khoản tạm thời (Suspension) hoặc Khóa tài khoản vĩnh viễn (Ban) và phong tỏa số dư ví đối với các trường hợp cố tình tái phạm nghiêm trọng.",
                Map.of("category", "PENALTY", "tags", "lach_san,xu_phat,circumvention,gian_lan"), stats);
        
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
        chunk.setQualityScore(0.9);
        
        try {
            if (metadata != null) {
                chunk.setMetadataJson(objectMapper.writeValueAsString(metadata));
            }
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
}
