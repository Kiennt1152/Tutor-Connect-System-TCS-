package com.tcs.module.ai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ngoài phạm vi Report 5.1: MethodList không liệt kê lớp này.
 * Đây là bộ test tích hợp / đánh giá chất lượng trợ lý AI của nhóm.
 */
class AiIntentServiceTest {

    private AiIntentService service;

    @BeforeEach
    void setUp() {
        service = new AiIntentService(new IntentClassifier());
    }

    @Test
    @DisplayName("Extract entities from accented Vietnamese")
    void shouldExtractEntitiesFromAccentedInput() {
        var result = service.classifyAndExtract("Tìm cho tôi gia sư môn Toán lớp 12 khu vực Cầu Giấy dưới 250k");
        assertThat(result.entities()).containsEntry("subject", "Toán");
        assertThat(result.entities()).containsEntry("grade", "12");
        assertThat(result.entities()).containsEntry("location", "Cầu Giấy");
        assertThat(result.entities()).containsKey("maxFee");
        assertThat(Long.parseLong(result.entities().get("maxFee"))).isEqualTo(250000L);
    }

    @Test
    @DisplayName("Extract entities from unaccented Vietnamese")
    void shouldExtractEntitiesFromUnaccentedInput() {
        var result = service.classifyAndExtract("tim gia su toan lop 12 cau giay duoi 250k");
        assertThat(result.entities()).containsKey("subject");
        assertThat(result.entities()).containsKey("grade");
        assertThat(result.entities().get("grade")).isEqualTo("12");
    }

    @Test
    @DisplayName("Extract maxFee with various formats")
    void shouldExtractMaxFeeVariousFormats() {
        // 250k
        var r1 = service.classifyAndExtract("tìm gia sư dưới 250k");
        assertThat(r1.entities().get("maxFee")).isEqualTo("250000");

        // 300 ngàn  
        var r2 = service.classifyAndExtract("tìm gia sư tầm 300 ngàn");
        assertThat(r2.entities().get("maxFee")).isEqualTo("300000");
    }

    @Test
    @DisplayName("No entities for unrelated queries")
    void shouldReturnEmptyEntitiesForUnrelatedQuery() {
        var result = service.classifyAndExtract("1+1 bằng mấy?");
        assertThat(result.entities()).doesNotContainKey("subject");
        assertThat(result.entities()).doesNotContainKey("grade");
    }
}
