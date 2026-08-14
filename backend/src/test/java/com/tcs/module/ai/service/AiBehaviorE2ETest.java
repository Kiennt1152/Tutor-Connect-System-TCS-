package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.provider.AiClassSearchContextProvider;
import com.tcs.module.ai.service.provider.AiTutorSearchContextProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiBehaviorE2ETest {

    private AiFallbackService fallbackService;
    private AiTutorSearchContextProvider tutorSearchContextProvider;
    private AiClassSearchContextProvider classSearchContextProvider;

    @BeforeEach
    void setUp() {
        fallbackService = new AiFallbackService();
        tutorSearchContextProvider = new AiTutorSearchContextProvider(null);
        classSearchContextProvider = new AiClassSearchContextProvider(null);
    }

    @Test
    @DisplayName("Level 0 Fast-Path: Greeting, Frustration, and Gibberish never output 1+1 or error")
    void testLevel0SafetyFastPathResponses() {
        var greeting = fallbackService.checkLevel0Safety(AiSubIntent.GREETING);
        assertThat(greeting).isNotNull();
        assertThat(greeting.message()).contains("Xin chào");

        var profanity = fallbackService.checkLevel0Safety(AiSubIntent.PROFANITY_OR_FRUSTRATION);
        assertThat(profanity).isNotNull();
        assertThat(profanity.message()).contains("văn minh");

        var gibberish = fallbackService.checkLevel0Safety(AiSubIntent.GIBBERISH);
        assertThat(gibberish).isNotNull();
        assertThat(gibberish.message()).contains("chưa hiểu rõ");

        var human = fallbackService.checkLevel0Safety(AiSubIntent.HUMAN_SUPPORT_REQUEST);
        assertThat(human).isNotNull();
        assertThat(human.suggestedRoute()).isEqualTo("/support/tickets");
    }

    @Test
    @DisplayName("Deterministic Tutor Search: Renders verified real tutor details without hallucinations")
    void testTutorDeterministicRendering() {
        var tutor = TutorReferenceDto.builder()
                .tutorId(101L)
                .fullName("Trần Thị Bích")
                .title("Gia sư Toán chuyên nghiệp")
                .hourlyRate(new BigDecimal("220000"))
                .averageRating(4.9)
                .teachingAreas("Cầu Giấy, Hà Nội")
                .build();

        String answer = tutorSearchContextProvider.renderDeterministicAnswer(List.of(tutor));
        assertThat(answer).contains("Trần Thị Bích");
        assertThat(answer).contains("220,000");
        assertThat(answer).doesNotContain("Gia sư A");
        assertThat(answer).doesNotContain("Gia sư B");
    }

    @Test
    @DisplayName("Deterministic Class Search: Renders OPEN class details without hallucinations")
    void testClassDeterministicRendering() {
        var clazz = ClassReferenceDto.builder()
                .classId(55L)
                .title("Lớp Toán 12 Cầu Giấy")
                .subjectName("Toán")
                .gradeLevelName("12")
                .location("Cầu Giấy")
                .tuitionFee(new BigDecimal("250000"))
                .build();

        String answer = classSearchContextProvider.renderDeterministicAnswer(List.of(clazz));
        assertThat(answer).contains("Lớp Toán 12 Cầu Giấy");
        assertThat(answer).contains("250,000");
    }

    @Test
    @DisplayName("Level 3 No Data Fallback: Explains zero results truthfully without fabricating")
    void testNoDataFallbackTruthfulness() {
        var tutorFallback = fallbackService.getLevel3NoData(AiSubIntent.FIND_TUTOR, Map.of("subject", "Tiếng Pháp", "location", "Hà Giang"));
        assertThat(tutorFallback.message()).contains("chưa tìm thấy gia sư");
        assertThat(tutorFallback.message()).contains("Tiếng Pháp");
        assertThat(tutorFallback.suggestedRoute()).isEqualTo("/tao-lop");

        var classFallback = fallbackService.getLevel3NoData(AiSubIntent.FIND_CLASS, Map.of("subject", "Hóa", "grade", "6"));
        assertThat(classFallback.message()).contains("chưa có lớp học nào đang mở");
        assertThat(classFallback.suggestedRoute()).isEqualTo("/lop-hoc");
    }

    @Test
    @DisplayName("Level 4 Permission Fallback: Clear guidance and deep-link route")
    void testPermissionFallbackGuidance() {
        var authFallback = fallbackService.getLevel4AuthRoleRequired("Gia sư", "/finance");
        assertThat(authFallback.message()).contains("cần đăng nhập với vai trò");
        assertThat(authFallback.suggestedRoute()).isEqualTo("/finance");
    }
}
