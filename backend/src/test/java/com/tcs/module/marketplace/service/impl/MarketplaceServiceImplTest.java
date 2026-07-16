package com.tcs.module.marketplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.dto.request.CreateClassTerminationRequest;
import com.tcs.module.marketplace.dto.response.ClassTerminationResponse;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassTerminationRequest;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassTerminationRequestRepository;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceImplTest {

    private static final Long CLASS_ID = 5L;
    private static final Long ASSIGNMENT_ID = 7L;
    private static final Long CLIENT_USER_ID = 11L;
    private static final Long TUTOR_USER_ID = 22L;

    @Mock
    private AuthHelper authHelper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private TutoringClassRepository tutoringClassRepository;

    @Mock
    private ClassAssignmentRepository classAssignmentRepository;

    @Mock
    private ClassTerminationRequestRepository classTerminationRequestRepository;

    @Mock
    private TutorApplicationRepository tutorApplicationRepository;

    @Mock
    private FavoriteTutorRepository favoriteTutorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    @Test
    void requestClassTerminationCreatesPendingRequestAndMarksClassDisputed() {
        User clientUser = user(CLIENT_USER_ID);
        User tutorUser = user(TUTOR_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, tutorUser);

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Gia sư cần dừng lớp sớm");
        request.setEffectiveDate(LocalDate.now().plusDays(2));

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.PENDING))
                .thenReturn(false);
        when(classTerminationRequestRepository.save(any(ClassTerminationRequest.class)))
                .thenAnswer(invocation -> {
                    ClassTerminationRequest saved = invocation.getArgument(0);
                    saved.setTerminationId(31L);
                    saved.setCreatedAt(LocalDateTime.of(2026, 7, 16, 9, 0));
                    return saved;
                });

        ClassTerminationResponse response = marketplaceService.requestClassTermination(CLASS_ID, request);

        assertEquals(31L, response.getTerminationId());
        assertEquals(CLASS_ID, response.getClassId());
        assertEquals(ASSIGNMENT_ID, response.getAssignmentId());
        assertEquals(TUTOR_USER_ID, response.getRequestedByUserId());
        assertEquals(ClassTerminationStatus.PENDING, response.getStatus());
        assertEquals(TutoringClassStatus.DISPUTED, tutoringClass.getStatus());
        verify(tutoringClassRepository).save(tutoringClass);
        verify(classTerminationRequestRepository).save(any(ClassTerminationRequest.class));
    }

    @Test
    void requestClassTerminationRejectsDuplicatePendingRequest() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, user(TUTOR_USER_ID));

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(classTerminationRequestRepository.existsByAssignment_AssignmentIdAndStatus(
                        ASSIGNMENT_ID, ClassTerminationStatus.PENDING))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        verify(tutoringClassRepository, never()).save(any());
        verify(classTerminationRequestRepository, never()).save(any());
    }

    @Test
    void requestClassTerminationRejectsNonParticipant() {
        User clientUser = user(CLIENT_USER_ID);
        User outsider = user(99L);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.IN_PROGRESS);
        ClassAssignment assignment = assignment(tutoringClass, user(TUTOR_USER_ID));

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");

        when(authHelper.currentUserId()).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(outsider));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));
        when(classAssignmentRepository.findByApplication_TutoringClass_ClassIdAndStatus(
                        CLASS_ID, ClassAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));

        assertThrows(ForbiddenException.class, () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        verify(classTerminationRequestRepository, never()).save(any());
    }

    @Test
    void requestClassTerminationRejectsClassThatIsNotInProgress() {
        User clientUser = user(CLIENT_USER_ID);
        TutoringClass tutoringClass = tutoringClass(clientUser, TutoringClassStatus.OPEN);

        CreateClassTerminationRequest request = new CreateClassTerminationRequest();
        request.setReason("Muốn dừng lớp");

        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));
        when(tutoringClassRepository.findById(CLASS_ID)).thenReturn(Optional.of(tutoringClass));

        assertThrows(BusinessException.class, () -> marketplaceService.requestClassTermination(CLASS_ID, request));
        verify(classTerminationRequestRepository, never()).save(any());
    }

    private User user(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("user" + userId + "@tcs.com");
        return user;
    }

    private TutoringClass tutoringClass(User creator, TutoringClassStatus status) {
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setClassId(CLASS_ID);
        tutoringClass.setCreator(creator);
        tutoringClass.setTitle("Lớp toán");
        tutoringClass.setDescription("Lớp toán test");
        tutoringClass.setStatus(status);
        return tutoringClass;
    }

    private ClassAssignment assignment(TutoringClass tutoringClass, User tutorUser) {
        Tutor tutor = new Tutor();
        tutor.setTutorId(44L);
        tutor.setUser(tutorUser);

        TutorApplication application = new TutorApplication();
        application.setApplicationId(55L);
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);

        ClassAssignment assignment = new ClassAssignment();
        assignment.setAssignmentId(ASSIGNMENT_ID);
        assignment.setTutor(tutor);
        assignment.setApplication(application);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        return assignment;
    }
}
