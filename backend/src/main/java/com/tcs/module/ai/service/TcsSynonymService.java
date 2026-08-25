package com.tcs.module.ai.service;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TCS-specific Synonym and Query Expansion Service.
 * Expands queries with domain-specific synonyms to improve retrieval recall.
 */
@Service
public class TcsSynonymService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TcsSynonymService.class);

    // TCS domain-specific synonym dictionary
    private static final Map<String, List<String>> TCS_SYNONYMS = Map.ofEntries(
        // Core Roles
        Map.entry("gia sư", List.of("thầy", "cô", "giáo viên", "tutor", "giáo viên dạy kèm", "người dạy")),
        Map.entry("học sinh", List.of("em", "học viên", "con", "trẻ", "student")),
        Map.entry("phụ huynh", List.of("bố mẹ", "cha mẹ", "người giám hộ", "guardian", "parent")),
        Map.entry("trung tâm", List.of("center", "trung tâm gia sư", "tổ chức", "trường")),
        
        // Finance Terms
        Map.entry("học phí", List.of("tiền học", "tuition fee", "chi phí học", "phí học", "giá học", "tiền dạy")),
        Map.entry("phí sàn", List.of("phí nền tảng", "platform fee", "phí hệ thống", "10%", "phí dịch vụ")),
        Map.entry("rút tiền", List.of("withdraw", "chuyển khoản", "thanh toán lương", "nhận tiền", "rút về ngân hàng")),
        Map.entry("nạp tiền", List.of("topup", "nạp ví", "deposit", "chuyển tiền vào", "add money")),
        Map.entry("ví", List.of("wallet", "tài khoản", "số dư", "balance")),
        Map.entry("escrow", List.of("ký quỹ", "ký gửi", "bảo lãnh", "đặt cọc")),
        Map.entry("hoàn tiền", List.of("refund", "hoàn lại", "trả lại tiền", "đền bù")),
        
        // Contract & Verification
        Map.entry("hợp đồng", List.of("contract", "thỏa thuận", "agreement", "ký kết")),
        Map.entry("otp", List.of("mã xác thực", "mã OTP", "verification code", "one-time password")),
        Map.entry("xác minh", List.of("verify", "verification", "xác thực", "kiểm tra", "duyệt")),
        Map.entry("cccd", List.of("căn cước", "cmnd", "chứng minh", "ID card", "identity card")),
        Map.entry("bằng cấp", List.of("certificate", "chứng chỉ", "degree", "diploma", "văn bằng")),
        
        // Marketplace
        Map.entry("tìm", List.of("search", "kiếm", "tra cứu", "xem", "looking for", "cần")),
        Map.entry("lớp học", List.of("lớp", "class", "tutoring class", "khóa học", "course")),
        Map.entry("môn học", List.of("subject", "môn", "bộ môn", "học phần")),
        Map.entry("khối", List.of("grade", "lớp", "cấp học", "level")),
        Map.entry("địa điểm", List.of("location", "nơi", "khu vực", "vị trí", "chỗ", "area", "place")),
        Map.entry("lịch", List.of("schedule", "thời gian", "buổi học", "calendar", "thời khóa biểu")),
        
        // Quality & Reputation
        Map.entry("đánh giá", List.of("review", "rating", "nhận xét", "feedback", "comment")),
        Map.entry("chất lượng", List.of("quality", "tốt", "giỏi", "uy tín", "xuất sắc")),
        Map.entry("kinh nghiệm", List.of("experience", "exp", "năm dạy", "experienced", "kỹ năng")),
        
        // Communication
        Map.entry("nhắn tin", List.of("message", "chat", "tin nhắn", "liên hệ", "gửi tin", "contact")),
        Map.entry("thông báo", List.of("notification", "noti", "announce", "báo", "alert")),
        Map.entry("hỗ trợ", List.of("support", "giúp đỡ", "help", "assistance", "ticket")),
        
        // Actions
        Map.entry("đăng ký", List.of("register", "signup", "sign up", "tạo tài khoản")),
        Map.entry("đăng nhập", List.of("login", "sign in", "log in", "vào hệ thống")),
        Map.entry("tạo", List.of("create", "thêm", "add", "new", "khởi tạo")),
        Map.entry("xóa", List.of("delete", "remove", "hủy", "cancel")),
        Map.entry("sửa", List.of("edit", "update", "modify", "change", "cập nhật")),
        Map.entry("xem", List.of("view", "see", "look", "check", "tra cứu"))
    );

    // Reverse mapping: synonym -> canonical term
    private static final Map<String, String> REVERSE_SYNONYMS = buildReverseSynonymMap();

    private static Map<String, String> buildReverseSynonymMap() {
        Map<String, String> reverse = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : TCS_SYNONYMS.entrySet()) {
            String canonical = entry.getKey();
            for (String synonym : entry.getValue()) {
                reverse.put(synonym.toLowerCase(), canonical);
            }
            reverse.put(canonical.toLowerCase(), canonical); // Self-mapping
        }
        return reverse;
    }

    /**
     * Expand query with TCS-specific synonyms.
     * Example: "Tìm thầy Toán" → "Tìm|kiếm|search thầy|gia sư|tutor Toán"
     */
    public String expandQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return originalQuery;
        }

        String lowerQuery = originalQuery.toLowerCase();
        Set<String> expandedTerms = new LinkedHashSet<>();
        
        // Split into words
        String[] words = lowerQuery.split("\\s+");
        
        for (String word : words) {
            boolean expanded = false;
            
            // Check if this word matches any canonical term or synonym
            for (Map.Entry<String, List<String>> entry : TCS_SYNONYMS.entrySet()) {
                String canonical = entry.getKey();
                List<String> synonyms = entry.getValue();
                
                // If word matches canonical term
                if (word.contains(canonical)) {
                    // Add original + top 2 synonyms
                    expandedTerms.add(word);
                    expandedTerms.addAll(synonyms.stream().limit(2).collect(Collectors.toList()));
                    expanded = true;
                    break;
                }
                
                // If word matches any synonym
                for (String syn : synonyms) {
                    if (word.contains(syn)) {
                        expandedTerms.add(word);
                        expandedTerms.add(canonical);
                        expandedTerms.addAll(synonyms.stream().limit(1).collect(Collectors.toList()));
                        expanded = true;
                        break;
                    }
                }
                
                if (expanded) break;
            }
            
            // If no expansion found, keep original
            if (!expanded) {
                expandedTerms.add(word);
            }
        }

        // Join with spaces for natural language processing
        String expanded = String.join(" ", expandedTerms);
        
        if (!expanded.equals(lowerQuery)) {
            log.debug("Query expansion: '{}' → '{}'", originalQuery, expanded);
        }
        
        return expanded;
    }

    /**
     * Normalize query by converting synonyms to canonical forms.
     * Example: "Tìm thầy dạy Toán" → "Tìm gia sư dạy Toán"
     */
    public String normalizeQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return originalQuery;
        }

        String result = originalQuery;
        String lowerResult = result.toLowerCase();
        
        // Replace synonyms with canonical terms (longest match first)
        List<String> sortedSynonyms = REVERSE_SYNONYMS.keySet().stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .collect(Collectors.toList());
        
        for (String synonym : sortedSynonyms) {
            if (lowerResult.contains(synonym)) {
                String canonical = REVERSE_SYNONYMS.get(synonym);
                // Preserve case by replacing in original string
                result = result.replaceAll("(?i)" + synonym, canonical);
                lowerResult = result.toLowerCase();
            }
        }
        
        if (!result.equals(originalQuery)) {
            log.debug("Query normalization: '{}' → '{}'", originalQuery, result);
        }
        
        return result;
    }

    /**
     * Get all synonyms for a given term.
     */
    public List<String> getSynonyms(String term) {
        if (term == null) return List.of();
        
        String lowerTerm = term.toLowerCase();
        
        // Check if it's a canonical term
        if (TCS_SYNONYMS.containsKey(lowerTerm)) {
            return TCS_SYNONYMS.get(lowerTerm);
        }
        
        // Check if it's a synonym
        String canonical = REVERSE_SYNONYMS.get(lowerTerm);
        if (canonical != null && TCS_SYNONYMS.containsKey(canonical)) {
            List<String> result = new ArrayList<>();
            result.add(canonical);
            result.addAll(TCS_SYNONYMS.get(canonical));
            return result;
        }
        
        return List.of(term);
    }

    /**
     * Check if two queries are semantically similar based on synonyms.
     */
    public boolean areSemanticallyEquivalent(String query1, String query2) {
        if (query1 == null || query2 == null) return false;
        
        String normalized1 = normalizeQuery(query1);
        String normalized2 = normalizeQuery(query2);
        
        return normalized1.equalsIgnoreCase(normalized2);
    }
}
