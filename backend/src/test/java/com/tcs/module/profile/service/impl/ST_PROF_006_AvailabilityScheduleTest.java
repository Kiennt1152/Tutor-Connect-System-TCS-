package com.tcs.module.profile.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.response.TutorAvailabilityResponse;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorAvailability;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorAvailabilityRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * System Test: ST-PROF-006
 * Title: Tutor declares available time slots and syncs with Google Calendar.
 *
 * Steps:
 * 1. Tutor navigates to schedule / availability section.
 * 2. Adds available time slots (dayOfWeek, startTime, endTime, recurring).
 * 3. Checks availability slots list.
 * 4. Deletes an availability slot.
 * 5. Google Calendar 2-way sync verification (Gap check).
 *
 * Expected / Reality:
 * - Backend supports CRUD for availability slots (PASS).
 * - Input validation requires dayOfWeek, startTime, endTime (PASS).
 * - Google Calendar 2-way sync: googleCalendarEventId is null / not integrated (KNOWN GAP).
 */
@Tag("system-test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ST_PROF_006_AvailabilityScheduleTest {

    private static final Long TUTOR_USER_ID = 28L; // tutor01@tcs.test
    private static final Long TUTOR_ID = 10L;
    private static final Long OTHER_TUTOR_ID = 11L;
    private static final Long AVAILABILITY_ID = 5001L;

    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private TutorAvailabilityRepository tutorAvailabilityRepository;
    @Mock private PlatformMapper platformMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User tutorUser;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("tutor01@tcs.test");

        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia sư TCS 01");

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(platformAdminRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(tutorCenterRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.TUTOR);
    }

    @Test
    @DisplayName("ST-PROF-006: Tutor declares available time slot -> Slot saved and returned")
    void testAddAvailabilitySlot_Success() {
        TutorAvailabilityRequest request = new TutorAvailabilityRequest();
        request.setDayOfWeek(2); // Monday
        request.setStartTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(20, 0));
        request.setRecurring(true);

        when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(inv -> {
            TutorAvailability a = inv.getArgument(0);
            a.setAvailabilityId(AVAILABILITY_ID);
            return a;
        });

        TutorAvailabilityResponse response = profileService.addAvailability(request);

        assertNotNull(response);
        assertEquals(AVAILABILITY_ID, response.getAvailabilityId());
        assertEquals(2, response.getDayOfWeek());
        assertEquals(LocalTime.of(18, 0), response.getStartTime());
        assertEquals(LocalTime.of(20, 0), response.getEndTime());
        assertEquals(true, response.getRecurring());
        verify(tutorAvailabilityRepository).save(any(TutorAvailability.class));
    }

    @Test
    @DisplayName("ST-PROF-006: Validation - Missing dayOfWeek or times -> Throws IllegalArgumentException")
    void testAddAvailabilitySlot_MissingRequiredFields_ThrowsException() {
        TutorAvailabilityRequest missingDay = new TutorAvailabilityRequest();
        missingDay.setStartTime(LocalTime.of(18, 0));
        missingDay.setEndTime(LocalTime.of(20, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.addAvailability(missingDay));
        assertEquals("Ngày và khung giờ là bắt buộc", ex.getMessage());

        TutorAvailabilityRequest missingStart = new TutorAvailabilityRequest();
        missingStart.setDayOfWeek(3);
        missingStart.setEndTime(LocalTime.of(20, 0));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> profileService.addAvailability(missingStart));
        assertEquals("Ngày và khung giờ là bắt buộc", ex2.getMessage());
    }

    @Test
    @DisplayName("ST-PROF-006: Query tutor availability -> Returns saved slots")
    void testGetMyAvailability_ReturnsSlots() {
        TutorAvailability slot = new TutorAvailability();
        slot.setAvailabilityId(AVAILABILITY_ID);
        slot.setTutor(tutor);
        slot.setDayOfWeek(4); // Wednesday
        slot.setStartTime(LocalTime.of(14, 0));
        slot.setEndTime(LocalTime.of(16, 0));
        slot.setRecurring(true);

        when(tutorAvailabilityRepository.findByTutor_TutorId(TUTOR_ID))
                .thenReturn(List.of(slot));

        List<TutorAvailabilityResponse> slots = profileService.getMyAvailability();

        assertEquals(1, slots.size());
        assertEquals(AVAILABILITY_ID, slots.get(0).getAvailabilityId());
        assertEquals(4, slots.get(0).getDayOfWeek());
        assertEquals(LocalTime.of(14, 0), slots.get(0).getStartTime());
    }

    @Test
    @DisplayName("ST-PROF-006: Delete availability slot -> Deletes own slot successfully")
    void testDeleteAvailabilitySlot_Success() {
        TutorAvailability slot = new TutorAvailability();
        slot.setAvailabilityId(AVAILABILITY_ID);
        slot.setTutor(tutor);

        when(tutorAvailabilityRepository.findById(AVAILABILITY_ID)).thenReturn(Optional.of(slot));

        profileService.deleteAvailability(AVAILABILITY_ID);

        verify(tutorAvailabilityRepository).delete(slot);
    }

    @Test
    @DisplayName("ST-PROF-006: Delete another tutor's availability slot -> Throws ForbiddenException")
    void testDeleteOtherTutorAvailability_ThrowsForbiddenException() {
        Tutor otherTutor = new Tutor();
        otherTutor.setTutorId(OTHER_TUTOR_ID);

        TutorAvailability otherSlot = new TutorAvailability();
        otherSlot.setAvailabilityId(AVAILABILITY_ID);
        otherSlot.setTutor(otherTutor);

        when(tutorAvailabilityRepository.findById(AVAILABILITY_ID)).thenReturn(Optional.of(otherSlot));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> profileService.deleteAvailability(AVAILABILITY_ID));
        assertEquals("Không có quyền xóa lịch này", ex.getMessage());
    }

    @Test
    @DisplayName("ST-PROF-006 Gap Check: Google Calendar sync is not implemented -> googleCalendarEventId is null")
    void testGoogleCalendarSync_DocumentsGap() {
        TutorAvailability slot = new TutorAvailability();
        slot.setAvailabilityId(AVAILABILITY_ID);
        slot.setTutor(tutor);
        slot.setDayOfWeek(5);
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        // Note: In current implementation, Google Calendar sync service does not exist,
        // and googleCalendarEventId remains null
        slot.setGoogleCalendarEventId(null);

        when(tutorAvailabilityRepository.findByTutor_TutorId(TUTOR_ID))
                .thenReturn(List.of(slot));

        List<TutorAvailabilityResponse> slots = profileService.getMyAvailability();
        assertEquals(1, slots.size());
        assertNull(slots.get(0).getGoogleCalendarEventId(),
                "Google Calendar sync is not implemented; googleCalendarEventId is expectedly null");
    }
}
