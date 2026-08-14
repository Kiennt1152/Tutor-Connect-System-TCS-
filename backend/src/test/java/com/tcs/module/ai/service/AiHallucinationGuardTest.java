package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AiHallucinationGuardTest {

    private AiHallucinationGuard guard;
    private static final String TUTOR_FALLBACK = "Hiện tại chưa tìm thấy gia sư phù hợp.";
    private static final String STATS_FALLBACK = "Không thể truy xuất thống kê.";
    private static final String FINANCE_FALLBACK = "Cần đăng nhập tài khoản gia sư.";

    @BeforeEach
    void setUp() {
        guard = new AiHallucinationGuard();
    }

    @Test
    @DisplayName("FIND_TUTOR: no sources → fallback")
    void tutorNoSources_returnsFallback() {
        String result = guard.guardTutorResponse("Gia sư A rất tốt", Collections.emptyList(), TUTOR_FALLBACK);
        assertThat(result).isEqualTo(TUTOR_FALLBACK);
    }

    @Test
    @DisplayName("FIND_TUTOR: has sources but LLM invented fake names → deterministic list")
    void tutorFakeNames_replacedWithReal() {
        var realTutors = List.of(
            TutorReferenceDto.builder().tutorId(1L).fullName("Nguyễn Thị Hoa").hourlyRate(BigDecimal.valueOf(200000)).build(),
            TutorReferenceDto.builder().tutorId(2L).fullName("Trần Văn Minh").hourlyRate(BigDecimal.valueOf(180000)).build()
        );
        String fakeResponse = "Tôi tìm thấy Gia sư A và Gia sư B phù hợp với bạn.";
        String result = guard.guardTutorResponse(fakeResponse, realTutors, TUTOR_FALLBACK);
        assertThat(result).contains("Nguyễn Thị Hoa");
        assertThat(result).contains("Trần Văn Minh");
        assertThat(result).doesNotContain("Gia sư A");
    }

    @Test
    @DisplayName("FIND_TUTOR: has sources and LLM used real names → pass through")
    void tutorRealNames_passThrough() {
        var realTutors = List.of(
            TutorReferenceDto.builder().tutorId(1L).fullName("Nguyễn Thị Hoa").build()
        );
        String goodResponse = "Nguyễn Thị Hoa là gia sư phù hợp.";
        String result = guard.guardTutorResponse(goodResponse, realTutors, TUTOR_FALLBACK);
        assertThat(result).isEqualTo(goodResponse);
    }

    @Test
    @DisplayName("PLATFORM_STATS: no DB sources → fallback")
    void statsNoSources_returnsFallback() {
        String result = guard.guardStatsResponse("Có hơn 10.000 người dùng", Collections.emptyList(), STATS_FALLBACK);
        assertThat(result).isEqualTo(STATS_FALLBACK);
    }

    @Test
    @DisplayName("PLATFORM_STATS: has DB sources → pass through")
    void statsWithSources_passThrough() {
        var sources = List.of(AiSourceResponse.builder().sourceType("PLATFORM_STATS").snippet("Tổng: 13").build());
        String response = "Hệ thống có 13 người dùng.";
        String result = guard.guardStatsResponse(response, sources, STATS_FALLBACK);
        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("PAYMENT: personal query without TUTOR login → fallback")
    void financePersonalNoLogin_returnsFallback() {
        String result = guard.guardFinanceResponse("lương của tôi tháng này", "CLIENT", 1L, FINANCE_FALLBACK);
        assertThat(result).isEqualTo(FINANCE_FALLBACK);
    }

    @Test
    @DisplayName("PAYMENT: personal query with TUTOR login → null (no guard)")
    void financePersonalWithLogin_returnsNull() {
        String result = guard.guardFinanceResponse("lương của tôi tháng này", "TUTOR", 1L, FINANCE_FALLBACK);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("PAYMENT: general query → null (no guard)")
    void financeGeneralQuery_returnsNull() {
        String result = guard.guardFinanceResponse("quy trình thanh toán", "CLIENT", 1L, FINANCE_FALLBACK);
        assertThat(result).isNull();
    }
}
