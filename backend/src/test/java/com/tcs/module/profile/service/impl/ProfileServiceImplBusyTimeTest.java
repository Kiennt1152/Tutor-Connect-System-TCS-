package com.tcs.module.profile.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.VerificationService;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.dto.request.TutorBusyTimeRequest;
import com.tcs.module.profile.dto.response.TutorBusyTimeResponse;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorBusyTime;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ChildProfileRepository;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.ParentChildLinkRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorAvailabilityRepository;
import com.tcs.module.profile.repository.TutorBusyTimeRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorCertificateRepository;
import com.tcs.module.profile.repository.TutorEducationRepository;
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Đăng ký thời gian bận của gia sư: chỉ lưu lúc bận, còn lại mặc định rảnh.
 * Ngày trong test tính từ hôm nay để không hỏng theo thời gian.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceImplBusyTimeTest {

    private static final Long TUTOR_USER_ID = 200L;
    private static final Long TUTOR_ID = 20L;

    @Mock private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ParentChildLinkRepository parentChildLinkRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private TutorExperienceRepository tutorExperienceRepository;
    @Mock private TutorAvailabilityRepository tutorAvailabilityRepository;
    @Mock private TutorBusyTimeRepository tutorBusyTimeRepository;
    @Mock private TutorEducationRepository tutorEducationRepository;
    @Mock private TutorCertificateRepository tutorCertificateRepository;
    @Mock private VerificationService verificationService;
    @Mock private PlatformMapper platformMapper;
    @Mock private ClientLegalAccountService clientLegalAccountService;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CccdService cccdService;

    @InjectMocks private ProfileServiceImpl service;

    private Tutor tutor;
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    /** Dựng gia sư giả đang đăng nhập và mock repository: mặc định chưa có lịch bận, saveAll gán id tăng dần. */
    @BeforeEach
    void setUp() {
        User tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("tutor1@tcs.vn");

        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(platformAdminRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(tutorCenterRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.TUTOR);

        when(tutorBusyTimeRepository.findByTutor_TutorIdAndBusyDateIn(eq(TUTOR_ID), anyCollection()))
                .thenReturn(List.of());
        when(tutorBusyTimeRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<TutorBusyTime> rows = new ArrayList<>(inv.getArgument(0));
            long id = 1;
            for (TutorBusyTime row : rows) {
                row.setBusyTimeId(id++);
            }
            return rows;
        });
    }

    /** Tạo yêu cầu đăng ký bận cho các ngày với một khoảng giờ (null = cả ngày). */
    private TutorBusyTimeRequest request(List<LocalDate> dates, LocalTime start, LocalTime end) {
        TutorBusyTimeRequest r = new TutorBusyTimeRequest();
        r.setDates(dates);
        r.setStartTime(start);
        r.setEndTime(end);
        return r;
    }

    /** Tạo một bản ghi lịch bận có sẵn của gia sư (id 99) để giả lập trùng giờ. */
    private TutorBusyTime existing(LocalDate date, LocalTime start, LocalTime end) {
        TutorBusyTime b = new TutorBusyTime();
        b.setBusyTimeId(99L);
        b.setTutor(tutor);
        b.setBusyDate(date);
        b.setStartTime(start);
        b.setEndTime(end);
        return b;
    }

    @Test
    @DisplayName("Bận cả ngày cho 2 ngày -> lưu 2 dòng, giờ để trống")
    void allDayForTwoDates() {
        List<TutorBusyTimeResponse> saved = service.addBusyTimes(
                request(List.of(tomorrow, tomorrow.plusDays(1)), null, null));

        assertEquals(2, saved.size());
        assertTrue(saved.get(0).isAllDay());
        assertNull(saved.get(0).getStartTime());
        assertNull(saved.get(0).getEndTime());
    }

    @Test
    @DisplayName("Audit log ghi mỗi ngày một dòng kèm id thật (audit_logs.entity_id NOT NULL)")
    void auditLogNeverUsesNullEntityId() {
        service.addBusyTimes(request(List.of(tomorrow, tomorrow.plusDays(1)), null, null));

        verify(auditLogService, times(2)).record(eq(TUTOR_USER_ID), eq("ADD_BUSY_TIME"), eq("TutorBusyTime"),
                anyLong(), isNull(), any());
        verify(auditLogService, never()).record(any(Long.class), eq("ADD_BUSY_TIME"), any(), isNull(), any(), any());
    }

    /** Tạo khoảng giờ bận từ startHour đến endHour (24 = nửa đêm 00:00). */
    private TutorBusyTimeRequest.TimeRange range(int startHour, int endHour) {
        TutorBusyTimeRequest.TimeRange r = new TutorBusyTimeRequest.TimeRange();
        r.setStartTime(LocalTime.of(startHour, 0));
        r.setEndTime(endHour == 24 ? LocalTime.MIDNIGHT : LocalTime.of(endHour, 0));
        return r;
    }

    @Test
    @DisplayName("Bận sáng + tối, rảnh chiều cho 2 ngày -> lưu 4 dòng, không có dòng buổi chiều")
    void morningAndEveningForTwoDates() {
        TutorBusyTimeRequest req = request(List.of(tomorrow, tomorrow.plusDays(1)), null, null);
        req.setRanges(List.of(range(18, 24), range(6, 12)));

        List<TutorBusyTimeResponse> saved = service.addBusyTimes(req);

        assertEquals(4, saved.size());
        assertTrue(saved.stream().noneMatch(TutorBusyTimeResponse::isAllDay));
        assertTrue(saved.stream().noneMatch(b -> LocalTime.of(12, 0).equals(b.getStartTime())));
        assertEquals(2, saved.stream().filter(b -> LocalTime.MIDNIGHT.equals(b.getEndTime())).count());
    }

    @Test
    @DisplayName("Các khung giờ trong cùng lượt chồng nhau -> báo lỗi, không lưu")
    void overlappingRangesInRequestRejected() {
        TutorBusyTimeRequest req = request(List.of(tomorrow), null, null);
        req.setRanges(List.of(range(6, 12), range(10, 14)));

        assertThrows(IllegalArgumentException.class, () -> service.addBusyTimes(req));
        verify(tutorBusyTimeRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Một trong các khung giờ trùng lịch bận cũ -> không lưu khung nào")
    void oneRangeClashingWithExistingRejectsWholeRequest() {
        when(tutorBusyTimeRepository.findByTutor_TutorIdAndBusyDateIn(eq(TUTOR_ID), anyCollection()))
                .thenReturn(List.of(existing(tomorrow, LocalTime.of(19, 0), LocalTime.of(21, 0))));
        TutorBusyTimeRequest req = request(List.of(tomorrow), null, null);
        req.setRanges(List.of(range(6, 12), range(18, 24)));

        assertThrows(IllegalArgumentException.class, () -> service.addBusyTimes(req));
        verify(tutorBusyTimeRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Ngày trùng trong cùng yêu cầu -> chỉ lưu một lần")
    void duplicateDatesAreCollapsed() {
        List<TutorBusyTimeResponse> saved = service.addBusyTimes(
                request(List.of(tomorrow, tomorrow), LocalTime.of(8, 0), LocalTime.of(10, 0)));

        assertEquals(1, saved.size());
    }

    @Test
    @DisplayName("Buổi tối 18:00–00:00 (nửa đêm) là khoảng hợp lệ")
    void eveningUntilMidnightIsValid() {
        List<TutorBusyTimeResponse> saved = service.addBusyTimes(
                request(List.of(tomorrow), LocalTime.of(18, 0), LocalTime.MIDNIGHT));

        assertEquals(1, saved.size());
        assertEquals(LocalTime.MIDNIGHT, saved.get(0).getEndTime());
    }

    @Test
    @DisplayName("Không chọn ngày nào -> báo lỗi")
    void noDatesRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addBusyTimes(request(List.of(), null, null)));
        verify(tutorBusyTimeRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Ngày đã qua -> báo lỗi, không lưu ngày nào")
    void pastDateRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addBusyTimes(request(List.of(LocalDate.now().minusDays(1), tomorrow), null, null)));
        verify(tutorBusyTimeRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Chỉ có giờ bắt đầu, thiếu giờ kết thúc -> báo lỗi")
    void halfRangeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addBusyTimes(request(List.of(tomorrow), LocalTime.of(8, 0), null)));
    }

    @Test
    @DisplayName("Giờ kết thúc trước giờ bắt đầu -> báo lỗi")
    void reversedRangeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addBusyTimes(request(List.of(tomorrow), LocalTime.of(10, 0), LocalTime.of(8, 0))));
    }

    @Test
    @DisplayName("Quá 31 ngày một lượt -> báo lỗi")
    void tooManyDatesRejected() {
        List<LocalDate> dates = IntStream.rangeClosed(1, 32).mapToObj(tomorrow::plusDays).toList();
        assertThrows(IllegalArgumentException.class, () -> service.addBusyTimes(request(dates, null, null)));
    }

    @Test
    @DisplayName("Chồng giờ với lịch bận cũ -> báo lỗi kèm ngày, không lưu gì")
    void overlapWithExistingRejected() {
        when(tutorBusyTimeRepository.findByTutor_TutorIdAndBusyDateIn(eq(TUTOR_ID), anyCollection()))
                .thenReturn(List.of(existing(tomorrow, LocalTime.of(8, 0), LocalTime.of(10, 0))));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addBusyTimes(request(List.of(tomorrow), LocalTime.of(9, 0), LocalTime.of(11, 0))));

        String ddmm = String.format("%02d/%02d", tomorrow.getDayOfMonth(), tomorrow.getMonthValue());
        assertTrue(ex.getMessage().contains(ddmm), ex.getMessage());
        verify(tutorBusyTimeRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Chạm mép (08:00–10:00 rồi 10:00–12:00) -> được phép")
    void touchingEdgesAllowed() {
        when(tutorBusyTimeRepository.findByTutor_TutorIdAndBusyDateIn(eq(TUTOR_ID), anyCollection()))
                .thenReturn(List.of(existing(tomorrow, LocalTime.of(8, 0), LocalTime.of(10, 0))));

        List<TutorBusyTimeResponse> saved = service.addBusyTimes(
                request(List.of(tomorrow), LocalTime.of(10, 0), LocalTime.of(12, 0)));

        assertEquals(1, saved.size());
    }

    @Test
    @DisplayName("Ngày đã bận cả ngày -> không thêm khung giờ nào nữa")
    void existingAllDayBlocksAnySlot() {
        when(tutorBusyTimeRepository.findByTutor_TutorIdAndBusyDateIn(eq(TUTOR_ID), anyCollection()))
                .thenReturn(List.of(existing(tomorrow, null, null)));

        assertThrows(IllegalArgumentException.class,
                () -> service.addBusyTimes(request(List.of(tomorrow), LocalTime.of(20, 0), LocalTime.of(21, 0))));
    }

    @Test
    @DisplayName("Xem theo tháng -> truy vấn đúng ngày đầu và cuối tháng")
    void listByMonthUsesMonthBounds() {
        YearMonth month = YearMonth.of(2026, 2);
        service.getMyBusyTimes(month);

        verify(tutorBusyTimeRepository).findByTutor_TutorIdAndBusyDateBetweenOrderByBusyDateAscStartTimeAsc(
                TUTOR_ID, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("Xoá lịch bận của gia sư khác -> Forbidden")
    void deleteOthersBusyTimeForbidden() {
        Tutor other = new Tutor();
        other.setTutorId(21L);
        TutorBusyTime busy = existing(tomorrow, null, null);
        busy.setTutor(other);
        when(tutorBusyTimeRepository.findById(99L)).thenReturn(Optional.of(busy));

        assertThrows(ForbiddenException.class, () -> service.deleteBusyTime(99L));
        verify(tutorBusyTimeRepository, never()).delete(any(TutorBusyTime.class));
    }
}
