package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ngoài phạm vi Report 5.1: MethodList không liệt kê lớp này.
 * Đây là bộ test tích hợp / đánh giá chất lượng trợ lý AI của nhóm.
 */
class AiRolePolicyTest {

    private AiCapabilityRouter router;
    private AiFallbackService fallbackService;

    @BeforeEach
    void setUp() {
        router = new AiCapabilityRouter();
        fallbackService = new AiFallbackService();
    }

    @Test
    @DisplayName("Guest asking about personal wallet/salary gets L4 Role Guidance")
    void shouldRequireTutorRoleForPersonalWallet() {
        var policy = router.getPolicy(AiDomain.FINANCE_WALLET, AiSubIntent.WALLET_VIEW);
        assertThat(policy.requireAuth()).isTrue();
        assertThat(policy.guardType()).isEqualTo(AiCapabilityRouter.GuardType.FINANCE_LOGIN_GUARD);

        var fallback = fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance");
        assertThat(fallback.message()).contains("cần đăng nhập với vai trò");
        assertThat(fallback.message()).contains("Gia sư hoặc Trung tâm gia sư");
        assertThat(fallback.suggestedRoute()).isEqualTo("/finance");
    }

    @Test
    @DisplayName("Admin dashboard policy strictly requires PLATFORM_ADMIN role")
    void shouldRequirePlatformAdminForAdminDashboard() {
        var policy = router.getPolicy(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_DASHBOARD);
        assertThat(policy.requireAuth()).isTrue();
        assertThat(policy.allowedRoles()).contains("PLATFORM_ADMIN");
        assertThat(policy.deepLinkRoute()).isEqualTo("/platform");
        assertThat(policy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.ADMIN_LINK_ONLY);
    }

    @Test
    @DisplayName("Admin revenue report policy strictly requires PLATFORM_ADMIN role")
    void shouldRequirePlatformAdminForRevenueReport() {
        var policy = router.getPolicy(AiDomain.PLATFORM_ADMIN, AiSubIntent.ADMIN_REVENUE_REPORT);
        assertThat(policy.requireAuth()).isTrue();
        assertThat(policy.allowedRoles()).contains("PLATFORM_ADMIN");
        assertThat(policy.deepLinkRoute()).isEqualTo("/platform/analytics");
        assertThat(policy.cardPolicy()).isEqualTo(AiCapabilityRouter.CardPolicy.ADMIN_LINK_ONLY);
    }

    @Test
    @DisplayName("Center operations policy requires TUTOR_CENTER or PLATFORM_ADMIN role")
    void shouldRequireCenterRoleForCenterOps() {
        var policy = router.getPolicy(AiDomain.CENTER_OPS, AiSubIntent.CENTER_TUTOR_MANAGEMENT);
        assertThat(policy.requireAuth()).isTrue();
        assertThat(policy.allowedRoles()).contains("TUTOR_CENTER");
        assertThat(policy.deepLinkRoute()).isEqualTo("/center");
    }

    @Test
    @DisplayName("Tutor operations policy requires TUTOR role")
    void shouldRequireTutorRoleForTutorOps() {
        var policy = router.getPolicy(AiDomain.TUTOR_OPS, AiSubIntent.TUTOR_SCHEDULE_VIEW);
        assertThat(policy.requireAuth()).isTrue();
        assertThat(policy.allowedRoles()).contains("TUTOR");
        assertThat(policy.deepLinkRoute()).isEqualTo("/tutor/schedule");
    }
}
