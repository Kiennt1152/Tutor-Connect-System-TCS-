package com.tcs.module.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.ai.dto.request.ChatRequest;
import com.tcs.module.ai.dto.response.*;
import com.tcs.module.ai.entity.AiChatMessage;
import com.tcs.module.ai.entity.AiChatSession;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.repository.AiChatMessageRepository;
import com.tcs.module.ai.repository.AiChatSessionRepository;
import com.tcs.module.ai.service.*;
import com.tcs.module.ai.service.provider.AiClassSearchContextProvider;
import com.tcs.module.ai.service.provider.AiPublicPlatformStatsContextProvider;
import com.tcs.module.ai.service.provider.AiTutorFinanceContextProvider;
import com.tcs.module.ai.service.provider.AiTutorSearchContextProvider;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final FaqEntryRepository faqEntryRepository;
    private final TutoringClassRepository tutoringClassRepository;

    private final AiConversationContextService contextService;
    private final AiQueryRewriteService rewriteService;
    private final AiIntentService intentService;
    private final AiRetrievalService retrievalService;
    private final AiRerankService rerankService;
    private final AiPromptBuilderService promptBuilderService;
    private final AiAnswerEvaluatorService evaluatorService;
    private final AiCapabilityRouter capabilityRouter;
    private final AiFallbackService fallbackService;
    private final AiHallucinationGuard hallucinationGuard;
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
    private final ObjectMapper objectMapper;

    @Value("${ai.provider.max-output-tokens:700}")
    private int maxOutputTokens = 700;

    @Override
    @Transactional
    public AiMessageResponse chat(ChatRequest request, Long userId) {
        String userRole = "GUEST";
        if (userId != null) {
            if (platformAdminRepository.findByUser_UserId(userId).isPresent()) {
                userRole = "PLATFORM_ADMIN";
            } else if (tutorRepository.findByUser_UserId(userId).isPresent()) {
                userRole = "TUTOR";
            } else if (tutorCenterRepository.findByUser_UserId(userId).isPresent()) {
                userRole = "TUTOR_CENTER";
            } else if (clientRepository.findByUser_UserId(userId).isPresent()) {
                userRole = "CLIENT";
            } else {
                userRole = "USER";
            }
        }

        AiChatSession session = getOrCreateSession(request.getSessionId(), userId, request.getMessage());

        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setSession(session);
        userMsg.setRole("user");
        userMsg.setContent(request.getMessage());
        messageRepository.save(userMsg);

        // 0. Content Safety & Crisis Fast-Path Filter
        ContentSafetyFilter.SafetyCheckResult safetyCheck = contentSafetyFilter.checkQuery(request.getMessage());
        if (!safetyCheck.isSafe()) {
            AiChatMessage aiMsg = new AiChatMessage();
            aiMsg.setSession(session);
            aiMsg.setRole("assistant");
            aiMsg.setContent(safetyCheck.suggestedResponse());
            messageRepository.save(aiMsg);
            sessionRepository.save(session);

            List<String> options;
            String suggestedRoute = "/help";
            if (safetyCheck.isCrisis()) {
                options = List.of("Tổng đài Trẻ em (111)", "Đường dây nóng Sức khỏe Tâm thần (1800 599 920)", "Trung tâm trợ giúp (/help)");
            } else if ("PRIVACY_AND_ACCESS_RESTRICTED".equals(safetyCheck.reason())) {
                options = List.of("Quản lý người dùng (/platform/users)", "Bảng điều khiển Quản trị (/platform/analytics)", "Chính sách bảo mật (/help)");
                suggestedRoute = "/platform/users";
            } else {
                options = List.of("Tìm gia sư uy tín (/tim-gia-su)", "Quy tắc cộng đồng (/help)");
            }

            return AiMessageResponse.builder()
                    .messageId(aiMsg.getMessageId())
                    .sessionId(session.getSessionId())
                    .role("assistant")
                    .content(safetyCheck.suggestedResponse())
                    .createdAt(aiMsg.getCreatedAt())
                    .intent(AiIntent.OUT_OF_SCOPE.name())
                    .domain(AiDomain.CONVERSATION_SAFETY.name())
                    .subIntent(AiSubIntent.OUT_OF_SCOPE.name())
                    .suggestedRoute(suggestedRoute)
                    .clarificationOptions(options)
                    .answerMode("SAFETY_FILTER")
                    .confidenceScore(1.0)
                    .confidenceLevel("HIGH")
                    .sourceCount(0)
                    .groundingStatus("SAFETY_FILTERED")
                    .sources(List.of())
                    .referencedTutors(List.of())
                    .referencedClasses(List.of())
                    .referencedFaqs(List.of())
                    .build();
        }

        List<AiChatMessage> history = contextService.getHistory(session.getSessionId());

        // 0.5. Query Expansion with TCS Synonyms
        String expandedQuery = synonymService.expandQuery(request.getMessage());
        String normalizedQuery = synonymService.normalizeQuery(request.getMessage());
        log.debug("Query expansion: original='{}', expanded='{}', normalized='{}'", 
                  request.getMessage(), expandedQuery, normalizedQuery);

        // 0.6. Semantic Cache Check (before expensive LLM calls)
        Optional<AiSemanticCacheService.CachedResponse> cachedResponse = 
            semanticCacheService.get(request.getMessage(), userRole, null);
        
        if (cachedResponse.isPresent()) {
            AiSemanticCacheService.CachedResponse cache = cachedResponse.get();
            
            AiChatMessage aiMsg = new AiChatMessage();
            aiMsg.setSession(session);
            aiMsg.setRole("assistant");
            aiMsg.setContent(cache.content());
            aiMsg.setReferencedTutorIds(cache.referencedTutorIds());
            aiMsg.setReferencedClassIds(cache.referencedClassIds());
            aiMsg.setReferencedFaqIds(cache.referencedFaqIds());
            messageRepository.save(aiMsg);
            sessionRepository.save(session);
            
            log.info("Returned cached response for query: '{}'", request.getMessage());
            
            return AiMessageResponse.builder()
                    .messageId(aiMsg.getMessageId())
                    .sessionId(session.getSessionId())
                    .role("assistant")
                    .content(cache.content())
                    .createdAt(aiMsg.getCreatedAt())
                    .intent(cache.intent())
                    .domain(cache.domain())
                    .subIntent(cache.subIntent())
                    .answerMode("CACHED")
                    .confidenceScore(cache.confidenceScore() != null ? cache.confidenceScore() : 1.0)
                    .confidenceLevel("HIGH")
                    .sourceCount(cache.sourceCount() != null ? cache.sourceCount() : 0)
                    .groundingStatus("CACHED")
                    .sources(List.of())
                    .referencedTutors(List.of())
                    .referencedClasses(List.of())
                    .referencedFaqs(List.of())
                    .build();
        }

        // 1. 3-Tier Classification (Domain -> SubIntent -> Entities)
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
            AiChatMessage aiMsg = new AiChatMessage();
            aiMsg.setSession(session);
            aiMsg.setRole("assistant");
            aiMsg.setContent(safetyResult.message());
            messageRepository.save(aiMsg);
            sessionRepository.save(session);

            return AiMessageResponse.builder()
                    .messageId(aiMsg.getMessageId())
                    .sessionId(session.getSessionId())
                    .role("assistant")
                    .content(safetyResult.message())
                    .createdAt(aiMsg.getCreatedAt())
                    .intent(legacyIntent.name())
                    .domain(domain.name())
                    .subIntent(subIntent.name())
                    .suggestedRoute(safetyResult.suggestedRoute())
                    .clarificationOptions(safetyResult.clarificationOptions())
                    .answerMode("DIRECT")
                    .confidenceScore(1.0)
                    .confidenceLevel("HIGH")
                    .sourceCount(0)
                    .groundingStatus("GROUNDED")
                    .sources(List.of())
                    .referencedTutors(List.of())
                    .referencedClasses(List.of())
                    .referencedFaqs(List.of())
                    .build();
        }

        // 3. Query Rewriting for follow-up conversational context
        AiQueryRewriteService.RewriteResult rewritten = rewriteService.rewriteQuery(history, expandedQuery, legacyIntent);
        log.info("AI role={}, domain={}, subIntent={}, entities={}, rewrittenQuery={}",
                userRole, domain, subIntent, entities, rewritten.rewrittenQuery());

        // 4. Capability Policy & Role Verification
        AiCapabilityRouter.CapabilityPolicy policy = capabilityRouter.getPolicy(domain, subIntent);
        if (policy.requireAuth() && !policy.allowedRoles().isEmpty() && !policy.allowedRoles().contains(userRole)) {
            String requiredRoleDesc = policy.allowedRoles().contains("PLATFORM_ADMIN") ? "Quản trị viên (PLATFORM_ADMIN)" :
                                     policy.allowedRoles().contains("TUTOR") ? "Gia sư" : "Người dùng hợp lệ";
            AiFallbackService.FallbackResult authFallback = fallbackService.getLevel4AuthRoleRequired(requiredRoleDesc, policy.deepLinkRoute());

            AiChatMessage aiMsg = new AiChatMessage();
            aiMsg.setSession(session);
            aiMsg.setRole("assistant");
            aiMsg.setContent(authFallback.message());
            messageRepository.save(aiMsg);
            sessionRepository.save(session);

            return AiMessageResponse.builder()
                    .messageId(aiMsg.getMessageId())
                    .sessionId(session.getSessionId())
                    .role("assistant")
                    .content(authFallback.message())
                    .createdAt(aiMsg.getCreatedAt())
                    .intent(legacyIntent.name())
                    .domain(domain.name())
                    .subIntent(subIntent.name())
                    .suggestedRoute(authFallback.suggestedRoute())
                    .clarificationOptions(authFallback.clarificationOptions())
                    .answerMode("FALLBACK")
                    .confidenceScore(0.9)
                    .confidenceLevel("HIGH")
                    .sourceCount(0)
                    .groundingStatus("PERMISSION_RESTRICTED")
                    .sources(List.of())
                    .referencedTutors(List.of())
                    .referencedClasses(List.of())
                    .referencedFaqs(List.of())
                    .build();
        }


        // 4.5. Out-of-Scope Gating (includes former Open Domain queries)
        if (domain == AiDomain.OUT_OF_SCOPE) {
            String outOfScopeMsg = "Xin lỗi, tôi chỉ hỗ trợ các câu hỏi liên quan đến hệ thống Tutor Connect System (TCS) như: tìm gia sư, tìm lớp học, quy trình thanh toán, hợp đồng, và các chính sách nền tảng. Câu hỏi này nằm ngoài phạm vi hỗ trợ của tôi.";
            AiChatMessage aiMsg = new AiChatMessage();
            aiMsg.setSession(session);
            aiMsg.setRole("assistant");
            aiMsg.setContent(outOfScopeMsg);
            messageRepository.save(aiMsg);
            sessionRepository.save(session);
            conversationContextService.saveContext(session.getSessionId(), domain, subIntent, entities, request.getMessage());

            return AiMessageResponse.builder()
                    .messageId(aiMsg.getMessageId())
                    .sessionId(session.getSessionId())
                    .role("assistant")
                    .content(outOfScopeMsg)
                    .createdAt(aiMsg.getCreatedAt())
                    .intent(legacyIntent.name())
                    .domain(domain.name())
                    .subIntent(subIntent.name())
                    .suggestedRoute("/help")
                    .clarificationOptions(List.of("Tìm gia sư (/tim-gia-su)", "Xem lớp học (/lop-hoc)", "Câu hỏi thường gặp (/help)"))
                    .answerMode("FALLBACK_OUT_OF_SCOPE")
                    .confidenceScore(0.0)
                    .confidenceLevel("NONE")
                    .sourceCount(0)
                    .groundingStatus("OUT_OF_SCOPE")
                    .sources(List.of())
                    .referencedTutors(List.of())
                    .referencedClasses(List.of())
                    .referencedFaqs(List.of())
                    .build();
        }

        // 5. Context Retrieval (Unified Vector RAG + Database Providers)
        List<AiSourceResponse> allSources = new ArrayList<>();
        String lowerQuery = request.getMessage().toLowerCase();
        boolean retrievalUnavailable = false;

        if (domain != AiDomain.CONVERSATION_SAFETY && domain != AiDomain.OUT_OF_SCOPE) {
            try {
                // Unified Vector RAG retrieval: Always retrieve top semantic chunks across all knowledge
                List<AiRetrievalService.RetrievalResult> vectorResults = retrievalService.retrieve(rewritten.rewrittenQuery(), userRole, userId);
                List<AiSourceResponse> rerankedVectorResults = rerankService.rerank(vectorResults, new AiIntentService.IntentResultWithEntities(legacyIntent, classification.confidence(), entities), rewritten.rewrittenQuery());
                allSources.addAll(rerankedVectorResults);

                if ((subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) && allSources.stream().noneMatch(s -> "TUTOR".equals(s.getSourceType()))) {
                    allSources.addAll(tutorSearchContextProvider.searchTutors(entities));
                } else if ((subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) && allSources.stream().noneMatch(s -> "CLASS".equals(s.getSourceType()))) {
                    allSources.addAll(classSearchContextProvider.searchClasses(entities));
                } else if (subIntent == AiSubIntent.PLATFORM_STATS) {
                    allSources.addAll(platformStatsContextProvider.getPlatformStats());
                } else if (domain == AiDomain.PLATFORM_ADMIN) {
                    allSources.addAll(dashboardContextProvider.getDashboardContext(userRole));
                } else if (domain == AiDomain.FINANCE_WALLET) {
                    if (lowerQuery.contains("lương của tôi") || lowerQuery.contains("thu nhập của tôi") || lowerQuery.contains("tiền kiếm được của tôi") || lowerQuery.contains("ví của tôi") || lowerQuery.contains("số dư của tôi")) {
                        allSources.addAll(tutorFinanceContextProvider.getTutorFinanceContext(userRole, userId));
                    }
                } else if (domain == AiDomain.MESSAGING_TICKET || domain == AiDomain.TRUST_SAFETY) {
                    allSources.addAll(ticketContextProvider.getTicketContext(userRole, userId));
                }
            } catch (Exception e) {
                log.warn("AI Context Retrieval failed: {}", e.getMessage());
                retrievalUnavailable = true;
            }
        }

        // Deduplicate and keep top 4 sources
        allSources = deduplicateAndLimitSources(allSources, 4);

        // Score Guard: Drop sources with finalScore below minimum relevance threshold.
        allSources.removeIf(s -> s.getFinalScore() < 0.60);

        // 6. Grounding Evaluation
        AiAnswerEvaluatorService.EvaluatedAnswer evaluation = evaluatorService.evaluate(legacyIntent, allSources);

        // 7. Answer Generation (Natural LLM Generation with Grounding)
        String aiResponseText = null;
        List<TutorReferenceDto> tutors = new ArrayList<>();
        List<ClassReferenceDto> classes = new ArrayList<>();
        List<FaqReferenceDto> faqs = new ArrayList<>();

        // Populate UI cards dynamically from high-confidence vector sources (>= 0.65)
        if (domain != AiDomain.CONVERSATION_SAFETY) {
            Set<Long> addedTutorIds = new HashSet<>();
            Set<Long> addedClassIds = new HashSet<>();
            Set<Long> addedFaqIds = new HashSet<>();

            for (AiSourceResponse s : allSources) {
                if (s.getFinalScore() < 0.65) continue;

                if ("TUTOR".equals(s.getSourceType()) && (subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) && tutors.size() < 3) {
                    try {
                        Long tId = Long.parseLong(s.getSourceId());
                        if (addedTutorIds.add(tId)) {
                            tutorRepository.findById(tId).ifPresent(t -> tutors.add(
                                TutorReferenceDto.builder()
                                    .tutorId(t.getTutorId())
                                    .fullName(t.getFullName())
                                    .avatarUrl(t.getAvatar())
                                    .title(t.getBio() != null && t.getBio().length() > 60 ? t.getBio().substring(0, 60) + "..." : t.getBio())
                                    .hourlyRate(t.getHourlyRate())
                                    .averageRating(t.getRatingAvg() != null ? t.getRatingAvg().doubleValue() : 5.0)
                                    .teachingAreas(t.getAddress())
                                    .build()
                            ));
                        }
                    } catch (NumberFormatException ignored) {}
                } else if ("CLASS".equals(s.getSourceType()) && (subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) && classes.size() < 3) {
                    try {
                        Long cId = Long.parseLong(s.getSourceId());
                        if (addedClassIds.add(cId)) {
                            tutoringClassRepository.findById(cId).ifPresent(c -> classes.add(
                                ClassReferenceDto.builder()
                                    .classId(c.getClassId())
                                    .title(c.getTitle())
                                    .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                                    .gradeLevelName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                                    .tuitionFee(c.getTuitionFee())
                                    .location(c.getAddress())
                                    .status(c.getStatus() != null ? c.getStatus().name() : "OPEN")
                                    .build()
                            ));
                        }
                    } catch (NumberFormatException ignored) {}
                } else if ("FAQ".equals(s.getSourceType()) && (domain == AiDomain.CATALOG_FAQ || subIntent == AiSubIntent.FAQ_SEARCH) && faqs.size() < 3) {
                    try {
                        Long fId = Long.parseLong(s.getSourceId());
                        if (addedFaqIds.add(fId)) {
                            faqEntryRepository.findById(fId).ifPresent(f -> faqs.add(
                                    FaqReferenceDto.builder()
                                            .faqId(f.getFaqId())
                                            .question(f.getQuestion())
                                            .build()));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (domain == AiDomain.FINANCE_WALLET) {
            boolean isPersonal = lowerQuery.contains("của tôi") || lowerQuery.contains("lương của tôi") || lowerQuery.contains("thu nhập của tôi") || lowerQuery.contains("ví của tôi") || lowerQuery.contains("tiền của tôi");
            if (isPersonal && (userId == null || (!"TUTOR".equals(userRole) && !"TUTOR_CENTER".equals(userRole)))) {
                aiResponseText = fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance").message();
            }
        }

        // Call LLM for natural, intelligent RAG response synthesis
        if (aiResponseText == null) {
            String finalPrompt = promptBuilderService.buildPrompt(request.getMessage(), rewritten.rewrittenQuery(), legacyIntent, userRole, allSources, retrievalUnavailable);
            aiResponseText = callLlm(finalPrompt, history, evaluation.answerMode(), legacyIntent, allSources);
        }

        // Apply Hallucination Guard to prevent fake names, classes, or false stats
        if (subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) {
            aiResponseText = hallucinationGuard.guardTutorResponse(aiResponseText, tutors, fallbackService.getLevel3NoData(subIntent, entities).message());
        } else if (subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) {
            aiResponseText = hallucinationGuard.guardClassResponse(aiResponseText, classes, fallbackService.getLevel3NoData(subIntent, entities).message());
        } else if (subIntent == AiSubIntent.PLATFORM_STATS) {
            aiResponseText = hallucinationGuard.guardStatsResponse(aiResponseText, allSources, fallbackService.getLevel3NoData(subIntent, entities).message());
        } else if (domain == AiDomain.FINANCE_WALLET) {
            String financeGuardResult = hallucinationGuard.guardFinanceResponse(request.getMessage(), userRole, userId, fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance").message());
            if (financeGuardResult != null) {
                aiResponseText = financeGuardResult;
            }
        }

        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResponseText);

        if (!tutors.isEmpty()) aiMsg.setReferencedTutorIds(tutors.stream().map(t -> String.valueOf(t.getTutorId())).collect(Collectors.joining(",")));
        if (!classes.isEmpty()) aiMsg.setReferencedClassIds(classes.stream().map(c -> String.valueOf(c.getClassId())).collect(Collectors.joining(",")));
        if (!faqs.isEmpty()) aiMsg.setReferencedFaqIds(faqs.stream().map(f -> String.valueOf(f.getFaqId())).collect(Collectors.joining(",")));
        messageRepository.save(aiMsg);
        sessionRepository.save(session);

        List<Long> tutorIdList = tutors.stream().map(TutorReferenceDto::getTutorId).filter(Objects::nonNull).toList();
        List<Long> classIdList = classes.stream().map(ClassReferenceDto::getClassId).filter(Objects::nonNull).toList();
        List<Long> faqIdList = faqs.stream().map(FaqReferenceDto::getFaqId).filter(Objects::nonNull).toList();
        conversationContextService.saveContext(
            session.getSessionId(),
            domain,
            subIntent,
            entities,
            request.getMessage(),
            tutorIdList,
            classIdList,
            faqIdList,
            entities.getOrDefault("subject", null)
        );

        // Store in cache for future similar queries (only for high-confidence, grounded responses)
        if (evaluation.confidenceScore() >= 0.70 && 
            !"FALLBACK".equals(evaluation.answerMode()) && 
            !"SAFETY_FILTER".equals(evaluation.answerMode()) &&
            domain != AiDomain.OUT_OF_SCOPE) {
            
            semanticCacheService.put(
                request.getMessage(),
                normalizedQuery,
                aiResponseText,
                legacyIntent.name(),
                domain.name(),
                subIntent.name(),
                evaluation.confidenceScore(),
                evaluation.sourceCount(),
                !tutors.isEmpty() ? tutors.stream().map(t -> String.valueOf(t.getTutorId())).collect(Collectors.joining(",")) : null,
                !classes.isEmpty() ? classes.stream().map(c -> String.valueOf(c.getClassId())).collect(Collectors.joining(",")) : null,
                !faqs.isEmpty() ? faqs.stream().map(f -> String.valueOf(f.getFaqId())).collect(Collectors.joining(",")) : null,
                userRole,
                null
            );
        }

        return AiMessageResponse.builder()
                .messageId(aiMsg.getMessageId())
                .sessionId(session.getSessionId())
                .role("assistant")
                .content(aiResponseText)
                .createdAt(aiMsg.getCreatedAt())
                .referencedTutors(tutors)
                .referencedClasses(classes)
                .referencedFaqs(faqs)
                .sources(allSources)
                .intent(legacyIntent.name())
                .domain(domain.name())
                .subIntent(subIntent.name())
                .suggestedRoute(suggestedRoute != null ? suggestedRoute : policy.deepLinkRoute())
                .answerMode(evaluation.answerMode())
                .confidenceScore(evaluation.confidenceScore())
                .confidenceLevel(evaluation.confidenceLevel())
                .sourceCount(evaluation.sourceCount())
                .groundingStatus(evaluation.groundingStatus())
                .warningCode(evaluation.warningCode())
                .rewrittenQuery(rewritten.isFollowUp() ? rewritten.rewrittenQuery() : null)
                .followUp(rewritten.isFollowUp())
                .evaluationNotes(evaluation.evaluationNotes())
                .build();
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
                
                VÍ DỤ TỐT:
                "Phí nền tảng TCS là 10% học phí, được khấu trừ tự động từ mỗi thanh toán."
                
                VÍ DỤ XẤU (CẤM):
                "Tôi nghĩ phí khoảng 8-12%..." ❌
                "Gia sư Nguyễn Văn A rất giỏi..." ❌ (nếu không có trong Context)
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

        // Fallback directly to top FAQ / Knowledge source snippet ONLY if high confidence (>= 0.65)
        if (sources != null && !sources.isEmpty()) {
            for (AiSourceResponse s : sources) {
                if (s.getFinalScore() >= 0.65 && ("FAQ".equals(s.getSourceType()) || "POLICY".equals(s.getSourceType()) || "SYSTEM_DOC".equals(s.getSourceType()) || "TUTOR".equals(s.getSourceType()) || "CLASS".equals(s.getSourceType()))) {
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
        if (userId == null) {
            return List.of();
        }
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
        if (sessionId == null) {
            return List.of();
        }
        AiChatSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên hội thoại"));

        if (session.getUserId() != null) {
            if (userId == null || !session.getUserId().equals(userId)) {
                throw new ForbiddenException("Bạn không có quyền truy cập phiên hội thoại này");
            }
        }

        List<AiChatMessage> msgs = messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);
        if (msgs.isEmpty()) {
            return List.of();
        }

        // Collect all IDs in batch to eliminate N+1 queries
        Set<Long> tutorIds = new HashSet<>();
        Set<Long> classIds = new HashSet<>();
        Set<Long> faqIds = new HashSet<>();

        for (AiChatMessage m : msgs) {
            if (m.getReferencedTutorIds() != null && !m.getReferencedTutorIds().isBlank()) {
                for (String idStr : m.getReferencedTutorIds().split(",")) {
                    try {
                        tutorIds.add(Long.parseLong(idStr.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (m.getReferencedClassIds() != null && !m.getReferencedClassIds().isBlank()) {
                for (String idStr : m.getReferencedClassIds().split(",")) {
                    try {
                        classIds.add(Long.parseLong(idStr.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (m.getReferencedFaqIds() != null && !m.getReferencedFaqIds().isBlank()) {
                for (String idStr : m.getReferencedFaqIds().split(",")) {
                    try {
                        faqIds.add(Long.parseLong(idStr.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        Map<Long, Tutor> tutorMap = tutorIds.isEmpty()
            ? Map.of()
            : tutorRepository.findAllById(tutorIds).stream()
                .collect(Collectors.toMap(Tutor::getTutorId, t -> t, (a, b) -> a));

        Map<Long, TutoringClass> classMap = classIds.isEmpty()
            ? Map.of()
            : tutoringClassRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(TutoringClass::getClassId, c -> c, (a, b) -> a));

        Map<Long, FaqEntry> faqMap = faqIds.isEmpty()
            ? Map.of()
            : faqEntryRepository.findAllById(faqIds).stream()
                .collect(Collectors.toMap(FaqEntry::getFaqId, f -> f, (a, b) -> a));

        return msgs.stream().map(m -> {
            AiMessageResponse.AiMessageResponseBuilder builder = AiMessageResponse.builder()
                .messageId(m.getMessageId())
                .sessionId(sessionId)
                .role(m.getRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt());

            // Hydrate tutor references preserving order
            if (m.getReferencedTutorIds() != null && !m.getReferencedTutorIds().isBlank()) {
                List<TutorReferenceDto> tutors = new ArrayList<>();
                for (String idStr : m.getReferencedTutorIds().split(",")) {
                    try {
                        Long tutorId = Long.parseLong(idStr.trim());
                        Tutor t = tutorMap.get(tutorId);
                        if (t != null) {
                            tutors.add(TutorReferenceDto.builder()
                                .tutorId(t.getTutorId())
                                .fullName(t.getFullName())
                                .avatarUrl(t.getAvatar())
                                .title(t.getBio() != null && t.getBio().length() > 60 ? t.getBio().substring(0, 60) + "..." : t.getBio())
                                .hourlyRate(t.getHourlyRate())
                                .averageRating(t.getRatingAvg() != null ? t.getRatingAvg().doubleValue() : 5.0)
                                .teachingAreas(t.getAddress())
                                .build());
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (!tutors.isEmpty()) {
                    builder.referencedTutors(tutors);
                    builder.intent("FIND_TUTOR");
                }
            }

            // Hydrate class references preserving order
            if (m.getReferencedClassIds() != null && !m.getReferencedClassIds().isBlank()) {
                List<ClassReferenceDto> classes = new ArrayList<>();
                for (String idStr : m.getReferencedClassIds().split(",")) {
                    try {
                        Long classId = Long.parseLong(idStr.trim());
                        TutoringClass c = classMap.get(classId);
                        if (c != null) {
                            classes.add(ClassReferenceDto.builder()
                                .classId(c.getClassId())
                                .title(c.getTitle())
                                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                                .gradeLevelName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                                .tuitionFee(c.getTuitionFee())
                                .location(c.getAddress())
                                .status(c.getStatus() != null ? c.getStatus().name() : "OPEN")
                                .build());
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (!classes.isEmpty()) {
                    builder.referencedClasses(classes);
                    builder.intent("FIND_CLASS");
                }
            }

            // Hydrate FAQ references preserving order
            if (m.getReferencedFaqIds() != null && !m.getReferencedFaqIds().isBlank()) {
                List<FaqReferenceDto> faqs = new ArrayList<>();
                for (String idStr : m.getReferencedFaqIds().split(",")) {
                    try {
                        Long faqId = Long.parseLong(idStr.trim());
                        FaqEntry f = faqMap.get(faqId);
                        if (f != null) {
                            faqs.add(FaqReferenceDto.builder()
                                .faqId(f.getFaqId())
                                .question(f.getQuestion())
                                .answer(f.getAnswer())
                                .category(f.getCategory())
                                .build());
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (!faqs.isEmpty()) builder.referencedFaqs(faqs);
            }

            return builder.build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        if (sessionId == null) return;
        AiChatSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên hội thoại"));

        if (session.getUserId() != null) {
            if (userId == null || !session.getUserId().equals(userId)) {
                throw new ForbiddenException("Bạn không có quyền xóa phiên hội thoại này");
            }
        }

        messageRepository.deleteBySession_SessionId(sessionId);
        sessionRepository.delete(session);
    }

    private AiChatSession getOrCreateSession(Long sessionId, Long userId, String initialMsg) {
        if (sessionId != null) {
            Optional<AiChatSession> opt = sessionRepository.findById(sessionId);
            if (opt.isPresent()) {
                AiChatSession session = opt.get();
                if (session.getUserId() == null) {
                    if (userId != null) {
                        session.setUserId(userId);
                        return sessionRepository.save(session);
                    }
                    return session;
                } else if (session.getUserId().equals(userId)) {
                    return session;
                }
                // Session belongs to another user: isolate and spawn a new session for current user
                return createSession(userId, initialMsg);
            }
        }
        return createSession(userId, initialMsg);
    }

    private AiChatSession createSession(Long userId, String initialMsg) {
        AiChatSession s = new AiChatSession();
        s.setUserId(userId);
        s.setTitle(initialMsg.length() > 35 ? initialMsg.substring(0, 35) + "..." : initialMsg);
        return sessionRepository.save(s);
    }
}
