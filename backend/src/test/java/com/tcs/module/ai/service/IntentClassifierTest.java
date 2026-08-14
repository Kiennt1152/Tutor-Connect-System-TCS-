package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class IntentClassifierTest {

    private IntentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new IntentClassifier();
    }

    @ParameterizedTest
    @DisplayName("Vietnamese intent classification with diacritics")
    @CsvSource({
        "'Tìm cho tôi gia sư môn Toán lớp 12 khu vực Cầu Giấy dưới 250k', FIND_TUTOR",
        "'Hệ thống có bao nhiêu người dùng?', PLATFORM_STATS",
        "'Làm sao để xem báo cáo doanh thu trên dashboard?', ADMIN_DASHBOARD",
        "'Hướng dẫn tạo ticket khiếu nại gia sư', TICKET_SUPPORT",
        "'Làm sao báo cáo gia sư lách sàn?', TICKET_SUPPORT",
        "'Khi nào nên mở tranh chấp lớp học?', TICKET_SUPPORT",
        "'xem lương gia sư của tôi như nào', PAYMENT_SUPPORT",
        "'1+1 bằng mấy?', AI_TUTORING",
        "'TCS là gì?', FAQ_SUPPORT"
    })
    void shouldClassifyVietnameseWithDiacritics(String message, AiIntent expected) {
        var result = classifier.classify(message);
        assertThat(result.intent()).isEqualTo(expected);
        assertThat(result.confidence()).isGreaterThan(0.5);
    }

    @ParameterizedTest
    @DisplayName("Vietnamese intent classification without diacritics")
    @CsvSource({
        "'tim gia su toan lop 12 cau giay duoi 250k', FIND_TUTOR",
        "'co bao nhieu nguoi dung tren he thong', PLATFORM_STATS",
        "'bao cao doanh thu dashboard', ADMIN_DASHBOARD",
        "'co lop day tiieng viet khong', FIND_CLASS",
        "'co lop toan khong', FIND_CLASS"
    })
    void shouldClassifyVietnameseWithoutDiacritics(String message, AiIntent expected) {
        var result = classifier.classify(message);
        assertThat(result.intent()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Null and empty input returns OUT_OF_SCOPE")
    void shouldReturnOutOfScopeForNullOrEmpty() {
        assertThat(classifier.classify(null).intent()).isEqualTo(AiIntent.OUT_OF_SCOPE);
        assertThat(classifier.classify("").intent()).isEqualTo(AiIntent.OUT_OF_SCOPE);
        assertThat(classifier.classify("   ").intent()).isEqualTo(AiIntent.OUT_OF_SCOPE);
    }

    @Test
    @DisplayName("ADMIN_DASHBOARD has higher priority than generic FAQ")
    void adminDashboardPriorityOverFaq() {
        var result = classifier.classify("xem báo cáo doanh thu nền tảng");
        assertThat(result.intent()).isEqualTo(AiIntent.ADMIN_DASHBOARD);
    }
}
