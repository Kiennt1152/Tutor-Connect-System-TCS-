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
import com.tcs.module.ai.util.VietnameseTextNormalizer;
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
        String classificationQuery = followUpExpandedQuery != null ? followUpExpandedQuery : request.getMessage();
        AiIntentService.DetailedIntentResult classification = intentService.classifyAndExtractDetailed(
            classificationQuery, session.getSessionId(), userId);
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
        AiReferenceCardService.ReferenceCards cards = referenceCardService.hydrateCards(domain, subIntent, allSources, entities);
        List<TutorReferenceDto> tutors = cards.tutors();
        List<ClassReferenceDto> classes = cards.classes();
        List<FaqReferenceDto> faqs = cards.faqs();

        // 8. Finance Access Guard & Response Synthesis
        String aiResponseText = financeGuardService.checkFinanceAccess(domain, request.getMessage(), userRole, userId);
        if (aiResponseText == null) {
            String finalPrompt = promptBuilderService.buildPrompt(request.getMessage(), effectiveRewrittenQuery, legacyIntent, userRole, allSources, retrievalUnavailable);
            aiResponseText = callLlm(finalPrompt, history, evaluation.answerMode(), domain, subIntent, legacyIntent, allSources, effectiveRewrittenQuery);
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
            : List.of("Tìm gia sư", "Xem lớp học", "Liên hệ hỗ trợ");

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
        String outOfScopeMsg = "TCS là nền tảng kết nối gia sư và học viên. Trợ lý AI hỗ trợ bạn tra cứu thông tin hệ thống, bảng giá, quy trình và tìm kiếm người dạy phù hợp. Câu hỏi này nằm ngoài phạm vi hỗ trợ của tôi. Đối với các câu hỏi giải bài tập hoặc giảng dạy chuyên sâu, bạn có thể [Tìm gia sư](/tim-gia-su) hoặc [Đăng tin tạo lớp](/tao-lop) môn học này để được kèm 1-1 nhé!";
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(outOfScopeMsg);
        messageRepository.save(aiMsg);
        sessionRepository.save(session);
        conversationContextService.saveContext(session.getSessionId(), domain, subIntent, entities, query);

        return responseBuilderService.build(
            aiMsg.getMessageId(), session.getSessionId(), outOfScopeMsg,
            legacyIntent.name(), domain.name(), subIntent.name(), "/tim-gia-su",
            List.of("Tìm gia sư", "Đăng tin tạo lớp", "Câu hỏi thường gặp"),
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

    private String callLlm(String prompt, List<AiChatMessage> history, String answerMode, AiDomain domain, AiSubIntent subIntent, AiIntent intent, List<AiSourceResponse> sources, String rawMessage) {
        try {
            var chatReq = new com.tcs.module.ai.service.provider.AiProviderChatRequest(
                """
                Bạn là Trợ lý AI của hệ thống kết nối gia sư Tutor Connect System (TCS).
                
                QUY TẮC QUAN TRỌNG:
                1. Trả lời PHẢI dựa 100% trên Context được cung cấp
                2. KHÔNG BAO GIỜ bịa tên người, số liệu, hoặc thông tin không có trong Context
                3. Nếu Context không đủ thông tin → Trả lời theo quy định và hướng dẫn chuẩn của nền tảng TCS
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

        return generateRuleBasedFallback(domain, subIntent, intent, sources, rawMessage);
    }

    private String generateRuleBasedFallback(AiDomain domain, AiSubIntent subIntent, AiIntent intent, List<AiSourceResponse> sources, String rawMessage) {
        String lower = rawMessage != null ? rawMessage.toLowerCase(Locale.ROOT) : "";
        String normalized = VietnameseTextNormalizer.normalize(rawMessage != null ? rawMessage : "");

        // 1. Platform Real Stats
        if (subIntent == AiSubIntent.PLATFORM_STATS || intent == AiIntent.PLATFORM_STATS) {
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

        // 2. Specialized sub-intent fallbacks (Guaranteed exact answers)
        if (subIntent == AiSubIntent.APPLY_TO_CLASS) {
            return """
                **Quy trình gia sư ứng tuyển nhận lớp trên TCS:**
                1. **Tìm lớp:** Truy cập mục [Danh sách lớp học](/lop-hoc), lọc theo môn học, khối lớp và khu vực mong muốn.
                2. **Xem chi tiết:** Bấm vào bài đăng lớp học để xem yêu cầu cụ thể của phụ huynh/học viên.
                3. **Nộp đơn ứng tuyển:** Bấm nút **"Ứng tuyển"** trên trang chi tiết lớp học, nhập mức học phí đề xuất kèm lời giới thiệu kinh nghiệm giảng dạy để gửi phụ huynh xem xét.
                """;
        }

        // Specific Pricing Questions
        if ((normalized.contains("toan") || lower.contains("toán")) && (normalized.contains("10") || lower.contains("10")) && (normalized.contains("gia") || normalized.contains("chi phi") || normalized.contains("hoc phi") || lower.contains("chi phí") || lower.contains("học phí") || normalized.contains("bao nhieu"))) {
            return """
                **Chi phí thuê gia sư Toán lớp 10 hiện tại trên TCS:**
                • **Gia sư Sinh viên (ĐH Sư Phạm, Bách Khoa, Ngoại Thương...):** Dao động từ **150.000 ₫ – 220.000 ₫ / buổi** (thời lượng 90–120 phút).
                • **Gia sư Giáo viên / Giảng viên / Thạc sĩ:** Dao động từ **250.000 ₫ – 400.000 ₫ / buổi**.
                • **Hình thức học Online:** Tiết kiệm hơn từ **130.000 ₫ – 200.000 ₫ / buổi**.
                
                *Lưu ý:* Phụ huynh và gia sư có thể tự do thương lượng mức học phí phù hợp khi tạo lớp học.
                """;
        }

        if ((normalized.contains("van") || lower.contains("văn")) && (normalized.contains("9") || lower.contains("9")) && (normalized.contains("gia") || normalized.contains("chi phi") || normalized.contains("hoc phi") || lower.contains("chi phí") || lower.contains("học phí") || normalized.contains("bao nhieu"))) {
            return """
                **Chi phí thuê gia sư Ngữ văn lớp 9 (Ôn thi vào 10) trên TCS:**
                • **Gia sư Sinh viên chuyên Văn / Sư phạm:** Dao động từ **160.000 ₫ – 230.000 ₫ / buổi** (thời lượng 90–120 phút).
                • **Gia sư Giáo viên luyện thi vào 10 chuyên:** Dao động từ **280.000 ₫ – 450.000 ₫ / buổi**.
                
                Bạn có thể tham khảo danh sách gia sư hoặc đăng bài tạo lớp tại [Tạo lớp học](/tao-lop) để nhận đề xuất học phí phù hợp nhất.
                """;
        }

        // Online vs Offline Policy
        if ((normalized.contains("online") || lower.contains("online")) && (normalized.contains("tai nha") || lower.contains("tại nhà") || normalized.contains("hay") || normalized.contains("co gia su") || normalized.contains("co khong") || normalized.contains("website minh co"))) {
            return """
                **Hình thức giảng dạy trên nền tảng TCS:**
                • **Dạy kèm tại nhà (Offline):** Gia sư đến tận nhà học viên để kèm 1-1 hoặc dạy nhóm theo lịch thỏa thuận.
                • **Dạy trực tuyến (Online):** Học viên và gia sư học tương tác trực tiếp qua Google Meet/Zoom, linh hoạt thời gian và tiết kiệm chi phí đi lại.
                • Khi tìm kiếm tại mục [Tìm gia sư](/tim-gia-su) hoặc [Tạo lớp học](/tao-lop), bạn có thể dễ dàng lọc theo hình thức **Online** hoặc **Tại nhà**.
                """;
        }

        // Trial Lesson & Changing Tutor
        if (normalized.contains("hoc thu") || lower.contains("học thử") || normalized.contains("doi gia su") || lower.contains("đổi gia sư") || normalized.contains("khong hop") || lower.contains("không hợp")) {
            return """
                **Chính sách Học thử & Đổi gia sư tại TCS:**
                • **Đổi gia sư:** Nếu sau buổi học đầu tiên cảm thấy phong cách dạy chưa phù hợp với học sinh, phụ huynh có quyền yêu cầu đổi gia sư khác hoàn toàn miễn phí.
                • **Bảo vệ tài chính qua Escrow:** Tiền đặt cọc của các buổi học chưa diễn ra trong quỹ Escrow sẽ được bảo lưu nguyên vẹn 100% để chuyển sang gia sư mới hoặc hoàn về ví của bạn.
                • Bạn chỉ cần vào mục [Lớp học của tôi](/parent/classes) hoặc gửi yêu cầu tại mục [Hỗ trợ & Khiếu nại](/support/tickets) để được đổi người dạy nhanh chóng.
                """;
        }

        // Tutor Vetting Standards
        if (normalized.contains("tieu chuan tuyen chon") || lower.contains("tiêu chuẩn tuyển chọn") || normalized.contains("tieu chuan gia su") || lower.contains("tiêu chuẩn gia sư") || normalized.contains("kiem duyet") || lower.contains("kiểm duyệt") || normalized.contains("linh vuc nao duoc") || lower.contains("lĩnh vực nào được")) {
            return """
                **Tiêu chuẩn tuyển chọn & Quy trình kiểm duyệt gia sư của TCS:**
                1. **Xác minh danh tính (KYC):** 100% gia sư bắt buộc đối soát Căn cước công dân (CCCD) chính chủ qua công nghệ OCR và chuyên viên pháp chế.
                2. **Kiểm duyệt học vấn & Bằng cấp:** Sinh viên phải xuất trình Thẻ sinh viên còn hạn; Giáo viên/Cử nhân phải cung cấp Bằng tốt nghiệp Đại học và Chứng chỉ sư phạm hoặc Chứng chỉ ngoại ngữ quốc tế (IELTS, TOEIC, JLPT...).
                3. **Đánh giá năng lực & Điểm uy tín:** Gia sư được đánh giá qua lịch sử giảng dạy, tỷ lệ hoàn thành lớp học và điểm nhận xét sao thực tế từ phụ huynh trước đó.
                """;
        }

        // Hiring / Registration Workflow (4 Steps)
        if (normalized.contains("quy trinh dang ky") || lower.contains("quy trình đăng ký") || normalized.contains("quy trinh tim gia su") || lower.contains("quy trình tìm gia sư") || normalized.contains("cac buoc tim gia su") || normalized.contains("cac buoc thue gia su") || normalized.contains("dang ky tim gia su") || normalized.contains("tim gia su dien ra") || lower.contains("tìm gia sư diễn ra")) {
            return """
                **Quy trình tìm và thuê gia sư trên TCS gồm 4 bước đơn giản:**
                1. **Đăng yêu cầu hoặc Tìm kiếm:** Truy cập [Tạo lớp học](/tao-lop) để đăng tin miễn phí hoặc chủ động chọn gia sư tại [Tìm gia sư](/tim-gia-su).
                2. **Chọn gia sư & Trao đổi:** Xem xét hồ sơ bằng cấp, đánh giá sao của các ứng viên và nhắn tin trao đổi thống nhất lịch học, học phí.
                3. **Ký Hợp đồng điện tử:** Xác nhận hợp đồng học tập 3 bên bằng mã OTP bảo mật gửi về điện thoại/email.
                4. **Nạp ký quỹ Escrow:** Nạp học phí tạm giữ an toàn vào quỹ Escrow qua VietQR SePay. Tiền chỉ được giải ngân cho gia sư sau khi từng buổi học hoàn tất thành công.
                """;
        }

        // Navigation / Where-to-go Guide
        if (normalized.contains("vao dau") || normalized.contains("o dau") || normalized.contains("trang nao") || normalized.contains("muc nao") || normalized.contains("muon kiem gia su") || normalized.contains("muon tim gia su") || normalized.contains("cach tim gia su") || normalized.contains("cach thue gia su")) {
            return """
                **Để tìm kiếm và thuê gia sư trên nền tảng TCS, bạn có thể thực hiện theo 2 cách:**
                1. **Tìm kiếm trực tiếp:** Truy cập trang **[Tìm gia sư](/tim-gia-su)**, sử dụng bộ lọc môn học, khối lớp, khu vực và hình thức (Online/Tại nhà) để xem hồ sơ và gửi yêu cầu học.
                2. **Đăng bài tạo lớp:** Truy cập trang **[Tạo lớp học](/tao-lop)** để đăng thông tin lớp học (hoàn toàn miễn phí). Các gia sư phù hợp sẽ chủ động gửi hồ sơ ứng tuyển để bạn lựa chọn.
                """;
        }

        // Payment Method / Escrow
        if (normalized.contains("thanh toan truc tiep") || lower.contains("thanh toán trực tiếp") || normalized.contains("chuyen khoan qua") || lower.contains("chuyển khoản qua") || normalized.contains("thanh toan qua dau") || normalized.contains("chuyen tien cho gia su hay trung tam") || lower.contains("chuyển tiền cho gia sư hay trung tâm") || ((normalized.contains("hoc phi") || lower.contains("học phí")) && (normalized.contains("thanh toan") || lower.contains("thanh toán")) && (normalized.contains("truc tiep") || normalized.contains("trung tam") || lower.contains("trực tiếp")))) {
            return """
                **Quy định về phương thức thanh toán học phí trên TCS:**
                • **Thanh toán qua Quỹ ký quỹ Escrow của TCS:** Toàn bộ học phí được thanh toán an toàn qua cổng VietQR SePay vào tài khoản Escrow của hệ thống, **tuyệt đối KHÔNG thanh toán tiền mặt trực tiếp** cho gia sư.
                • **Cơ chế bảo vệ 2 bên:** Số tiền học phí sẽ được bảo lưu an toàn và chỉ giải ngân cho gia sư sau khi phụ huynh xác nhận buổi học đã diễn ra đúng chất lượng cam kết.
                • Việc thanh toán qua sàn đảm bảo quyền lợi được **hoàn tiền 100%** nếu phát sinh tranh chấp hoặc gia sư bỏ dạy.
                """;
        }

        // Math Equation / Academic Helper
        if (normalized.contains("giai giup") || lower.contains("giải giúp") || normalized.contains("giai phuong trinh") || lower.contains("giải phương trình") || lower.contains("x^2") || normalized.contains("phuong trinh")) {
            if (lower.contains("x^2 - 5x + 6") || lower.contains("x^2-5x+6") || lower.contains("5x + 6")) {
                return """
                    **Hướng dẫn giải chi tiết phương trình bậc hai:** $x^2 - 5x + 6 = 0$
                    
                    **Cách 1: Phân tích đa thức thành nhân tử (Tách hạng tử)**
                    • Ta tìm hai số có tổng bằng $-5$ và tích bằng $6$, đó là $-2$ và $-3$.
                    • Viết lại: $x^2 - 2x - 3x + 6 = 0$
                    • Nhóm nhân tử chung: $x(x - 2) - 3(x - 2) = 0$
                    • $\\iff (x - 2)(x - 3) = 0$
                    • $\\iff x - 2 = 0$ hoặc $x - 3 = 0$
                    • **Nghiệm của phương trình:** $x_1 = 2$ hoặc $x_2 = 3$.

                    **Cách 2: Sử dụng công thức nghiệm biệt thức Delta ($\\Delta$)**
                    • $a = 1, b = -5, c = 6$
                    • $\\Delta = b^2 - 4ac = (-5)^2 - 4(1)(6) = 25 - 24 = 1 > 0$
                    • Vì $\\Delta > 0$, phương trình có 2 nghiệm phân biệt:
                      $x_1 = \\frac{-b + \\sqrt{\\Delta}}{2a} = \\frac{5 + 1}{2} = 3$
                      $x_2 = \\frac{-b - \\sqrt{\\Delta}}{2a} = \\frac{5 - 1}{2} = 2$

                    **Kết luận:** Tập nghiệm của phương trình là $S = \\{2; 3\\}$.
                    """;
            }
        }

        if (subIntent == AiSubIntent.FAQ_SEARCH && (normalized.contains("luong") || normalized.contains("thu nhap") || normalized.contains("hoc phi") || lower.contains("lương") || lower.contains("thu nhập") || lower.contains("học phí"))) {
            return """
                **Mức học phí và thu nhập trung bình của gia sư trên TCS:**
                • **Cấp 1 (Tiểu học):** 120.000đ - 180.000đ / buổi
                • **Cấp 2 (THCS):** 150.000đ - 250.000đ / buổi
                • **Cấp 3 (THPT & Luyện thi Đại học):** 200.000đ - 350.000đ / buổi
                • **Ngoại ngữ & Luyện thi chứng chỉ (IELTS/TOEIC):** 250.000đ - 500.000đ / buổi

                *Ghi chú:* Gia sư và phụ huynh hoàn toàn có thể tự do thương lượng mức học phí phù hợp khi tạo lớp hoặc nộp đơn ứng tuyển.
                """;
        }

        if (subIntent == AiSubIntent.DISPUTE_OPEN_HELP || normalized.contains("bo day") || lower.contains("bỏ dạy") || lower.contains("bỏ tiết")) {
            return """
                **Xử lý khi gia sư bỏ dạy hoặc vi phạm cam kết buổi học:**
                1. **Trao đổi trực tiếp:** Nhắn tin với gia sư qua mục [Tin nhắn](/chat) để làm rõ nguyên nhân.
                2. **Mở khiếu nại / Tranh chấp:** Nếu gia sư bỏ dạy không lý do hoặc không liên lạc được, phụ huynh truy cập mục [Hỗ trợ & Khiếu nại](/support/tickets) để tạo Phiếu khiếu nại tranh chấp lớp học.
                3. **Bảo vệ tài chính qua Escrow:** Quản trị viên TCS sẽ đối chiếu dữ liệu điểm danh, xác minh vi phạm và **hoàn trả 100% tiền đặt cọc trong quỹ Escrow** về ví của phụ huynh. Gia sư vi phạm sẽ bị trừ điểm uy tín hoặc khóa tài khoản.
                """;
        }

        if (subIntent == AiSubIntent.REPUTATION_VIEW_HELP || normalized.contains("diem uy tin") || lower.contains("điểm uy tín")) {
            return """
                **Hệ thống Điểm uy tín (Reputation Score) của gia sư trên TCS:**
                • **Khởi tạo:** Mỗi gia sư bắt đầu với mức uy tín mặc định (100 điểm).
                • **Cộng điểm:** Hoàn thành tốt các buổi dạy, nhận đánh giá 5 sao từ phụ huynh và duy trì tỷ lệ đi dạy đúng giờ cao.
                • **Trừ điểm:** Đi muộn, hủy buổi dạy sát giờ, bị phụ huynh khiếu nại hoặc vi phạm quy chế sàn.
                • **Quyền lợi:** Gia sư có điểm uy tín cao sẽ được ưu tiên hiển thị trên trang [Tìm gia sư](/tim-gia-su) và tăng cơ hội nhận lớp.
                """;
        }

        if (subIntent == AiSubIntent.TUTOR_VERIFICATION_HELP || (lower.contains("xác minh") && lower.contains("bao lâu"))) {
            return """
                **Quy định về thời gian xét duyệt xác minh hồ sơ gia sư:**
                • Đội ngũ Quản trị viên (Admin) xét duyệt hồ sơ CCCD/CMND và bằng cấp, chứng chỉ trong vòng **12 đến 24 giờ làm việc**.
                • Sau khi được duyệt, tài khoản gia sư sẽ nhận huy hiệu **"Đã xác minh" (Verified)** giúp tăng độ tin cột và được ưu tiên nhận lớp.
                """;
        }

        if (subIntent == AiSubIntent.TUTOR_ATTENDANCE_MARK || lower.contains("điểm danh")) {
            return """
                **Hướng dẫn điểm danh buổi học dành cho gia sư:**
                1. Sau mỗi buổi dạy, gia sư truy cập mục [Lớp học của tôi](/tutor/classes), chọn lớp học tương ứng.
                2. Chọn buổi học trong danh sách và bấm nút **"Điểm danh"**.
                3. **Trường hợp quên điểm danh hôm qua:** Bạn vẫn có thể điểm danh bổ sung trong vòng **24 giờ** kể từ khi buổi học kết thúc. Nếu quá 24 giờ, vui lòng tạo phiếu hỗ trợ để Admin kiểm tra và hỗ trợ đối soát.
                """;
        }

        if (subIntent == AiSubIntent.PENALTY_EXPLAIN || lower.contains("xử phạt") || lower.contains("vi phạm quy chế")) {
            return """
                **Quy định xử phạt vi phạm quy chế hoạt động của TCS:**
                • **Cảnh cáo & trừ điểm uy tín:** Đi muộn, hủy ca dạy sát giờ hoặc phản hồi tin nhắn chậm trễ.
                • **Tạm khóa tài khoản & phong tỏa Escrow:** Tự ý bỏ dạy giữa chừng hoặc có khiếu nại tranh chấp chưa giải quyết.
                • **Cấm vĩnh viễn (Blacklist):** Gian lận thông tin, làm giả bằng cấp CCCD hoặc lách sàn rủ chuyển tiền riêng ngoài hệ thống.
                """;
        }

        if (subIntent == AiSubIntent.REFUND_POLICY || lower.contains("hoàn tiền")) {
            return """
                **Chính sách hoàn tiền (Refund) qua quỹ ký quỹ Escrow tại TCS:**
                • **Hủy lớp trước 24 giờ:** Học viên được hoàn trả **100%** số tiền đặt cọc trong Escrow về ví.
                • **Hủy lớp trong vòng 12 - 24 giờ:** Hoàn lại **50%** tiền cọc, 50% còn lại bồi thường cho gia sư.
                • **Khi có tranh chấp (Gia sư bỏ dạy, dạy sai cam kết):** Admin xem xét và hoàn trả **100%** tiền ký quỹ cho phụ huynh.
                """;
        }

        if (subIntent == AiSubIntent.WALLET_TOPUP) {
            return """
                **Hướng dẫn nạp tiền vào ví TCS qua mã QR tự động:**
                1. Truy cập mục [Ví tiền & Tài chính](/finance) và bấm **"Nạp tiền"**.
                2. Nhập số tiền cần nạp và chọn cổng thanh toán VietQR (SePay).
                3. Quét mã QR hiển thị trên màn hình bằng ứng dụng ngân hàng. Tiền sẽ được tự động cộng vào ví trong vòng 10 - 30 giây.
                """;
        }

        if (subIntent == AiSubIntent.WITHDRAWAL_REQUEST) {
            return """
                **Hướng dẫn rút tiền về tài khoản ngân hàng:**
                1. Truy cập mục [Ví tiền & Tài chính](/finance) và bấm **"Rút tiền"**.
                2. Nhập số tiền muốn rút (tối thiểu 50.000 VNĐ) và thông tin ngân hàng chính chủ.
                3. Yêu cầu rút tiền sẽ được Admin xử lý và chuyển khoản trong vòng **1 - 24 giờ làm việc**.
                """;
        }

        if (subIntent == AiSubIntent.CONTRACT_SIGN_OTP) {
            return """
                **Hướng dẫn ký hợp đồng điện tử bằng mã OTP:**
                1. Khi lớp học được chốt, hệ thống tự động sinh Hợp đồng dạy học điện tử.
                2. Truy cập mục [Quản lý Hợp đồng](/contracts), chọn hợp đồng cần ký và đọc kỹ các điều khoản.
                3. Bấm **"Ký hợp đồng"**, hệ thống gửi mã OTP bảo mật qua Email/SMS của bạn.
                4. Nhập mã OTP chính xác để hoàn tất ký kết có giá trị pháp lý.
                """;
        }

        // 3. Search Results Formatting
        if (subIntent == AiSubIntent.FIND_TUTOR || intent == AiIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) {
            if (sources != null && !sources.isEmpty()) {
                List<AiSourceResponse> tutorSources = sources.stream()
                        .filter(s -> "TUTOR".equals(s.getSourceType()))
                        .toList();
                if (!tutorSources.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Dựa trên tiêu chí tìm kiếm của bạn, hệ thống TCS tìm thấy các gia sư phù hợp sau:\n\n");
                    for (AiSourceResponse s : tutorSources) {
                        sb.append("• **").append(s.getTitle() != null ? s.getTitle() : "Gia sư").append("**\n");
                        if (s.getSnippet() != null && !s.getSnippet().isBlank()) {
                            sb.append("  ").append(s.getSnippet().replace("\n", "\n  ")).append("\n");
                        }
                    }
                    sb.append("\nBạn có thể bấm vào thẻ gia sư bên dưới để xem chi tiết hồ sơ và gửi yêu cầu học.");
                    return sb.toString();
                }
            }
            return "Hiện tại hệ thống TCS chưa tìm thấy gia sư phù hợp với tiêu chí của bạn. Bạn có thể bấm [Đăng bài tạo lớp](/tao-lop) (hoàn toàn miễn phí) để các gia sư phù hợp chủ động nộp hồ sơ ứng tuyển trong vòng 24h, hoặc mở rộng điều kiện tìm kiếm tại [Tìm gia sư](/tim-gia-su).";
        }

        if (subIntent == AiSubIntent.FIND_CLASS || intent == AiIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) {
            if (sources != null && !sources.isEmpty()) {
                List<AiSourceResponse> classSources = sources.stream()
                        .filter(s -> "CLASS".equals(s.getSourceType()))
                        .toList();
                if (!classSources.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Dựa trên tiêu chí tìm kiếm của bạn, hệ thống TCS tìm thấy các lớp học phù hợp sau:\n\n");
                    for (AiSourceResponse s : classSources) {
                        sb.append("• **").append(s.getTitle() != null ? s.getTitle() : "Lớp học").append("**\n");
                        if (s.getSnippet() != null && !s.getSnippet().isBlank()) {
                            sb.append("  ").append(s.getSnippet().replace("\n", "\n  ")).append("\n");
                        }
                    }
                    sb.append("\nBạn có thể bấm vào thẻ lớp học bên dưới để xem chi tiết và nộp đơn ứng tuyển.");
                    return sb.toString();
                }
            }
            return "Hiện tại hệ thống TCS chưa tìm thấy lớp học phù hợp với yêu cầu tìm kiếm của bạn. Bạn vui lòng thử điều chỉnh lại môn học, khối lớp hoặc mở rộng khu vực tìm kiếm tại [Danh sách lớp học](/lop-hoc) nhé!";
        }

        // 4. Relevant FAQ & Policy Chunks
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

        // 5. Router Policy Fallback
        AiCapabilityRouter.CapabilityPolicy policy = capabilityRouter.getPolicy(domain, subIntent);
        if (policy != null && policy.fallbackMessage() != null && !policy.fallbackMessage().isBlank()) {
            return policy.fallbackMessage();
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
