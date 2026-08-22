package com.tcs.module.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TcsSynonymServiceTest {

    @Autowired
    private TcsSynonymService synonymService;

    @Test
    void testExpandQuery_BasicSynonyms() {
        String original = "Tìm gia sư Toán";
        String expanded = synonymService.expandQuery(original);
        
        assertThat(expanded).isNotNull();
        assertThat(expanded.toLowerCase()).containsAnyOf("tìm", "kiếm", "search");
        assertThat(expanded.toLowerCase()).containsAnyOf("gia sư", "thầy", "tutor");
    }

    @Test
    void testExpandQuery_FinanceTerms() {
        String original = "Phí sàn là bao nhiêu";
        String expanded = synonymService.expandQuery(original);
        
        assertThat(expanded.toLowerCase()).containsAnyOf("phí sàn", "phí nền tảng", "platform fee");
    }

    @Test
    void testNormalizeQuery_ConvertToCanonical() {
        String original = "Tìm thầy dạy Toán";
        String normalized = synonymService.normalizeQuery(original);
        
        assertThat(normalized.toLowerCase()).contains("gia sư");
    }

    @Test
    void testNormalizeQuery_WalletTerms() {
        String original = "Làm sao để rút tiền về ngân hàng";
        String normalized = synonymService.normalizeQuery(original);
        
        assertThat(normalized.toLowerCase()).contains("rút tiền");
    }

    @Test
    void testGetSynonyms_ReturnsList() {
        List<String> synonyms = synonymService.getSynonyms("gia sư");
        
        assertThat(synonyms).isNotEmpty();
        assertThat(synonyms).contains("thầy", "tutor");
    }

    @Test
    void testGetSynonyms_ForSynonym() {
        List<String> synonyms = synonymService.getSynonyms("thầy");
        
        assertThat(synonyms).isNotEmpty();
        assertThat(synonyms).contains("gia sư");
    }

    @Test
    void testAreSemanticallyEquivalent_SameNormalized() {
        String query1 = "Tìm gia sư Toán";
        String query2 = "Tìm thầy Toán";
        
        boolean equivalent = synonymService.areSemanticallyEquivalent(query1, query2);
        
        assertThat(equivalent).isTrue();
    }

    @Test
    void testAreSemanticallyEquivalent_Different() {
        String query1 = "Tìm gia sư Toán";
        String query2 = "Xem lịch học";
        
        boolean equivalent = synonymService.areSemanticallyEquivalent(query1, query2);
        
        assertThat(equivalent).isFalse();
    }

    @Test
    void testExpandQuery_ContractTerms() {
        String original = "Ký hợp đồng cần OTP không";
        String expanded = synonymService.expandQuery(original);
        
        assertThat(expanded.toLowerCase()).containsAnyOf("hợp đồng", "contract");
        assertThat(expanded.toLowerCase()).containsAnyOf("otp", "mã xác thực");
    }

    @Test
    void testExpandQuery_EmptyQuery() {
        String result = synonymService.expandQuery("");
        assertThat(result).isEmpty();
    }

    @Test
    void testExpandQuery_NullQuery() {
        String result = synonymService.expandQuery(null);
        assertThat(result).isNull();
    }
}
