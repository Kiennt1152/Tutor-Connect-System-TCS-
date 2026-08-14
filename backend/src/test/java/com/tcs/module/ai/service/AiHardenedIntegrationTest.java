package com.tcs.module.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.provider.AiClassSearchContextProvider;
import com.tcs.module.ai.service.provider.AiTutorSearchContextProvider;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.repository.TutorRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiHardenedIntegrationTest {

    private TutorRepository tutorRepository;
    private TutoringClassRepository tutoringClassRepository;
    private AiTutorSearchContextProvider tutorSearchProvider;
    private AiClassSearchContextProvider classSearchProvider;

    @BeforeEach
    void setUp() {
        tutorRepository = mock(TutorRepository.class);
        tutoringClassRepository = mock(TutoringClassRepository.class);
        tutorSearchProvider = new AiTutorSearchContextProvider(tutorRepository);
        classSearchProvider = new AiClassSearchContextProvider(tutoringClassRepository);
    }

    @Test
    @DisplayName("Hard Filter: Tìm gia sư Toán Cầu Giấy dưới 250k chỉ trả về gia sư Toán Cầu Giấy <= 250k")
    void searchTutors_withHardFilters_excludesUnmatchedTutors() {
        // Given
        Tutor tMath = new Tutor();
        tMath.setTutorId(101L);
        tMath.setFullName("Nguyễn Văn Toán");
        tMath.setBio("Chuyên gia dạy môn Toán lớp 12 luyện thi THPT Quốc gia");
        tMath.setAddress("Cầu Giấy, Hà Nội");
        tMath.setHourlyRate(new BigDecimal("200000"));
        tMath.setRatingAvg(new BigDecimal("5.0"));
        tMath.setAvatar("https://example.com/u1.jpg");

        Tutor tPhysics = new Tutor();
        tPhysics.setTutorId(102L);
        tPhysics.setFullName("Trần Thị Lý");
        tPhysics.setBio("Chuyên gia dạy môn Vật Lý cấp 3 và luyện thi");
        tPhysics.setAddress("Đống Đa, Hà Nội");
        tPhysics.setHourlyRate(new BigDecimal("220000"));
        tPhysics.setRatingAvg(new BigDecimal("4.9"));
        tPhysics.setAvatar("https://example.com/u2.jpg");

        Tutor tExpensiveMath = new Tutor();
        tExpensiveMath.setTutorId(103L);
        tExpensiveMath.setFullName("Lê Minh Toán");
        tExpensiveMath.setBio("Thầy giáo dạy Toán giỏi");
        tExpensiveMath.setAddress("Cầu Giấy, Hà Nội");
        tExpensiveMath.setHourlyRate(new BigDecimal("350000")); // Exceeds 250k
        tExpensiveMath.setRatingAvg(new BigDecimal("5.0"));
        tExpensiveMath.setAvatar("https://example.com/u3.jpg");

        when(tutorRepository.findByUser_StatusAndVerificationStatus(UserStatus.ACTIVE, ProfileVerificationStatus.VERIFIED))
                .thenReturn(List.of(tMath, tPhysics, tExpensiveMath));

        // When
        Map<String, String> entities = Map.of(
                "subject", "Toán",
                "location", "Cầu Giấy",
                "maxFee", "250000"
        );
        List<AiSourceResponse> results = tutorSearchProvider.searchTutors(entities);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSourceId()).isEqualTo("101");
        assertThat(results.get(0).getTitle()).isEqualTo("Nguyễn Văn Toán");
        assertThat(results.get(0).getSnippet()).contains("Nguyễn Văn Toán");
        assertThat(results.get(0).getSnippet()).doesNotContain("Trần Thị Lý");
    }

    @Test
    @DisplayName("Hard Filter: Tìm gia sư không có ai khớp tiêu chí trả về danh sách rỗng (kích hoạt L3 Fallback)")
    void searchTutors_whenNoTutorsMatch_returnsEmptyList() {
        Tutor tMath = new Tutor();
        tMath.setTutorId(101L);
        tMath.setFullName("Nguyễn Văn Toán");
        tMath.setBio("Chuyên gia dạy môn Toán");
        tMath.setAddress("Hà Nội");
        tMath.setHourlyRate(new BigDecimal("200000"));

        when(tutorRepository.findByUser_StatusAndVerificationStatus(any(), any()))
                .thenReturn(List.of(tMath));

        // When searching for French in Ha Giang
        Map<String, String> entities = Map.of(
                "subject", "Tiếng Pháp",
                "location", "Hà Giang"
        );
        List<AiSourceResponse> results = tutorSearchProvider.searchTutors(entities);

        // Then: 0 results returned, no fake tutors hallucinated
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Hard Filter: Tìm lớp môn Toán lớp 12 chỉ trả về lớp Toán 12, loại trừ lớp Lý")
    void searchClasses_withHardFilters_excludesUnmatchedClasses() {
        TutoringClass cMath = new TutoringClass();
        cMath.setClassId(201L);
        cMath.setTitle("Tìm gia sư kèm Toán 12");
        cMath.setAddress("Cầu Giấy, Hà Nội");
        cMath.setTuitionFee(new BigDecimal("200000"));
        cMath.setStatus(TutoringClassStatus.OPEN);

        TutoringClass cPhysics = new TutoringClass();
        cPhysics.setClassId(202L);
        cPhysics.setTitle("Tìm gia sư kèm Vật Lý 12");
        cPhysics.setAddress("Cầu Giấy, Hà Nội");
        cPhysics.setTuitionFee(new BigDecimal("200000"));
        cPhysics.setStatus(TutoringClassStatus.OPEN);

        when(tutoringClassRepository.findByStatus(TutoringClassStatus.OPEN))
                .thenReturn(List.of(cMath, cPhysics));

        // When
        Map<String, String> entities = Map.of(
                "subject", "Toán",
                "grade", "12",
                "location", "Cầu Giấy"
        );
        List<AiSourceResponse> results = classSearchProvider.searchClasses(entities);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSourceId()).isEqualTo("201");
        assertThat(results.get(0).getTitle()).contains("Toán");
    }

    @Test
    @DisplayName("Deterministic Rendering: Tutor & Class answers format fields rich and cleanly")
    void renderDeterministicAnswer_formatsRichFields() {
        TutorReferenceDto tDto = TutorReferenceDto.builder()
                .tutorId(101L)
                .fullName("Nguyễn Văn Toán")
                .hourlyRate(new BigDecimal("200000"))
                .averageRating(5.0)
                .teachingAreas("Cầu Giấy, Hà Nội")
                .build();

        String answer = tutorSearchProvider.renderDeterministicAnswer(List.of(tDto));
        assertThat(answer).contains("Nguyễn Văn Toán");
        assertThat(answer).contains("200,000 ₫/buổi");
        assertThat(answer).contains("5.0★");
        assertThat(answer).contains("Cầu Giấy, Hà Nội");

        ClassReferenceDto cDto = ClassReferenceDto.builder()
                .classId(201L)
                .title("Tìm gia sư kèm Toán 12")
                .subjectName("Toán")
                .gradeLevelName("Lớp 12")
                .tuitionFee(new BigDecimal("250000"))
                .location("Cầu Giấy")
                .build();

        String classAnswer = classSearchProvider.renderDeterministicAnswer(List.of(cDto));
        assertThat(classAnswer).contains("Tìm gia sư kèm Toán 12");
        assertThat(classAnswer).contains("Toán");
        assertThat(classAnswer).contains("250,000 ₫/buổi");
        assertThat(classAnswer).contains("Cầu Giấy");
    }

    @Test
    @DisplayName("Intent Classifier: English and Slang/Teencode queries classified accurately")
    void intentClassifier_classifiesEnglishAndSlangAccurately() {
        IntentClassifier classifier = new IntentClassifier();

        // English
        var r1 = classifier.classifyDetailed("find math classes open");
        assertThat(r1.domain()).isEqualTo(AiDomain.MARKETPLACE);
        assertThat(r1.subIntent()).isEqualTo(AiSubIntent.FIND_CLASS);

        var r2 = classifier.classifyDetailed("need a tutor for math");
        assertThat(r2.domain()).isEqualTo(AiDomain.MARKETPLACE);
        assertThat(r2.subIntent()).isEqualTo(AiSubIntent.FIND_TUTOR);

        var r3 = classifier.classifyDetailed("solve equation 2x + 5 = 11");
        assertThat(r3.domain()).isEqualTo(AiDomain.AI_TUTORING);

        // Slang / Teencode
        var r4 = classifier.classifyDetailed("tim gs toan");
        assertThat(r4.domain()).isEqualTo(AiDomain.MARKETPLACE);
        assertThat(r4.subIntent()).isEqualTo(AiSubIntent.FIND_TUTOR);

        var r5 = classifier.classifyDetailed("can thue gia su hoa");
        assertThat(r5.domain()).isEqualTo(AiDomain.MARKETPLACE);
        assertThat(r5.subIntent()).isEqualTo(AiSubIntent.FIND_TUTOR);
    }
}
