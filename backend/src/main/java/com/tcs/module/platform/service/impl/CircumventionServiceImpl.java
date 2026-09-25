package com.tcs.module.platform.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.messaging.entity.ConversationParticipant;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import com.tcs.module.messaging.repository.MessageRepository;
import com.tcs.module.platform.dto.response.CircumventionConversationMessageResponse;
import com.tcs.module.platform.dto.response.CircumventionConversationParticipantResponse;
import com.tcs.module.platform.dto.response.CircumventionConversationResponse;
import com.tcs.module.platform.dto.request.ReviewCircumventionRequest;
import com.tcs.module.platform.dto.response.CircumventionEventResponse;
import com.tcs.module.platform.dto.response.PageCircumventionEventResponse;
import com.tcs.module.platform.entity.CircumventionEvent;
import com.tcs.module.platform.repository.CircumventionEventRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.CircumventionService;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * [UC-59] PHÁT HIỆN & GIÁM SÁT HÀNH VI LÁCH NỀN TẢNG (CIRCUMVENTION DETECTION SERVICE)
 * ============================================================================
 * * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-11
 * * Mô tả Use Case:
 *   - Phát hiện, ngăn chặn và lưu vết các hành vi cố tình giao dịch ngoài sàn (lách nền tảng).
 *   - Bảo vệ quyền lợi an toàn giao dịch qua Escrow và bảo toàn doanh thu phí nền tảng.
 * * Chức năng chính:
 *   1. Quét tin nhắn thời gian thực: Áp dụng Regex nhận diện số điện thoại, email, URL và tài khoản mạng xã hội.
 *   2. Đánh giá điểm rủi ro (Risk Score): Tính điểm vi phạm lũy tiến và gắn cờ cảnh báo khi vượt ngưỡng.
 *   3. Thẩm định bằng chứng đối soát: Truy xuất ngữ cảnh các tin nhắn trước và sau vi phạm trong cuộc trò chuyện.
 *   4. Phán quyết xử lý: Admin xác nhận vi phạm (CONFIRMED) hoặc bác bỏ (DISMISSED), liên thông ban hành chế tài.
 *   5. Ghi vết kiểm toán: Lưu lại lịch sử thẩm định của Quản trị viên vào Audit Log.
 * * Luồng xử lý chính:
 *   - Bước 1: Khi tin nhắn được gửi, hệ thống gọi `scanMessage` kiểm tra nội dung theo 4 bộ lọc Regex.
 *   - Bước 2: Nếu phát hiện từ khóa lách sàn, tạo bản ghi `CircumventionEvent` với trạng thái PENDING_REVIEW.
 *   - Bước 3: Admin mở màn hình kiểm duyệt, đọc bằng chứng và ngữ cảnh hội thoại (`getConversationContext`).
 *   - Bước 4: Admin phê duyệt (`reviewCircumventionEvent`), hệ thống cập nhật kết luận và ghi log kiểm toán.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class CircumventionServiceImpl implements CircumventionService {
    // =========================================================================
    // LUỒNG 13: PHÁT HIỆN HÀNH VI LÁCH NỀN TẢNG (CIRCUMVENTION DETECTION - UC-59)
    // =========================================================================

    private record Rule(String code, Pattern pattern, int score) {}

    // Luồng 13 - Bước 1: Khai báo 4 bộ lọc Regex nhận diện thông tin liên lạc ngoài nền tảng kèm điểm rủi ro
    private static final List<Rule> RULES = List.of(
            // Luật 1: Nhận diện Số điện thoại di động Việt Nam (+84 hoặc 0...) -> Điểm rủi ro: 80
            new Rule("PHONE", Pattern.compile("(?<!\\d)(?:\\+?84|0)(?:[ .-]?\\d){9,10}(?!\\d)"), 80),
            // Luật 2: Nhận diện Email liên hệ cá nhân -> Điểm rủi ro: 90
            new Rule("EMAIL", Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE), 90),
            // Luật 3: Nhận diện Đường dẫn liên kết bên ngoài (URL / Web Link) -> Điểm rủi ro: 70
            new Rule("URL", Pattern.compile("(?i)(?:https?://|www\\.)\\S+"), 70),
            // Luật 4: Nhận diện Tài khoản mạng xã hội (Zalo, Telegram, Facebook, Instagram) -> Điểm rủi ro: 65
            new Rule("SOCIAL", Pattern.compile("(?i)(?:zalo|telegram|facebook|fb|instagram)\\s*[:@-]?\\s*[A-Z0-9_.-]{3,}"), 65));

    private final CircumventionEventRepository repository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;
    private final AuditLogService auditLogService;

    /**
     * [UC-59] Quét kiểm duyệt tin nhắn thời gian thực để phát hiện hành vi lách sàn.
     * 
     * Luồng xử lý:
     * 1. Kiểm tra tính hợp lệ của tin nhắn và nội dung văn bản.
     * 2. Quét qua 4 luật biểu thức chính quy (SĐT, Email, URL, MXH).
     * 3. Khi phát hiện chuỗi vi phạm đầu tiên, trích xuất đoạn bằng chứng (tối đa 500 ký tự).
     * 4. Khởi tạo thực thể CircumventionEvent với điểm rủi ro tương ứng và lưu vào CSDL.
     * 
     * @param message Thực thể tin nhắn cần quét
     */
    // Luồng 13 - Bước 2: Quét kiểm duyệt tin nhắn thời gian thực (Real-time Message Inspection)
    @Override
    @Transactional
    public void inspect(Message message) {
        if (message == null || message.getContent() == null) return;
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(message.getContent());
            if (!matcher.find()) continue;
            // Nếu phát hiện vi phạm, tạo thực thể CircumventionEvent lưu vết bằng chứng
            CircumventionEvent event = new CircumventionEvent();
            event.setMessage(message);
            event.setConversation(message.getConversation());
            event.setSender(message.getSender());
            event.setMatchedRule(rule.code());
            event.setEvidence(matcher.group().substring(0, Math.min(matcher.group().length(), 500)));
            event.setRiskScore(rule.score());
            event.setCreatedAt(LocalDateTime.now());
            repository.save(event);
        }
    }

    /**
     * [UC-59] Lấy danh sách các sự kiện nghi vấn lách nền tảng có phân trang và lọc theo trạng thái.
     * 
     * Luồng xử lý:
     * 1. Chuẩn hóa tham số phân trang page và size (giới hạn tối đa 100 bản ghi/trang).
     * 2. Truy vấn CSDL theo trạng thái (hoặc lấy tất cả nếu để trống), sắp xếp thời gian tạo mới nhất.
     * 3. Ánh xạ danh sách CircumventionEvent sang CircumventionEventResponse và đóng gói trang kết quả.
     * 
     * @param status Trạng thái cần lọc (PENDING, CONFIRMED, DISMISSED hoặc null)
     * @param page Số trang truy vấn
     * @param size Kích thước trang
     * @return PageCircumventionEventResponse danh sách sự kiện phân trang
     */
    @Override
    @Transactional(readOnly = true)
    public PageCircumventionEventResponse list(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<CircumventionEvent> result = status == null || status.isBlank()
                ? repository.findAllByOrderByCreatedAtDesc(pageable)
                : repository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        return PageCircumventionEventResponse.builder().content(result.map(this::toResponse).getContent())
                .page(result.getNumber()).size(result.getSize()).totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages()).build();
    }

    /**
     * [UC-59] Truy xuất bằng chứng ngữ cảnh cuộc hội thoại chứa tin nhắn vi phạm lách sàn.
     * 
     * Luồng xử lý:
     * 1. Tìm thực thể CircumventionEvent theo eventId, ném ResourceNotFoundException nếu không thấy.
     * 2. Lấy 100 tin nhắn mới nhất trong cuộc trò chuyện, đảo ngược theo thứ tự thời gian tăng dần.
     * 3. Tập hợp danh sách thành viên tham gia hội thoại (participants).
     * 4. Đánh dấu cờ (flagged = true) cho tin nhắn cụ thể kích hoạt cảnh báo vi phạm.
     * 5. Ghi nhật ký kiểm toán hành động xem bằng chứng vào AuditLogService.
     * 
     * @param eventId ID sự kiện nghi vấn
     * @return CircumventionConversationResponse đối tượng chứa toàn bộ ngữ cảnh tin nhắn bằng chứng
     * @throws ResourceNotFoundException nếu không tìm thấy sự kiện
     */
    @Override
    @Transactional
    public CircumventionConversationResponse getConversationEvidence(Long eventId) {
        CircumventionEvent event = repository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện: " + eventId));
        Long conversationId = event.getConversation().getConversationId();
        Page<Message> messagePage = messageRepository.findByConversation_ConversationIdOrderBySentAtDesc(
                conversationId, PageRequest.of(0, 100));
        List<Message> orderedMessages = new ArrayList<>(messagePage.getContent());
        Collections.reverse(orderedMessages);

        List<CircumventionConversationParticipantResponse> participants = conversationParticipantRepository
                .findByConversation_ConversationId(conversationId).stream()
                .map(ConversationParticipant::getUser)
                .map(user -> CircumventionConversationParticipantResponse.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .build())
                .toList();
        List<CircumventionConversationMessageResponse> messages = orderedMessages.stream()
                .map(message -> CircumventionConversationMessageResponse.builder()
                        .messageId(message.getMessageId())
                        .senderId(message.getSender().getUserId())
                        .senderEmail(message.getSender().getEmail())
                        .content(message.getContent())
                        .sentAt(message.getSentAt())
                        .flagged(message.getMessageId().equals(event.getMessage().getMessageId()))
                        .build())
                .toList();

        auditLogService.record("VIEW_CIRCUMVENTION_CONVERSATION", "CircumventionEvent", eventId, null,
                java.util.Map.of("conversationId", conversationId));
        return CircumventionConversationResponse.builder()
                .eventId(eventId)
                .conversationId(conversationId)
                .conversationType(event.getConversation().getType())
                .conversationName(event.getConversation().getName())
                .flaggedMessageId(event.getMessage().getMessageId())
                .participants(participants)
                .messages(messages)
                .hasMore(messagePage.hasNext())
                .build();
    }

    /**
     * [UC-59] Thẩm định và cập nhật kết luận xử lý sự kiện vi phạm lách nền tảng.
     * 
     * Luồng xử lý:
     * 1. Tìm sự kiện theo eventId và kiểm tra trạng thái hiện tại phải là PENDING.
     * 2. Xác định danh tính Quản trị viên đang thực hiện thẩm định từ AuthHelper.
     * 3. Cập nhật trạng thái mới (CONFIRMED hoặc DISMISSED), ghi chú thẩm định và mốc thời gian xét duyệt.
     * 4. Lưu sự kiện vào CSDL và ghi nhận vết kiểm toán REVIEW_CIRCUMVENTION.
     * 
     * @param eventId ID sự kiện cần thẩm định
     * @param request Yêu cầu thẩm định chứa trạng thái phê duyệt và ghi chú
     * @return CircumventionEventResponse kết quả sự kiện sau khi cập nhật
     * @throws ResourceNotFoundException nếu không tìm thấy sự kiện hoặc Quản trị viên
     * @throws IllegalStateException nếu sự kiện đã được xét duyệt trước đó
     */
    @Override
    @Transactional
    public CircumventionEventResponse review(Long eventId, ReviewCircumventionRequest request) {
        CircumventionEvent event = repository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện: " + eventId));
        if (!"PENDING".equals(event.getStatus())) throw new IllegalStateException("Sự kiện đã được duyệt.");
        User reviewer = userRepository.findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quản trị viên."));
        event.setStatus(request.getStatus());
        event.setReviewNote(request.getNote() == null ? null : request.getNote().trim());
        event.setReviewedBy(reviewer);
        event.setReviewedAt(LocalDateTime.now());
        CircumventionEvent saved = repository.save(event);
        auditLogService.record("REVIEW_CIRCUMVENTION", "CircumventionEvent", eventId, null, toResponse(saved));
        return toResponse(saved);
    }

    private CircumventionEventResponse toResponse(CircumventionEvent event) {
        return CircumventionEventResponse.builder().eventId(event.getEventId())
                .messageId(event.getMessage().getMessageId()).conversationId(event.getConversation().getConversationId())
                .senderId(event.getSender().getUserId()).senderEmail(event.getSender().getEmail())
                .matchedRule(event.getMatchedRule()).evidence(event.getEvidence()).riskScore(event.getRiskScore())
                .status(event.getStatus()).reviewNote(event.getReviewNote()).reviewedAt(event.getReviewedAt())
                .createdAt(event.getCreatedAt()).build();
    }
}
