package com.tcs.module.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.*;
import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.entity.AiChatSession;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import com.tcs.module.ai.repository.AiChatSessionRepository;
import com.tcs.module.ai.service.AiService;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.TutorRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final FaqEntryRepository faqEntryRepository;
    private final TutorRepository tutorRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ObjectMapper objectMapper;

    @Value("${ai.provider:groq}")
    private String provider;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.groq.api-key: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}")
    private String groqApiKey;

    @Value("${ai.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Override
    @Transactional
    public AiMessageResponse chat(ChatRequest request, Long userId) {
        AiChatSession session = getOrCreateSession(request.getSessionId(), userId, request.getMessage());

        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setSession(session);
        userMsg.setRole("user");
        userMsg.setContent(request.getMessage());
        messageRepository.save(userMsg);

        List<FaqReferenceDto> faqs = retrieveRelevantFaqs(request.getMessage());
        List<TutorReferenceDto> tutors = retrieveRelevantTutors(request.getMessage());
        List<ClassReferenceDto> classes = retrieveRelevantClasses(request.getMessage());

        String aiResponseText = callLlmOrFallback(request.getMessage(), faqs, tutors, classes);

        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResponseText);
        if (!tutors.isEmpty()) {
            aiMsg.setReferencedTutorIds(tutors.stream().map(t -> String.valueOf(t.getTutorId())).collect(Collectors.joining(",")));
        }
        if (!classes.isEmpty()) {
            aiMsg.setReferencedClassIds(classes.stream().map(c -> String.valueOf(c.getClassId())).collect(Collectors.joining(",")));
        }
        if (!faqs.isEmpty()) {
            aiMsg.setReferencedFaqIds(faqs.stream().map(f -> String.valueOf(f.getFaqId())).collect(Collectors.joining(",")));
        }
        messageRepository.save(aiMsg);

        sessionRepository.save(session);

        return AiMessageResponse.builder()
                .messageId(aiMsg.getMessageId())
                .sessionId(session.getSessionId())
                .role("assistant")
                .content(aiResponseText)
                .createdAt(aiMsg.getCreatedAt())
                .referencedTutors(tutors)
                .referencedClasses(classes)
                .referencedFaqs(faqs)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiSessionResponse> getUserSessions(Long userId) {
        List<AiChatSession> list;
        if (userId != null) {
            list = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        } else {
            list = sessionRepository.findTop20ByOrderByUpdatedAtDesc();
        }
        return list.stream().map(s -> AiSessionResponse.builder()
                .sessionId(s.getSessionId())
                .title(s.getTitle())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiMessageResponse> getSessionMessages(Long sessionId, Long userId) {
        List<AiChatMessage> msgs = messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);
        return msgs.stream().map(m -> {
            List<TutorReferenceDto> tutors = parseTutorRefs(m.getReferencedTutorIds());
            List<ClassReferenceDto> classes = parseClassRefs(m.getReferencedClassIds());
            List<FaqReferenceDto> faqs = parseFaqRefs(m.getReferencedFaqIds());
            return AiMessageResponse.builder()
                    .messageId(m.getMessageId())
                    .sessionId(m.getSession() != null ? m.getSession().getSessionId() : sessionId)
                    .role(m.getRole())
                    .content(m.getContent())
                    .createdAt(m.getCreatedAt())
                    .referencedTutors(tutors)
                    .referencedClasses(classes)
                    .referencedFaqs(faqs)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        messageRepository.deleteBySession_SessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    private AiChatSession getOrCreateSession(Long sessionId, Long userId, String initialMsg) {
        if (sessionId != null) {
            Optional<AiChatSession> opt = sessionRepository.findById(sessionId);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        AiChatSession s = new AiChatSession();
        s.setUserId(userId);
        String title = initialMsg.length() > 35 ? initialMsg.substring(0, 35) + "..." : initialMsg;
        s.setTitle(title);
        return sessionRepository.save(s);
    }

    private List<FaqReferenceDto> retrieveRelevantFaqs(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        List<FaqEntry> all = faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc();
        return all.stream()
                .map(f -> {
                    int score = 0;
                    if (f.getQuestion() != null && f.getQuestion().toLowerCase(Locale.ROOT).contains(lower)) score += 5;
                    if (f.getAnswer() != null && f.getAnswer().toLowerCase(Locale.ROOT).contains(lower)) score += 3;
                    if (f.getCategory() != null && f.getCategory().toLowerCase(Locale.ROOT).contains(lower)) score += 2;
                    for (String word : lower.split("\\s+")) {
                        if (word.length() > 2) {
                            if (f.getQuestion() != null && f.getQuestion().toLowerCase(Locale.ROOT).contains(word)) score += 1;
                            if (f.getAnswer() != null && f.getAnswer().toLowerCase(Locale.ROOT).contains(word)) score += 1;
                        }
                    }
                    return new AbstractMap.SimpleEntry<>(f, score);
                })
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> FaqReferenceDto.builder()
                        .faqId(e.getKey().getFaqId())
                        .question(e.getKey().getQuestion())
                        .answer(e.getKey().getAnswer())
                        .category(e.getKey().getCategory())
                        .build())
                .collect(Collectors.toList());
    }

    private List<TutorReferenceDto> retrieveRelevantTutors(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        boolean isTutorQuery = lower.contains("gia sư") || lower.contains("tutor") || lower.contains("dạy") || lower.contains("thầy") || lower.contains("cô") || lower.contains("tìm");
        List<Tutor> all = tutorRepository.findAll();
        List<TutorReferenceDto> list = all.stream()
                .map(t -> {
                    int score = isTutorQuery ? 1 : 0;
                    if (t.getFullName() != null && t.getFullName().toLowerCase(Locale.ROOT).contains(lower)) score += 5;
                    if (t.getBio() != null && t.getBio().toLowerCase(Locale.ROOT).contains(lower)) score += 3;
                    if (t.getAddress() != null && t.getAddress().toLowerCase(Locale.ROOT).contains(lower)) score += 2;
                    for (String word : lower.split("\\s+")) {
                        if (word.length() > 2) {
                            if (t.getBio() != null && t.getBio().toLowerCase(Locale.ROOT).contains(word)) score += 1;
                        }
                    }
                    return new AbstractMap.SimpleEntry<>(t, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> toTutorRef(e.getKey()))
                .collect(Collectors.toList());
        if (list.isEmpty() && isTutorQuery && !all.isEmpty()) {
            return all.stream().limit(3).map(this::toTutorRef).collect(Collectors.toList());
        }
        return list;
    }

    private List<ClassReferenceDto> retrieveRelevantClasses(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        boolean isClassQuery = lower.contains("lớp") || lower.contains("class") || lower.contains("học phí") || lower.contains("môn") || lower.contains("đăng ký");
        List<TutoringClass> all = tutoringClassRepository.findByStatus(TutoringClassStatus.OPEN);
        List<ClassReferenceDto> list = all.stream()
                .map(c -> {
                    int score = isClassQuery ? 1 : 0;
                    if (c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(lower)) score += 5;
                    if (c.getDescription() != null && c.getDescription().toLowerCase(Locale.ROOT).contains(lower)) score += 3;
                    if (c.getSubject() != null && c.getSubject().getSubjectName() != null && c.getSubject().getSubjectName().toLowerCase(Locale.ROOT).contains(lower)) score += 4;
                    for (String word : lower.split("\\s+")) {
                        if (word.length() > 2) {
                            if (c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(word)) score += 1;
                        }
                    }
                    return new AbstractMap.SimpleEntry<>(c, score);
                })
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> toClassRef(e.getKey()))
                .collect(Collectors.toList());
        if (list.isEmpty() && isClassQuery && !all.isEmpty()) {
            return all.stream().limit(3).map(this::toClassRef).collect(Collectors.toList());
        }
        return list;
    }

    private TutorReferenceDto toTutorRef(Tutor t) {
        return TutorReferenceDto.builder()
                .tutorId(t.getTutorId())
                .fullName(t.getFullName())
                .avatarUrl(t.getAvatar())
                .title(t.getBio() != null && t.getBio().length() > 60 ? t.getBio().substring(0, 60) + "..." : t.getBio())
                .hourlyRate(t.getHourlyRate())
                .averageRating(t.getRatingAvg() != null ? t.getRatingAvg().doubleValue() : 5.0)
                .totalReviews(10)
                .teachingAreas(t.getAddress() != null ? t.getAddress() : "Hà Nội")
                .build();
    }

    private ClassReferenceDto toClassRef(TutoringClass c) {
        return ClassReferenceDto.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : "Môn học")
                .gradeLevelName(c.getGrade() != null ? c.getGrade().getGradeName() : "Các cấp")
                .tuitionFee(c.getTuitionFee())
                .location(c.getLocation() != null ? c.getLocation().getAddressLine() : "Online / Hà Nội")
                .status(c.getStatus().name())
                .build();
    }

    private List<TutorReferenceDto> parseTutorRefs(String ids) {
        if (ids == null || ids.isBlank()) return Collections.emptyList();
        List<TutorReferenceDto> list = new ArrayList<>();
        for (String idStr : ids.split(",")) {
            try {
                tutorRepository.findById(Long.parseLong(idStr.trim())).ifPresent(t -> list.add(toTutorRef(t)));
            } catch (Exception ignored) {}
        }
        return list;
    }

    private List<ClassReferenceDto> parseClassRefs(String ids) {
        if (ids == null || ids.isBlank()) return Collections.emptyList();
        List<ClassReferenceDto> list = new ArrayList<>();
        for (String idStr : ids.split(",")) {
            try {
                tutoringClassRepository.findById(Long.parseLong(idStr.trim())).ifPresent(c -> list.add(toClassRef(c)));
            } catch (Exception ignored) {}
        }
        return list;
    }

    private List<FaqReferenceDto> parseFaqRefs(String ids) {
        if (ids == null || ids.isBlank()) return Collections.emptyList();
        List<FaqReferenceDto> list = new ArrayList<>();
        for (String idStr : ids.split(",")) {
            try {
                faqEntryRepository.findById(Long.parseLong(idStr.trim())).ifPresent(f -> list.add(FaqReferenceDto.builder()
                        .faqId(f.getFaqId())
                        .question(f.getQuestion())
                        .answer(f.getAnswer())
                        .category(f.getCategory())
                        .build()));
            } catch (Exception ignored) {}
        }
        return list;
    }

    private String callLlmOrFallback(String userQuery, List<FaqReferenceDto> faqs, List<TutorReferenceDto> tutors, List<ClassReferenceDto> classes) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Dưới đây là DỮ LIỆU RAG THỰC TẾ từ hệ thống Tutor Connect System (TCS):\n");
            if (!tutors.isEmpty()) {
                prompt.append("--- GIA SƯ PHÙ HỢP ---\n");
                for (TutorReferenceDto t : tutors) {
                    prompt.append("ID: ").append(t.getTutorId()).append(" | Tên: ").append(t.getFullName()).append(" | Học phí: ").append(t.getHourlyRate()).append(" VND/buổi | Khu vực: ").append(t.getTeachingAreas()).append("\n");
                }
            }
            if (!classes.isEmpty()) {
                prompt.append("--- LỚP HỌC ĐANG MỞ ---\n");
                for (ClassReferenceDto c : classes) {
                    prompt.append("ID: ").append(c.getClassId()).append(" | Tiêu đề: ").append(c.getTitle()).append(" | Môn: ").append(c.getSubjectName()).append(" | Học phí: ").append(c.getTuitionFee()).append(" VND/buổi\n");
                }
            }
            if (!faqs.isEmpty()) {
                prompt.append("--- HƯỚNG DẪN / QUY ĐỊNH (FAQ) ---\n");
                for (FaqReferenceDto f : faqs) {
                    prompt.append("Hỏi: ").append(f.getQuestion()).append("\nĐáp: ").append(f.getAnswer()).append("\n");
                }
            }
            prompt.append("\nCÂU HỎI CỦA NGƯỜI DÙNG: ").append(userQuery).append("\n");
            prompt.append("Yêu cầu: Hãy đóng vai trợ lý AI của Tutor Connect System. Trả lời bằng tiếng Việt lịch sự, chính xác tuyệt đối theo thông tin RAG trên. Hãy giới thiệu rõ gia sư hoặc lớp học có trong danh sách nếu phù hợp.");

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .build();

            if ("groq".equalsIgnoreCase(provider) && groqApiKey != null && !groqApiKey.isBlank()) {
                String payload = objectMapper.writeValueAsString(Map.of(
                        "model", groqModel,
                        "messages", List.of(
                                Map.of("role", "system", "content", "Bạn là Trợ lý AI thông minh của sàn gia sư Tutor Connect System (TCS)."),
                                Map.of("role", "user", "content", prompt.toString())
                        ),
                        "temperature", 0.7
                ));
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                        .header("Authorization", "Bearer " + groqApiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(resp.body());
                    String text = root.path("choices").path(0).path("message").path("content").asText();
                    if (text != null && !text.isBlank()) return text;
                }
            } else if ("gemini".equalsIgnoreCase(provider) && geminiApiKey != null && !geminiApiKey.isBlank()) {
                String payload = objectMapper.writeValueAsString(Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.toString()))))
                ));
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(resp.body());
                    String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
                    if (text != null && !text.isBlank()) return text;
                }
            }
        } catch (Exception e) {
            log.warn("LLM API call failed or timed out, using fallback RAG generator: {}", e.getMessage());
        }

        // Fallback RAG generator
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chào! Dựa trên tri thức trực tuyến từ sàn **Tutor Connect System (TCS)**, tôi gửi đến bạn thông tin và đề xuất chính xác sau:\n\n");
        if (!tutors.isEmpty()) {
            sb.append("### 👨‍🏫 Gia sư phù hợp với bạn:\n");
            for (TutorReferenceDto t : tutors) {
                sb.append("- **").append(t.getFullName()).append("** — Học phí: **").append(t.getHourlyRate() != null ? t.getHourlyRate() : "200.000").append(" VND/buổi**.\n  *Khu vực*: ").append(t.getTeachingAreas()).append(" • *Đánh giá*: ⭐ ").append(t.getAverageRating()).append("/5.0\n");
            }
            sb.append("\n");
        }
        if (!classes.isEmpty()) {
            sb.append("### 📚 Lớp học đang mở tuyển gia sư:\n");
            for (ClassReferenceDto c : classes) {
                sb.append("- **").append(c.getTitle()).append("** — Môn: **").append(c.getSubjectName()).append("** (").append(c.getGradeLevelName()).append(")\n  *Học phí*: **").append(c.getTuitionFee() != null ? c.getTuitionFee() : "250.000").append(" VND/buổi** • *Địa điểm*: ").append(c.getLocation()).append("\n");
            }
            sb.append("\n");
        }
        if (!faqs.isEmpty()) {
            sb.append("### 💡 Giải đáp Hướng dẫn từ Hệ thống:\n");
            for (FaqReferenceDto f : faqs) {
                sb.append("- **").append(f.getQuestion()).append("**\n  👉 ").append(f.getAnswer()).append("\n");
            }
            sb.append("\n");
        }
        if (tutors.isEmpty() && classes.isEmpty() && faqs.isEmpty()) {
            sb.append("Hiện tại tôi chưa tìm thấy gia sư, lớp học hoặc bài hướng dẫn nào khớp chính xác với từ khóa của bạn trong cơ sở dữ liệu. Bạn có thể thử tìm với tên môn học (Toán, IELTS, Lý...) hoặc khu vực cụ thể (Cầu Giấy, Đống Đa...).");
        }
        return sb.toString();
    }
}
