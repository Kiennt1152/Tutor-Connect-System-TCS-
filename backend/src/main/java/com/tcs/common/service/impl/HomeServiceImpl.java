package com.tcs.common.service.impl;

import com.tcs.common.dto.response.FeaturedTutorResponse;
import com.tcs.common.dto.response.HomeResponse;
import com.tcs.common.dto.response.SubjectResponse;
import com.tcs.common.service.HomeService;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.SubjectRepository;
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
    private final SubjectRepository subjectRepository;
    private final TutoringClassRepository tutoringClassRepository;

    @Override
    @Transactional(readOnly = true)
    public HomeResponse getHomeData() {
        List<Subject> allSubjects = subjectRepository.findAll(Sort.by(Sort.Direction.ASC, "subjectName"));

        List<FeaturedTutorResponse> featuredTutors = tutorRepository
                .findAll(PageRequest.of(0, FEATURED_TUTOR_LIMIT, Sort.by(Sort.Direction.DESC, "ratingAvg")))
                .map(this::toFeaturedTutor)
                .getContent();

        List<SubjectResponse> subjects = allSubjects.stream()
                .limit(SUBJECT_LIMIT)
                .map(this::toSubject)
                .toList();

        return HomeResponse.builder()
                .totalTutors(tutorRepository.count())
                .totalSubjects((long) allSubjects.size())
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
                .verificationStatus(
                        tutor.getVerificationStatus() != null ? tutor.getVerificationStatus().name() : null)
                .build();
    }

    private SubjectResponse toSubject(Subject subject) {
        return SubjectResponse.builder()
                .id(subject.getSubjectId() != null ? subject.getSubjectId().toString() : null)
                .name(subject.getSubjectName())
                .build();
    }
}
