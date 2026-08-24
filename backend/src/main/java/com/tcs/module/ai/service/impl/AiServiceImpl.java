package com.tcs.module.ai.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.ai.constants.AiConstants;
import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.*;
import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.entity.AiChatSession;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import com.tcs.module.ai.repository.AiChatSessionRepository;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import com.tcs.module.ai.service.*;
import com.tcs.module.ai.service.provider.AiClassSearchContextProvider;
import com.tcs.module.ai.service.provider.AiPublicPlatformStatsContextProvider;
import com.tcs.module.ai.service.provider.AiTutorFinanceContextProvider;
import com.tcs.module.ai.service.provider.AiTutorSearchContextProvider;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production-Grade Orchestrator for the AI Assistant, Contextual RAG, and Intent Routing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final AiKnowledgeChunkRepository chunkRepository;

    private final AiConversationContextService contextService;
    private final AiQueryRewriteService rewriteService;
    private final AiIntentService intentService;
    private final AiRetrievalService retrievalService;
    private final AiRerankService rerankService;
    private final ContextualChunkRetriever contextualChunkRetriever;
    private final AiPromptBuilderService promptBuilderService;
    private final AiAnswerEvaluatorService evaluatorService;
    private final AiCapabilityRouter capabilityRouter;
    private final AiFallbackService fallbackService;
    private final AiFinanceGuardService financeGuardService;
    private final AiReferenceCardService referenceCardService;
    private final AiHallucinationGuardService hallucinationGuardService;
    private final AiResponseBuilderService responseBuilderService;
    private final ContentSafetyFilter contentSafetyFilter;
    private final ConversationContextService conversationContextService;
    private final AiSemanticCacheService semanticCacheService;
    private final TcsSynonymService synonymService;
    private final UserPreferenceService userPreferenceService;

    private final AiTicketContextProvider ticketContextProvider;
    private final AiAdminDashboardContextProvider dashboardContextProvider;
    private final AiTutorSearchContextProvider tutorSearchContextProvider;
    private final AiClassSearchContextProvider classSearchContextProvider;
    private final AiPublicPlatformStatsContextProvider platformStatsContextProvider;
    private final AiTutorFinanceContextProvider tutorFinanceContextProvider;

    private final com.tcs.module.ai.service.provider.AiProviderRouter aiProviderRouter;

    @Value("${ai.provider.max-output-tokens:700}")
    private int maxOutputTokens = 700;

    @Value("${ai.contextual-window.enabled:true}")
    private boolean contextualWindowEnabled = true;

    @Value("${ai.contextual-window.size:2}")
    private int contextualWindowSize = 2;

    @Override
    @Transactional
    public AiMessageResponse chat(ChatRequest request, Long userId) {
        String userRole = resolveUserRole(userId);
        AiChatSession session = getOrCreateSession(request.getSessionId(), userId, request.getMessage());

        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setSession(session);
        userMsg.setRole("user");
        userMsg.setContent(request.getMessage());
        messageRepository.save(userMsg);

        // 0. Content Safety & Crisis Fast-Path Filter
        ContentSafetyFilter.SafetyCheckResult safetyCheck = contentSafetyFilter.checkQuery(request.getMessage());
        if (!safetyCheck.isSafe()) {
            return handleSafetyBlockResponse(session, safetyCheck);
        }

        List<AiChatMessage> history = contextService.getHistory(session.getSessionId());

        // 0.4. Follow-Up Resolution & Query Expansion
        String followUpExpandedQuery = resolveFollowUp(session.getSessionId(), request.getMessage());
        String expandedQuery = synonymService != null ? synonymService.expandQuery(followUpExpandedQuery) : followUpExpandedQuery;
        if (expandedQuery == null || expandedQuery.isBlank()) {
            expandedQuery = followUpExpandedQuery != null ? followUpExpandedQuery : request.getMessage();
        }
        String normalizedQuery = synonymService != null ? synonymService.normalizeQuery(followUpExpandedQuery) : followUpExpandedQuery;

        // 0.5. Semantic Cache Check
        Optional<AiSemanticCacheService.CachedResponse> cachedResponse = 
            semanticCacheService.get(request.getMessage(), userRole);
        if (cachedResponse.isPresent()) {
            return handleSemanticCacheHit(session, request.getMessage(), cachedResponse.get());
        }

        // 1. 3-Tier Classification
        AiIntentService.DetailedIntentResult classification = intentService.classifyAndExtractDetailed(
            expandedQuery, session.getSessionId(), userId);
        AiDomain domain = classification.domain();
        AiSubIntent subIntent = classification.subIntent();
        AiIntent legacyIntent = classification.legacyIntent();
        Map<String, String> entities = classification.entities();
        if (userId != null) {
            userPreferenceService.updateFromInteraction(userId, entities, request.getMessage());
            entities = userPreferenceService.enrichWithPreferences(userId, entities);
        }
        String suggestedRoute = classification.suggestedRoute();

        // 2. Safety & Conversational fast-path (Level 0)
        AiFallbackService.FallbackResult safetyResult = fallbackService.checkLevel0Safety(subIntent);
        if (safetyResult != null) {
            return handleSafetyFastPath(session, domain, subIntent, legacyIntent, safetyResult);
        }

        // 2.5. Out-of-Scope Gating
        if (domain == AiDomain.OUT_OF_SCOPE || subIntent == AiSubIntent.OUT_OF_SCOPE) {
            return handleOutOfScopeResponse(session, domain, subIntent, legacyIntent, entities, request.getMessage());
        }

        // 3. Query Rewriting for follow-up conversational context
        AiQueryRewriteService.RewriteResult rewritten = rewriteService.rewriteQuery(history, expandedQuery, legacyIntent);
        String effectiveRewrittenQuery = rewritten != null && rewritten.rewrittenQuery() != null ? rewritten.rewrittenQuery() : expandedQuery;

        // 4. Capability Policy & Role Verification
        AiCapabilityRouter.CapabilityPolicy policy = capabilityRouter.getPolicy(domain, subIntent);
        if (policy != null && policy.requireAuth() && !policy.allowedRoles().isEmpty() && !policy.allowedRoles().contains(userRole)) {
            return handleAuthPolicyResponse(session, domain, subIntent, legacyIntent, policy);
        }

        // 5. Context Retrieval (Unified Vector RAG + Database Providers)
        List<AiSourceResponse> allSources = retrieveAllSources(domain, subIntent, legacyIntent, entities, effectiveRewrittenQuery, userRole, userId, request.getMessage());

        // 5.5. Contextual Window Enrichment
        if (contextualWindowEnabled && !allSources.isEmpty() && (domain == AiDomain.MARKETPLACE || domain == AiDomain.CATALOG_FAQ)) {
            try {
                enrichSourcesWithContextWindow(allSources, contextualWindowSize);
            } catch (Exception e) {
                log.warn("Contextual enrichment failed, continuing with original chunks: {}", e.getMessage());
            }
        }

        boolean retrievalUnavailable = allSources.isEmpty();

        // 6. Grounding Evaluation
        AiAnswerEvaluatorService.EvaluatedAnswer evaluation = evaluatorService.evaluate(legacyIntent, allSources);

        // 7. Reference Card Hydration
        AiReferenceCardService.ReferenceCards cards = referenceCardService.hydrateCards(domain, subIntent, allSources);
        List<TutorReferenceDto> tutors = cards.tutors();
        List<ClassReferenceDto> classes = cards.classes();
        List<FaqReferenceDto> faqs = cards.faqs();

        // 8. Finance Access Guard & Response Synthesis
        String aiResponseText = financeGuardService.checkFinanceAccess(domain, request.getMessage(), userRole, userId);
        if (aiResponseText == null) {
            String finalPrompt = promptBuilderService.buildPrompt(request.getMessage(), effectiveRewrittenQuery, legacyIntent, userRole, allSources, retrievalUnavailable);
            aiResponseText = callLlm(finalPrompt, history, evaluation.answerMode(), legacyIntent, allSources);
        }

        // 9. Hallucination Post-Processing
        aiResponseText = hallucinationGuardService.applyGuards(
            aiResponseText, domain, subIntent, entities, tutors, classes, allSources, request.getMessage(), userRole, userId
        );

        // 10. Persist Message & Context
        AiChatMessage aiMsg = persistAssistantMessage(session, aiResponseText, tutors, classes, faqs);
        persistSessionContext(session.getSessionId(), domain, subIntent, entities, request.getMessage(), tutors, classes, faqs);

        // 11. Semantic Caching
        cacheGroundedResponse(request.getMessage(), normalizedQuery, aiResponseText, legacyIntent, domain, subIntent, evaluation, tutors, classes, faqs, userRole);

        return responseBuilderService.build(
            aiMsg.getMessageId(), session.getSessionId(), aiResponseText,
            legacyIntent.name(), domain.name(), subIntent.name(),
            suggestedRoute != null ? suggestedRoute : (policy != null ? policy.deepLinkRoute() : "/help"),
            List.of(), evaluation.answerMode(), evaluation.confidenceScore(), evaluation.confidenceLevel(),
            evaluation.sourceCount(), evaluation.groundingStatus(), allSources, tutors, classes, faqs,
            rewritten != null && rewritten.isFollowUp() ? rewritten.rewrittenQuery() : null,
            rewritten != null && rewritten.isFollowUp(), evaluation.evaluationNotes(), evaluation.warningCode()
        );
    }

    private String resolveUserRole(Long userId) {
        if (userId == null) return "GUEST";
        if (platformAdminRepository.findByUser_UserId(userId).isPresent()) return "PLATFORM_ADMIN";
        if (tutorRepository.findByUser_UserId(userId).isPresent()) return "TUTOR";
        if (tutorCenterRepository.findByUser_UserId(userId).isPresent()) return "TUTOR_CENTER";
        if (clientRepository.findByUser_UserId(userId).isPresent()) return "CLIENT";
        return "USER";
    }

    private String resolveFollowUp(Long sessionId, String message) {
        if (conversationContextService != null) {
            String resolved = conversationContextService.resolveFollowUpQuery(sessionId, message);
            if (resolved != null && !resolved.isBlank()) {
                return resolved;
            }
        }
        return message;
    }

    private void enrichSourcesWithContextWindow(List<AiSourceResponse> sources, int windowSize) {
        if (sources == null || sources.isEmpty() || chunkRepository == null || contextualChunkRetriever == null) return;

        List<AiRetrievalService.RetrievalResult> retrievalResults = convertToRetrievalResults(sources);
        if (retrievalResults.isEmpty()) return;

        List<AiKnowledgeChunk> allChunks = chunkRepository.findByActiveTrue();
        List<ContextualChunkRetriever.ContextualChunk> contextualResults = 
            contextualChunkRetriever.retrieveWithContext(retrievalResults, allChunks, windowSize);

        for (int i = 0; i < Math.min(sources.size(), contextualResults.size()); i++) {
            AiSourceResponse source = sources.get(i);
            ContextualChunkRetriever.ContextualChunk contextual = contextualResults.get(i);

            if (contextual.mergedContext() != null && !contextual.mergedContext().isBlank() && 
                ((contextual.precedingChunks() != null && !contextual.precedingChunks().isEmpty()) || 
                 (contextual.succeedingChunks() != null && !contextual.succeedingChunks().isEmpty()))) {
                source.setSnippet(contextual.mergedContext());
                log.debug("Enriched chunk {} with contextual document window", source.getSourceId());
            }
        }
    }

    private List<AiRetrievalService.RetrievalResult> convertToRetrievalResults(List<AiSourceResponse> sources) {
        List<AiRetrievalService.RetrievalResult> list = new ArrayList<>();
        if (chunkRepository == null) return list;

        for (AiSourceResponse s : sources) {
            if (s.getSourceId() != null) {
                try {
                    Long cId = Long.parseLong(s.getSourceId());
                    chunkRepository.findById(cId).ifPresent(chunk -> 
                        list.add(new AiRetrievalService.RetrievalResult(chunk, s.getFinalScore()))
                    );
                } catch (NumberFormatException ignored) {}
            }
        }
        return list;
    }

    private AiMessageResponse handleSafetyBlockResponse(AiChatSession session, ContentSafetyFilter.SafetyCheckResult safetyCheck) {
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(safetyCheck.suggestedResponse());
        messageRepository.save(aiMsg);
        sessionRepository.save(session);

        String suggestedRoute = "PRIVACY".equals(safetyCheck.reason()) ? "/platform/users" : "/help";
        List<String> options = "PRIVACY".equals(safetyCheck.reason())
            ? List.of("Quy định bảo mật thông tin", "Liên hệ ban quản trị")
            : List.of("Tìm gia sư (/tim-gia-su)", "Xem lớp học (/lop-hoc)", "Liên hệ hỗ trợ (/support/tickets)");

        return responseBuilderService.build(
            aiMsg.getMessageId(), session.getSessionId(), safetyCheck.suggestedResponse(),
            AiIntent.OUT_OF_SCOPE.name(), AiDomain.CONVERSATION_SAFETY.name(), AiSubIntent.OUT_OF_SCOPE.name(),
            suggestedRoute, options, "SAFETY_FILTER", 1.0, "HIGH", 0, "SAFETY_FILTERED",
            List.of(), List.of(), List.of(), List.of(), null, false, null, null
        );
    }

    private AiMessageResponse handleSemanticCacheHit(AiChatSession session, String query, AiSemanticCacheService.CachedResponse cache) {
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(cache.content());
        aiMsg.setReferencedTutorIds(cache.referencedTutorIds());
        aiMsg.setReferencedClassIds(cache.referencedClassIds());
        aiMsg.setReferencedFaqIds(cache.referencedFaqIds());
        messageRepository.save(aiMsg);
        sessionRepository.save(session);
        
        log.info("Returned cached response for query: '{}'", query);

        List<TutorReferenceDto> tutors = referenceCardService.hydrateTutorsByIds(parseIds(cache.referencedTutorIds()));
        List<ClassReferenceDto> classes = referenceCardService.hydrateClassesByIds(parseIds(cache.referencedClassIds()));
        List<FaqReferenceDto> faqs = referenceCardService.hydrateFaqsByIds(parseIds(cache.referencedFaqIds()));

        return responseBuilderService.build(
            aiMsg.getMessageId(), session.getSessionId(), cache.content(),
            cache.intent(), cache.domain(), cache.subIntent(), null, List.of(),
            "CACHED", cache.confidenceScore() != null ? cache.confidenceScore() : 1.0, "HIGH",
            cache.sourceCount() != null ? cache.sourceCount() : 0, "CACHED",
            List.of(), tutors, classes, faqs, null, false, null, null
        );
    }

    private AiMessageResponse handleSafetyFastPath(AiChatSession session, AiDomain domain, AiSubIntent subIntent, AiIntent legacyIntent, AiFallbackService.FallbackResult safetyResult) {
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(safetyResult.message());
        messageRepository.save(aiMsg);
        sessionRepository.save(session);

        return responseBuilderService.build(
            aiMsg.getMessageId(), session.getSessionId(), safetyResult.message(),
            legacyIntent.name(), domain.name(), subIntent.name(), safetyResult.suggestedRoute(),
            safetyResult.clarificationOptions(), "DIRECT", 1.0, "HIGH", 0, "GROUNDED",
            List.of(), List.of(), List.of(), List.of(), null, false, null, null
        );
    }

    private AiMessageResponse handleAuthPolicyResponse(AiChatSession session, AiDomain domain, AiSubIntent subIntent, AiIntent legacyIntent, AiCapabilityRouter.CapabilityPolicy policy) {
        String requiredRoleDesc = policy.allowedRoles().contains("PLATFORM_ADMIN") ? "Quản trị viên (PLATFORM_ADMIN)" :
                                 policy.allowedRoles().contains("TUTOR") ? "Gia sư" : "Người dùng hợp lệ";
        AiFallbackService.FallbackResult authFallback = fallbackService.getLevel4AuthRoleRequired(requiredRoleDesc, policy.deepLinkRoute());

        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(authFallback.message());
        messageRepository.save(aiMsg);
        sessionRepository.save(session);

        return responseBuilderService.build(
            aiMsg.getMessageId(), session.getSessionId(), authFallback.message(),
            legacyIntent.name(), domain.name(), subIntent.name(), authFallback.suggestedRoute(),
            authFallback.clarificationOptions(), "FALLBACK", 0.9, "HIGH", 0, "PERMISSION_RESTRICTED",
            List.of(), List.of(), List.of(), List.of(), null, false, null, null
        );
    }

    private AiMessageResponse handleOutOfScopeResponse(AiChatSession session, AiDomain domain, AiSubIntent subIntent, AiIntent legacyIntent, Map<String, String> entities, String query) {
        String outOfScopeMsg = "Xin lỗi, tôi chỉ hỗ trợ các câu hỏi liên quan đến hệ thống Tutor Connect System (TCS) như: tìm gia sư, tìm lớp học, quy trình thanh toán, hợp đồng, và các chính sách nền tảng. Câu hỏi này nằm ngoài phạm vi hỗ trợ của tôi.";
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(outOfScopeMsg);
        messageRepository.save(aiMsg);
        sessionRepository.save(session);
        conversationContextService.saveContext(session.getSessionId(), domain, subIntent, entities, query);

        return responseBuilderService.build(
            aiMsg.getMessageId(), session.getSessionId(), outOfScopeMsg,
            legacyIntent.name(), domain.name(), subIntent.name(), "/help",
            List.of("Tìm gia sư (/tim-gia-su)", "Xem lớp học (/lop-hoc)", "Câu hỏi thường gặp (/help)"),
            "FALLBACK_OUT_OF_SCOPE", 0.0, "NONE", 0, "OUT_OF_SCOPE",
            List.of(), List.of(), List.of(), List.of(), null, false, null, null
        );
    }

    private List<AiSourceResponse> retrieveAllSources(AiDomain domain, AiSubIntent subIntent, AiIntent legacyIntent, Map<String, String> entities, String rewrittenQuery, String userRole, Long userId, String rawMessage) {
        List<AiSourceResponse> allSources = new ArrayList<>();
        String lowerQuery = rawMessage.toLowerCase();

        if (domain != AiDomain.CONVERSATION_SAFETY && domain != AiDomain.OUT_OF_SCOPE) {
            try {
                List<AiRetrievalService.RetrievalResult> vectorResults = retrievalService.retrieve(rewrittenQuery, userRole, userId);
                List<AiSourceResponse> rerankedVectorResults = rerankService.rerank(vectorResults, new AiIntentService.IntentResultWithEntities(legacyIntent, 0.9, entities), rewrittenQuery);
                allSources.addAll(rerankedVectorResults);

                enrichSourcesWithBusinessContext(allSources, domain, subIntent, entities, userRole, userId, lowerQuery);
            } catch (Exception e) {
                log.warn("AI Context Retrieval failed: {}", e.getMessage());
            }
        }

        allSources = deduplicateAndLimitSources(allSources, AiConstants.MAX_SOURCES_PER_QUERY);
        allSources.removeIf(s -> s.getFinalScore() < AiConstants.MIN_RETRIEVAL_SCORE);
        return allSources;
    }

    private void enrichSourcesWithBusinessContext(List<AiSourceResponse> allSources, AiDomain domain, AiSubIntent subIntent, Map<String, String> entities, String userRole, Long userId, String lowerQuery) {
        if ((subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) && allSources.stream().noneMatch(s -> "TUTOR".equals(s.getSourceType()))) {
            allSources.addAll(tutorSearchContextProvider.searchTutors(entities));
        } else if ((subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) && allSources.stream().noneMatch(s -> "CLASS".equals(s.getSourceType()))) {
            allSources.addAll(classSearchContextProvider.searchClasses(entities));
        } else if (subIntent == AiSubIntent.PLATFORM_STATS) {
            allSources.addAll(0, platformStatsContextProvider.getPlatformStats());
        } else if (domain == AiDomain.PLATFORM_ADMIN) {
            allSources.addAll(dashboardContextProvider.getDashboardContext(userRole));
        } else if (domain == AiDomain.FINANCE_WALLET) {
            if (lowerQuery.contains("lương của tôi") || lowerQuery.contains("thu nhập của tôi") || lowerQuery.contains("tiền kiếm được của tôi") || lowerQuery.contains("ví của tôi") || lowerQuery.contains("số dư của tôi")) {
                allSources.addAll(tutorFinanceContextProvider.getTutorFinanceContext(userRole, userId));
            }
        } else if (domain == AiDomain.MESSAGING_TICKET || domain == AiDomain.TRUST_SAFETY) {
            allSources.addAll(ticketContextProvider.getTicketContext(userRole, userId));
        }
    }

    private AiChatMessage persistAssistantMessage(AiChatSession session, String aiResponseText, List<TutorReferenceDto> tutors, List<ClassReferenceDto> classes, List<FaqReferenceDto> faqs) {
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResponseText);

        if (!tutors.isEmpty()) aiMsg.setReferencedTutorIds(tutors.stream().map(t -> String.valueOf(t.getTutorId())).collect(Collectors.joining(",")));
        if (!classes.isEmpty()) aiMsg.setReferencedClassIds(classes.stream().map(c -> String.valueOf(c.getClassId())).collect(Collectors.joining(",")));
        if (!faqs.isEmpty()) aiMsg.setReferencedFaqIds(faqs.stream().map(f -> String.valueOf(f.getFaqId())).collect(Collectors.joining(",")));
        messageRepository.save(aiMsg);
        sessionRepository.save(session);
        return aiMsg;
    }

    private void persistSessionContext(Long sessionId, AiDomain domain, AiSubIntent subIntent, Map<String, String> entities, String message, List<TutorReferenceDto> tutors, List<ClassReferenceDto> classes, List<FaqReferenceDto> faqs) {
        List<Long> tutorIdList = tutors.stream().map(TutorReferenceDto::getTutorId).filter(Objects::nonNull).toList();
        List<Long> classIdList = classes.stream().map(ClassReferenceDto::getClassId).filter(Objects::nonNull).toList();
        List<Long> faqIdList = faqs.stream().map(FaqReferenceDto::getFaqId).filter(Objects::nonNull).toList();
        conversationContextService.saveContext(
            sessionId, domain, subIntent, entities, message, tutorIdList, classIdList, faqIdList, entities.getOrDefault("subject", null)
        );
    }

    private void cacheGroundedResponse(String message, String normalizedQuery, String aiResponseText, AiIntent legacyIntent, AiDomain domain, AiSubIntent subIntent, AiAnswerEvaluatorService.EvaluatedAnswer evaluation, List<TutorReferenceDto> tutors, List<ClassReferenceDto> classes, List<FaqReferenceDto> faqs, String userRole) {
        if (evaluation.confidenceScore() >= AiConstants.HIGH_CONFIDENCE_THRESHOLD && 
            !"FALLBACK".equals(evaluation.answerMode()) && 
            !"SAFETY_FILTER".equals(evaluation.answerMode()) &&
            domain != AiDomain.OUT_OF_SCOPE &&
            subIntent != AiSubIntent.PLATFORM_STATS) {
            
            semanticCacheService.put(
                message, normalizedQuery, aiResponseText,
                legacyIntent.name(), domain.name(), subIntent.name(),
                evaluation.confidenceScore(), evaluation.sourceCount(),
                !tutors.isEmpty() ? tutors.stream().map(t -> String.valueOf(t.getTutorId())).collect(Collectors.joining(",")) : null,
                !classes.isEmpty() ? classes.stream().map(c -> String.valueOf(c.getClassId())).collect(Collectors.joining(",")) : null,
                !faqs.isEmpty() ? faqs.stream().map(f -> String.valueOf(f.getFaqId())).collect(Collectors.joining(",")) : null,
                userRole, (double[]) null
            );
        }
    }

    private List<AiSourceResponse> deduplicateAndLimitSources(List<AiSourceResponse> sources, int limit) {
        if (sources == null || sources.isEmpty()) return new ArrayList<>();
        Map<String, AiSourceResponse> map = new LinkedHashMap<>();
        for (AiSourceResponse s : sources) {
            String key = s.getSourceType() + "-" + s.getSourceId();
            if (!map.containsKey(key)) {
                map.put(key, s);
            }
        }
        return map.values().stream().limit(limit).collect(Collectors.toList());
    }

    private String callLlm(String prompt, List<AiChatMessage> history, String answerMode, AiIntent intent, List<AiSourceResponse> sources) {
        try {
            var chatReq = new com.tcs.module.ai.service.provider.AiProviderChatRequest(
                """
                Bạn là Trợ lý AI của hệ thống kết nối gia sư Tutor Connect System (TCS).
                
                QUY TẮC QUAN TRỌNG:
                1. Trả lời PHẢI dựa 100% trên Context được cung cấp
                2. KHÔNG BAO GIỜ bịa tên người, số liệu, hoặc thông tin không có trong Context
                3. Nếu Context không đủ thông tin → Nói rõ "Tôi không tìm thấy thông tin về..."
                4. Khi trích dẫn thông tin → Cite nguồn một cách tự nhiên (VD: "Theo chính sách của TCS...")
                5. Trả lời ngắn gọn, chính xác, thân thiện bằng tiếng Việt
                """,
                prompt,
                maxOutputTokens,
                0.3
            );
            var resp = aiProviderRouter.chat(chatReq);
            if (resp != null && resp.content() != null && !resp.content().isBlank()) {
                return resp.content();
            }
        } catch (Exception e) {
            log.warn("AI chat LLM invocation failed across providers: {}", e.getMessage());
        }

        if (intent == AiIntent.FIND_TUTOR || intent == AiIntent.FIND_CLASS || intent == AiIntent.CREATE_CLASS) {
            if (sources == null || sources.isEmpty()) {
                return "Hiện tại hệ thống chưa tìm thấy dữ liệu phù hợp với yêu cầu tìm kiếm của bạn. Bạn vui lòng thử điều chỉnh lại môn học, khối lớp hoặc khu vực tìm kiếm nhé!";
            }
        }

        if (intent == AiIntent.PLATFORM_STATS) {
            if (sources != null) {
                for (AiSourceResponse s : sources) {
                    if ("SYSTEM".equals(s.getSourceType()) || "STATS".equals(s.getSourceId())) {
                        if (s.getSnippet() != null && !s.getSnippet().isBlank()) {
                            return s.getSnippet();
                        }
                    }
                }
            }
        }

        if (sources != null && !sources.isEmpty()) {
            for (AiSourceResponse s : sources) {
                if (s.getFinalScore() >= AiConstants.MIN_REFERENCE_CARD_SCORE && 
                    ("FAQ".equals(s.getSourceType()) || "POLICY".equals(s.getSourceType()) || "SYSTEM_DOC".equals(s.getSourceType()) || "SYSTEM".equals(s.getSourceType()))) {
                    if (s.getSnippet() != null && !s.getSnippet().isBlank()) {
                        return s.getSnippet();
                    }
                }
            }
        }

        return "Xin chào! Tôi là Trợ lý AI của Tutor Connect System (TCS). Tôi có thể hỗ trợ bạn tìm kiếm gia sư, tham khảo lớp học, tra cứu học phí và giải đáp các quy định của hệ thống. Bạn có câu hỏi nào cụ thể về gia sư hoặc lớp học không ạ?";
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiSessionResponse> getUserSessions(Long userId) {
        if (userId == null) return List.of();
        List<AiChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return sessions.stream().map(s -> AiSessionResponse.builder()
                .sessionId(s.getSessionId())
                .title(s.getTitle())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiMessageResponse> getSessionMessages(Long sessionId, Long userId) {
        if (sessionId == null) return List.of();
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        if (session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to view messages in this session");
        }

        List<AiChatMessage> messages = messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);

        Set<Long> allTutorIds = new LinkedHashSet<>();
        Set<Long> allClassesIds = new LinkedHashSet<>();
        Set<Long> allFaqIds = new LinkedHashSet<>();

        for (AiChatMessage msg : messages) {
            allTutorIds.addAll(parseIds(msg.getReferencedTutorIds()));
            allClassesIds.addAll(parseIds(msg.getReferencedClassIds()));
            allFaqIds.addAll(parseIds(msg.getReferencedFaqIds()));
        }

        Map<Long, TutorReferenceDto> tutorMap = (referenceCardService != null && !allTutorIds.isEmpty())
                ? referenceCardService.hydrateTutorsByIds(allTutorIds).stream().collect(Collectors.toMap(TutorReferenceDto::getTutorId, t -> t, (a, b) -> a))
                : Collections.emptyMap();
        Map<Long, ClassReferenceDto> classMap = (referenceCardService != null && !allClassesIds.isEmpty())
                ? referenceCardService.hydrateClassesByIds(allClassesIds).stream().collect(Collectors.toMap(ClassReferenceDto::getClassId, c -> c, (a, b) -> a))
                : Collections.emptyMap();
        Map<Long, FaqReferenceDto> faqMap = (referenceCardService != null && !allFaqIds.isEmpty())
                ? referenceCardService.hydrateFaqsByIds(allFaqIds).stream().collect(Collectors.toMap(FaqReferenceDto::getFaqId, f -> f, (a, b) -> a))
                : Collections.emptyMap();

        return messages.stream().map(msg -> {
            List<TutorReferenceDto> msgTutors = parseIds(msg.getReferencedTutorIds()).stream()
                    .map(tutorMap::get).filter(Objects::nonNull).toList();
            List<ClassReferenceDto> msgClasses = parseIds(msg.getReferencedClassIds()).stream()
                    .map(classMap::get).filter(Objects::nonNull).toList();
            List<FaqReferenceDto> msgFaqs = parseIds(msg.getReferencedFaqIds()).stream()
                    .map(faqMap::get).filter(Objects::nonNull).toList();

            return AiMessageResponse.builder()
                    .messageId(msg.getMessageId())
                    .sessionId(sessionId)
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .createdAt(msg.getCreatedAt())
                    .referencedTutors(msgTutors)
                    .referencedClasses(msgClasses)
                    .referencedFaqs(msgFaqs)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        if (sessionId == null) return;
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));

        if (session.getUserId() != null && !session.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to delete this session");
        }

        messageRepository.deleteBySession_SessionId(sessionId);
        sessionRepository.delete(session);
    }

    private AiChatSession getOrCreateSession(Long sessionId, Long userId, String initialMessage) {
        if (sessionId != null) {
            Optional<AiChatSession> sessionOpt = sessionRepository.findById(sessionId);
            if (sessionOpt.isPresent()) {
                AiChatSession session = sessionOpt.get();
                if (session.getUserId() != null && !session.getUserId().equals(userId)) {
                    log.warn("Session isolation triggered: sessionId {} owned by userId {} accessed by userId {}",
                            sessionId, session.getUserId(), userId);
                    return createNewSession(userId, initialMessage);
                }
                if (session.getUserId() == null && userId != null) {
                    session.setUserId(userId);
                    sessionRepository.save(session);
                }
                return session;
            }
        }

        return createNewSession(userId, initialMessage);
    }

    private AiChatSession createNewSession(Long userId, String initialMessage) {
        String title = (initialMessage != null && !initialMessage.isBlank())
                ? (initialMessage.length() > 30 ? initialMessage.substring(0, 30) + "..." : initialMessage)
                : "New Conversation";

        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        return sessionRepository.save(session);
    }

    private List<Long> parseIds(String idString) {
        if (idString == null || idString.isBlank()) return List.of();
        return Arrays.stream(idString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try { return Long.parseLong(s); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
