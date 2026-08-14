package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OpenDomainProductionEnhancementsTest {

    private WeatherService weatherService;
    private ContentSafetyFilter safetyFilter;
    private OpenDomainRateLimiter rateLimiter;
    private ConversationContextService contextService;
    private OpenDomainAnalytics analytics;
    private OpenDomainHandler openHandler;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService();
        safetyFilter = new ContentSafetyFilter();
        rateLimiter = new OpenDomainRateLimiter(5); // 5 max requests for testing limit
        contextService = new ConversationContextService();
        analytics = new OpenDomainAnalytics();
        openHandler = new OpenDomainHandler(weatherService, safetyFilter);
    }

    @Test
    @DisplayName("1. WeatherService provides valid weather and caches responses")
    void testWeatherService() {
        var wOpt = weatherService.getWeather("Hà Nội");
        assertTrue(wOpt.isPresent());
        var w = wOpt.get();
        assertEquals("Hà Nội", w.location());
        assertTrue(w.tempC() > 0);
        assertNotNull(w.condition());

        // Fast retrieve from cache
        var cachedOpt = weatherService.getWeather("Hà Nội");
        assertTrue(cachedOpt.isPresent());
        assertEquals(w.tempC(), cachedOpt.get().tempC());
    }

    @Test
    @DisplayName("2. ContentSafetyFilter blocks dangerous content (weapons, hacking)")
    void testSafetyFilterBlocked() {
        var r1 = safetyFilter.checkQuery("Hướng dẫn chế tạo bom tự chế");
        assertFalse(r1.isSafe());
        assertEquals("BLOCKED_CONTENT", r1.reason());
        assertThat(r1.suggestedResponse()).contains("vi phạm Chính sách An toàn");

        var r2 = safetyFilter.checkQuery("Cách hack facebook người khác");
        assertFalse(r2.isSafe());
        assertEquals("BLOCKED_CONTENT", r2.reason());
    }

    @Test
    @DisplayName("3. ContentSafetyFilter returns compassionate crisis hotlines for mental health distress")
    void testSafetyFilterCrisis() {
        var r = safetyFilter.checkQuery("Tôi cảm thấy quá áp lực và muốn tự tử");
        assertFalse(r.isSafe());
        assertTrue(r.isCrisis());
        assertEquals("CRISIS_TOPIC", r.reason());
        assertThat(r.suggestedResponse()).contains("1800 599 920");
        assertThat(r.suggestedResponse()).contains("111");
    }

    @Test
    @DisplayName("4. OpenDomainRateLimiter throttles users exceeding query threshold")
    void testRateLimiter() {
        Long userId = 100L;
        Long sessionId = 1L;

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRequest(userId, sessionId, AiSubIntent.MATH_CALCULATION));
        }

        // 6th request within 1 minute should be blocked
        assertFalse(rateLimiter.allowRequest(userId, sessionId, AiSubIntent.MATH_CALCULATION));
    }

    @Test
    @DisplayName("5. ConversationContextService resolves multi-turn follow-up queries for Weather")
    void testMultiTurnWeatherFollowUp() {
        Long sessionId = 55L;
        // User first asks: "Thời tiết Hà Nội hôm nay thế nào?"
        contextService.saveContext(sessionId, AiDomain.OPEN_DOMAIN, AiSubIntent.WEATHER_QUERY, Map.of("location", "Hà Nội"), "Thời tiết Hà Nội");

        // User follows up: "Còn ở Đà Nẵng thì sao?"
        var followUp = contextService.resolveFollowUp(sessionId, "Còn ở Đà Nẵng thì sao?", Map.of());
        assertTrue(followUp.isFollowUp());
        assertEquals(AiDomain.OPEN_DOMAIN, followUp.domain());
        assertEquals(AiSubIntent.WEATHER_QUERY, followUp.subIntent());
        assertEquals("Đà Nẵng", followUp.resolvedEntities().get("location"));
    }

    @Test
    @DisplayName("6. ConversationContextService resolves multi-turn follow-up queries for Marketplace")
    void testMultiTurnMarketplaceFollowUp() {
        Long sessionId = 88L;
        // User first asks: "Tìm gia sư Toán lớp 12"
        contextService.saveContext(sessionId, AiDomain.MARKETPLACE, AiSubIntent.FIND_TUTOR, Map.of("subject", "Toán", "grade", "12"), "Tìm gia sư Toán 12");

        // User follows up: "Còn môn Hóa thì sao?"
        var followUp = contextService.resolveFollowUp(sessionId, "Còn môn Hóa thì sao?", Map.of("subject", "Hóa"));
        assertTrue(followUp.isFollowUp());
        assertEquals(AiDomain.MARKETPLACE, followUp.domain());
        assertEquals(AiSubIntent.FIND_TUTOR, followUp.subIntent());
        assertEquals("Hóa", followUp.resolvedEntities().get("subject"));
        assertEquals("12", followUp.resolvedEntities().get("grade"));
    }

    @Test
    @DisplayName("7. OpenDomainAnalytics tracks queries and conversion metrics")
    void testAnalytics() {
        analytics.track(1L, 10L, AiSubIntent.WEATHER_QUERY, "thời tiết hôm nay", "/tim-gia-su", java.util.List.of("Gia sư Online"));
        analytics.track(2L, 11L, AiSubIntent.MATH_CALCULATION, "1 + 1", "/tim-gia-su?subject=Toán", java.util.List.of("Gia sư Toán"));

        var stats = analytics.getStats();
        assertEquals(2, stats.totalQueries());
        assertEquals(1, stats.subIntentBreakdown().get("WEATHER_QUERY"));
        assertEquals(1, stats.subIntentBreakdown().get("MATH_CALCULATION"));
        assertEquals(100.0, stats.ctaSteeringRate());
    }

    @Test
    @DisplayName("8. OpenDomainHandler formats weather with real temperature and soft steering")
    void testOpenDomainHandlerWeather() {
        var response = openHandler.handle(AiSubIntent.WEATHER_QUERY, "Thời tiết TP.HCM thế nào?", Map.of("location", "TP.HCM"));
        assertThat(response.answer()).contains("TP.HCM");
        assertThat(response.answer()).contains("Nhiệt độ");
        assertThat(response.steeringMessage()).contains("Zoom");
        assertThat(response.suggestedRoute()).isEqualTo("/tim-gia-su?mode=ONLINE");
    }
}
