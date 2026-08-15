package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenDomainClassifierTest {

    private OpenDomainClassifier openClassifier;
    private OpenDomainHandler openHandler;
    private IntentClassifier intentClassifier;
    private AiIntentService intentService;

    @BeforeEach
    void setUp() {
        openClassifier = new OpenDomainClassifier();
        openHandler = new OpenDomainHandler();
        intentClassifier = new IntentClassifier(openClassifier);
        intentService = new AiIntentService(intentClassifier);
    }

    @Test
    @DisplayName("Classify math calculation queries")
    void testMathCalculation() {
        var result = openClassifier.classifyOpen("1 + 1 bằng mấy?");
        assertEquals(AiSubIntent.MATH_CALCULATION, result.subIntent());
        assertTrue(result.confidence() >= 0.9);
    }

    @Test
    @DisplayName("Classify time and date queries")
    void testTimeDateQuery() {
        var result = openClassifier.classifyOpen("Hôm nay là ngày mấy?");
        assertEquals(AiSubIntent.TIME_DATE_QUERY, result.subIntent());
        assertTrue(result.confidence() >= 0.9);
    }

    @Test
    @DisplayName("Classify weather queries")
    void testWeatherQuery() {
        var result = openClassifier.classifyOpen("Thời tiết Hà Nội hôm nay thế nào?");
        assertEquals(AiSubIntent.WEATHER_QUERY, result.subIntent());
        assertEquals("Hà Nội", result.extractedData().get("location"));
    }

    @Test
    @DisplayName("Classify general knowledge queries")
    void testGeneralKnowledge() {
        var result = openClassifier.classifyOpen("Thủ đô của Việt Nam là gì?");
        assertEquals(AiSubIntent.GENERAL_KNOWLEDGE, result.subIntent());
    }

    @Test
    @DisplayName("Classify definition lookup queries")
    void testDefinitionLookup() {
        var result = openClassifier.classifyOpen("Ecosystem nghĩa là gì?");
        assertEquals(AiSubIntent.DEFINITION_LOOKUP, result.subIntent());
    }



    @Test
    @DisplayName("Extract IELTS certification entity")
    void testEntityIELTS() {
        var result = intentService.classifyAndExtractDetailed("Tìm gia sư luyện IELTS band 7.5");
        assertEquals("IELTS 7.5", result.entities().get("certLevel"));
        assertEquals("Anh", result.entities().get("subject"));
    }

    @Test
    @DisplayName("Extract HSK certification entity")
    void testEntityHSK() {
        var result = intentService.classifyAndExtractDetailed("Cần gia sư Tiếng Trung HSK 5");
        assertEquals("Tiếng Trung", result.entities().get("subject"));
        assertEquals("HSK 5", result.entities().get("certLevel"));
    }

    @Test
    @DisplayName("Extract JLPT certification entity")
    void testEntityJLPT() {
        var result = intentService.classifyAndExtractDetailed("Tìm gia sư Tiếng Nhật JLPT N2");
        assertEquals("Tiếng Nhật", result.entities().get("subject"));
        assertEquals("JLPT N2", result.entities().get("certLevel"));
    }

    @Test
    @DisplayName("Extract programming language entities")
    void testEntityProgramming() {
        var r1 = intentService.classifyAndExtractDetailed("Tìm gia sư dạy Python cho trẻ em");
        assertEquals("Tin học", r1.entities().get("subject"));
        assertEquals("Python", r1.entities().get("programmingLang"));

        var r2 = intentService.classifyAndExtractDetailed("Gia sư dạy Scratch lớp 5");
        assertEquals("Tin học", r2.entities().get("subject"));
        assertEquals("Scratch", r2.entities().get("programmingLang"));
        assertEquals("5", r2.entities().get("grade"));
    }

    @ParameterizedTest
    @DisplayName("IntentClassifier routes open-domain queries to OPEN_DOMAIN domain")
    @CsvSource({
        "'thời tiết hôm nay thế nào', OPEN_DOMAIN, WEATHER_QUERY",
        "'hôm nay là ngày bao nhiêu', OPEN_DOMAIN, TIME_DATE_QUERY",
        "'thủ đô của nước pháp là gì', OPEN_DOMAIN, GENERAL_KNOWLEDGE",
        "'kể chuyện cười đi', OPEN_DOMAIN, ENTERTAINMENT"
    })
    void shouldRouteOpenDomainQueries(String query, AiDomain expectedDomain, AiSubIntent expectedSubIntent) {
        var detail = intentClassifier.classifyDetailed(query);
        assertEquals(expectedDomain, detail.domain());
        assertEquals(expectedSubIntent, detail.subIntent());
    }
}
