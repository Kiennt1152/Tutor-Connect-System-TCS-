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

@Service
@RequiredArgsConstructor
public class CircumventionServiceImpl implements CircumventionService {
    private record Rule(String code, Pattern pattern, int score) {}
    private static final List<Rule> RULES = List.of(
            new Rule("PHONE", Pattern.compile("(?<!\\d)(?:\\+?84|0)(?:[ .-]?\\d){9,10}(?!\\d)"), 80),
            new Rule("EMAIL", Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE), 90),
            new Rule("URL", Pattern.compile("(?i)(?:https?://|www\\.)\\S+"), 70),
            new Rule("SOCIAL", Pattern.compile("(?i)(?:zalo|telegram|facebook|fb|instagram)\\s*[:@-]?\\s*[A-Z0-9_.-]{3,}"), 65));

    private final CircumventionEventRepository repository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void inspect(Message message) {
        if (message == null || message.getContent() == null) return;
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(message.getContent());
            if (!matcher.find()) continue;
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
