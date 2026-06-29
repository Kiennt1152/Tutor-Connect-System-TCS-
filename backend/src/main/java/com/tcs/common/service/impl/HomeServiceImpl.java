package com.tcs.common.service.impl;

import com.tcs.common.dto.response.FeaturedTutorResponse;
import com.tcs.common.dto.response.HomeResponse;
import com.tcs.common.dto.response.SubjectResponse;
import com.tcs.common.service.HomeService;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.TutorRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private static final int FEATURED_TUTOR_LIMIT = 6;
    private static final int SUBJECT_LIMIT = 12;

    private final TutorRepository tutorRepository;
    private final CategoryRepository categoryRepository;
    private final TutoringClassRepository tutoringClassRepository;

    @Override
    @Transactional(readOnly = true)
    public HomeResponse getHomeData() {
        List<FeaturedTutorResponse> featuredTutors = tutorRepository
                .findAll(PageRequest.of(0, FEATURED_TUTOR_LIMIT, Sort.by(Sort.Direction.DESC, "ratingAvg")))
                .map(this::toFeaturedTutor)
                .getContent();

        List<SubjectResponse> subjects = categoryRepository
                .findAll(PageRequest.of(0, SUBJECT_LIMIT, Sort.by(Sort.Direction.ASC, "name")))
                .map(this::toSubject)
                .getContent();

        return HomeResponse.builder()
                .totalTutors(tutorRepository.count())
                .totalSubjects(categoryRepository.count())
                .totalClasses(tutoringClassRepository.count())
                .subjects(subjects)
                .featuredTutors(featuredTutors)
                .build();
    }

    private FeaturedTutorResponse toFeaturedTutor(Tutor tutor) {
        return FeaturedTutorResponse.builder()
                .id(tutor.getTutorId() != null ? tutor.getTutorId().toString() : null)
                .fullName(tutor.getFullName())
                .gender(tutor.getGender() != null ? tutor.getGender().name() : null)
                .bio(tutor.getBio())
                .hourlyRate(tutor.getHourlyRate())
                .ratingAvg(tutor.getRatingAvg())
                .experienceYears(tutor.getExperienceYears())
                .build();
    }

    private SubjectResponse toSubject(Category category) {
        return SubjectResponse.builder()
                .id(category.getCategoryId() != null ? category.getCategoryId().toString() : null)
                .name(category.getName())
                .build();
    }
}
