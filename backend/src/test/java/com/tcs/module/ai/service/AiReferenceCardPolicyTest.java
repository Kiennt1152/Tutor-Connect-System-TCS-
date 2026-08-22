package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiReferenceCardPolicyTest {

    private AiCapabilityRouter router;

    @BeforeEach
    void setUp() {
        router = new AiCapabilityRouter();
    }

    @Test
    @DisplayName("FIND_TUTOR card policy must strictly be TUTOR_CARDS")
    void testTutorCardPolicy() {
        var policy = router.getPolicy(AiDomain.MARKETPLACE, AiSubIntent.FIND_TUTOR);
        assertThat(policy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.TUTOR_CARDS);
    }

    @Test
    @DisplayName("FIND_CLASS card policy must strictly be CLASS_CARDS")
    void testClassCardPolicy() {
        var policy = router.getPolicy(AiDomain.MARKETPLACE, AiSubIntent.FIND_CLASS);
        assertThat(policy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.CLASS_CARDS);
    }

    @Test
    @DisplayName("PLATFORM_STATS and CONVERSATION_SAFETY must have CardPolicy NONE")
    void testStatsAndSafetyCardPolicy() {
        var statsPolicy = router.getPolicy(AiDomain.PLATFORM_ADMIN, AiSubIntent.PLATFORM_STATS);
        assertThat(statsPolicy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.NONE);

        var safetyPolicy = router.getPolicy(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GREETING);
        assertThat(safetyPolicy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.NONE);
    }

    @Test
    @DisplayName("Admin dashboard policy must be ADMIN_LINK_ONLY")
    void testAdminCardPolicy() {
        var dashPolicy = router.getPolicy(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_DASHBOARD);
        assertThat(dashPolicy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.ADMIN_LINK_ONLY);

        var revPolicy = router.getPolicy(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_REVENUE_REPORT);
        assertThat(revPolicy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.ADMIN_LINK_ONLY);
    }

    @Test
    @DisplayName("Tickets and Dispute reports must be TICKET_LINK_ONLY")
    void testTicketCardPolicy() {
        var ticketPolicy = router.getPolicy(AiDomain.MESSAGING_TICKET, AiSubIntent.SUPPORT_TICKET_CREATE);
        assertThat(ticketPolicy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.TICKET_LINK_ONLY);

        var circumventionPolicy = router.getPolicy(AiDomain.TRUST_SAFETY, AiSubIntent.REPORT_CIRCUMVENTION);
        assertThat(circumventionPolicy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.TICKET_LINK_ONLY);
    }

    @Test
    @DisplayName("Catalog, Auth, and Profile FAQs must be FAQ_CARDS")
    void testFaqCardPolicy() {
        var faqPolicy = router.getPolicy(AiDomain.CATALOG_FAQ, AiSubIntent.FAQ_SEARCH);
        assertThat(faqPolicy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.FAQ_CARDS);

        var authPolicy = router.getPolicy(AiDomain.IDENTITY_AUTH, AiSubIntent.LOGIN_HELP);
        assertThat(authPolicy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.FAQ_CARDS);
    }
}
