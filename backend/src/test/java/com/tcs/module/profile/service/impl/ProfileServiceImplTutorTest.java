package com.tcs.module.profile.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.VerificationService;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.request.TutorExperienceRequest;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorAvailability;
import com.tcs.module.profile.entity.TutorExperience;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ChildProfileRepository;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.ParentChildLinkRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorAvailabilityRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorCertificateRepository;
import com.tcs.module.profile.repository.TutorEducationRepository;
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Unit test module Profile — hồ sơ gia sư (kinh nghiệm, lịch rảnh, ảnh đại diện) và hồ sơ con.
 * Bám bộ test case trong Report_5.1_UnitTest: các sheet addExperience, deleteExperience,
 * addAvailability, deleteAvailability, uploadAvatar, createChild, submitVerification.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceImplTutorTest {

    private static final Long TUTOR_USER_ID = 200L;
    private static final Long TUTOR_ID = 20L;
    private static final Long OTHER_TUTOR_ID = 21L;
    private static final Long EXPERIENCE_ID = 700L;
    private static final Long AVAILABILITY_ID = 800L;

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
    @Mock private TutorEducationRepository tutorEducationRepository;
    @Mock private TutorCertificateRepository tutorCertificateRepository;
    @Mock private VerificationService verificationService;
    @Mock private PlatformMapper platformMapper;
    @Mock private ClientLegalAccountService clientLegalAccountService;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CccdService cccdService;

    @InjectMocks private ProfileServiceImpl service;

    private User tutorUser;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("tutor1@tcs.vn");

        tutor = new Tutor();
        tutor.setTutorId(TUTOR_ID);
        tutor.setUser(tutorUser);
        tutor.setFullName("Gia su 1");

        when(authHelper.currentUserId()).thenReturn(TUTOR_USER_ID);
        when(userRepository.findById(TUTOR_USER_ID)).thenReturn(Optional.of(tutorUser));
        when(platformAdminRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(tutor));
        when(tutorCenterRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.TUTOR);
    }

    private TutorExperienceRequest experience(String role, String org) {
        TutorExperienceRequest r = new TutorExperienceRequest();
        r.setRole(role);
        r.setOrganization(org);
        return r;
    }

    private TutorAvailabilityRequest availability(Integer day, LocalTime start, LocalTime end) {
        TutorAvailabilityRequest r = new TutorAvailabilityRequest();
        r.setDayOfWeek(day);
        r.setStartTime(start);
        r.setEndTime(end);
        return r;
    }

    // ===================================================================
    //  Sheet: addExperience
    // ===================================================================
    @Nested
    @DisplayName("addExperience")
    class AddExperience {

        @Test
        @DisplayName("UTCID01 (N) - Đủ chức danh + tổ chức -> lưu kinh nghiệm")
        void utcid01_addSuccessfully() {
            when(tutorExperienceRepository.save(any(TutorExperience.class))).thenAnswer(i -> {
                TutorExperience e = i.getArgument(0);
                e.setExperienceId(EXPERIENCE_ID);
                return e;
            });

            service.addExperience(experience("Gia su Toan", "Trung tam ABC"));

            verify(tutorExperienceRepository).save(any(TutorExperience.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - Thiếu chức danh -> 'Chức danh và tổ chức là bắt buộc'")
        void utcid02_missingRole() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.addExperience(experience(null, "Trung tam ABC")));
            assertEquals("Chức danh và tổ chức là bắt buộc", ex.getMessage());
            verify(tutorExperienceRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Thiếu tổ chức -> 'Chức danh và tổ chức là bắt buộc'")
        void utcid03_missingOrganization() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.addExperience(experience("Gia su Toan", "")));
        }

        @Test
        @DisplayName("UTCID04 (A) - Tài khoản không phải gia sư -> ForbiddenException")
        void utcid04_notTutorRole() {
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.addExperience(experience("Gia su Toan", "Trung tam ABC")));
            assertEquals("Không có quyền truy cập", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: deleteExperience
    // ===================================================================
    @Nested
    @DisplayName("deleteExperience")
    class DeleteExperience {

        private TutorExperience experienceOf(Long ownerTutorId) {
            Tutor owner = new Tutor();
            owner.setTutorId(ownerTutorId);
            TutorExperience e = new TutorExperience();
            e.setExperienceId(EXPERIENCE_ID);
            e.setTutor(owner);
            return e;
        }

        @Test
        @DisplayName("UTCID01 (N) - Xóa kinh nghiệm của chính mình -> thành công")
        void utcid01_deleteOwn() {
            when(tutorExperienceRepository.findById(EXPERIENCE_ID))
                    .thenReturn(Optional.of(experienceOf(TUTOR_ID)));

            service.deleteExperience(EXPERIENCE_ID);

            verify(tutorExperienceRepository).delete(any(TutorExperience.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - Xóa kinh nghiệm của gia sư khác -> ForbiddenException")
        void utcid02_deleteOfAnotherTutor() {
            when(tutorExperienceRepository.findById(EXPERIENCE_ID))
                    .thenReturn(Optional.of(experienceOf(OTHER_TUTOR_ID)));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.deleteExperience(EXPERIENCE_ID));
            assertEquals("Không có quyền xóa kinh nghiệm này", ex.getMessage());
            verify(tutorExperienceRepository, never()).delete(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Kinh nghiệm không tồn tại -> ResourceNotFoundException")
        void utcid03_notFound() {
            when(tutorExperienceRepository.findById(EXPERIENCE_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.deleteExperience(EXPERIENCE_ID));
            assertEquals("Không tìm thấy kinh nghiệm", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Tài khoản đăng nhập không có hồ sơ gia sư -> ResourceNotFoundException")
        void utcid04_noTutorProfile() {
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.deleteExperience(EXPERIENCE_ID));
            assertEquals("Không tìm thấy hồ sơ gia sư", ex.getMessage());
            verify(tutorExperienceRepository, never()).delete(any());
        }
    }

    // ===================================================================
    //  Sheet: addAvailability
    // ===================================================================
    @Nested
    @DisplayName("addAvailability")
    class AddAvailability {

        @Test
        @DisplayName("UTCID01 (N) - Đủ ngày + khung giờ -> lưu lịch rảnh")
        void utcid01_addSuccessfully() {
            when(tutorAvailabilityRepository.save(any(TutorAvailability.class))).thenAnswer(i -> {
                TutorAvailability a = i.getArgument(0);
                a.setAvailabilityId(AVAILABILITY_ID);
                return a;
            });

            service.addAvailability(availability(2, LocalTime.of(18, 0), LocalTime.of(20, 0)));

            verify(tutorAvailabilityRepository).save(any(TutorAvailability.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - Thiếu ngày -> 'Ngày và khung giờ là bắt buộc'")
        void utcid02_missingDay() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.addAvailability(availability(null, LocalTime.of(18, 0), LocalTime.of(20, 0))));
            assertEquals("Ngày và khung giờ là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Thiếu giờ bắt đầu -> chặn")
        void utcid03_missingStartTime() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.addAvailability(availability(2, null, LocalTime.of(20, 0))));
        }

        @Test
        @DisplayName("UTCID04 (A) - Thiếu giờ kết thúc -> chặn")
        void utcid04_missingEndTime() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.addAvailability(availability(2, LocalTime.of(18, 0), null)));
        }
    }

    // ===================================================================
    //  Sheet: deleteAvailability
    // ===================================================================
    @Nested
    @DisplayName("deleteAvailability")
    class DeleteAvailability {

        private TutorAvailability availabilityOf(Long ownerTutorId) {
            Tutor owner = new Tutor();
            owner.setTutorId(ownerTutorId);
            TutorAvailability a = new TutorAvailability();
            a.setAvailabilityId(AVAILABILITY_ID);
            a.setTutor(owner);
            return a;
        }

        @Test
        @DisplayName("UTCID01 (N) - Xóa lịch của chính mình -> thành công")
        void utcid01_deleteOwn() {
            when(tutorAvailabilityRepository.findById(AVAILABILITY_ID))
                    .thenReturn(Optional.of(availabilityOf(TUTOR_ID)));

            service.deleteAvailability(AVAILABILITY_ID);

            verify(tutorAvailabilityRepository).delete(any(TutorAvailability.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - Xóa lịch của gia sư khác -> ForbiddenException")
        void utcid02_deleteOfAnotherTutor() {
            when(tutorAvailabilityRepository.findById(AVAILABILITY_ID))
                    .thenReturn(Optional.of(availabilityOf(OTHER_TUTOR_ID)));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.deleteAvailability(AVAILABILITY_ID));
            assertEquals("Không có quyền xóa lịch này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Lịch không tồn tại -> ResourceNotFoundException")
        void utcid03_notFound() {
            when(tutorAvailabilityRepository.findById(AVAILABILITY_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteAvailability(AVAILABILITY_ID));
        }
    }

    // ===================================================================
    //  Sheet: uploadAvatar
    // ===================================================================
    @Nested
    @DisplayName("uploadAvatar")
    class UploadAvatar {

        @Test
        @DisplayName("UTCID02 (A) - File rỗng -> 'File ảnh không được để trống'")
        void utcid01_emptyFile() {
            MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.uploadAvatar(empty));
            assertEquals("File ảnh không được để trống", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID02 (A) - File null -> 'File ảnh không được để trống'")
        void utcid02_nullFile() {
            assertThrows(IllegalArgumentException.class, () -> service.uploadAvatar(null));
        }

        @Test
        @DisplayName("UTCID03 (B) - File vượt 5MB -> 'Kích thước ảnh không được vượt quá 5MB'")
        void utcid03_fileTooLarge() {
            byte[] big = new byte[5 * 1024 * 1024 + 1];
            MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.uploadAvatar(file));
            assertEquals("Kích thước ảnh không được vượt quá 5MB", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Không phải file ảnh (nội dung text) -> chặn theo magic number")
        void utcid04_notAnImage() {
            MockMultipartFile fake =
                    new MockMultipartFile("file", "fake.png", "image/png", "day khong phai anh".getBytes());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.uploadAvatar(fake));
            assertEquals("Chỉ chấp nhận file ảnh (JPEG, PNG, WEBP, GIF)", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: createChild
    // ===================================================================
    @Nested
    @DisplayName("createChild")
    class CreateChild {

        /** Đăng nhập bằng tài khoản phụ huynh (CLIENT) với tuổi cho trước. */
        private void loginAsClientAged(int age) {
            com.tcs.module.profile.entity.Client client = new com.tcs.module.profile.entity.Client();
            client.setClientId(9L);
            client.setUser(tutorUser);
            client.setFullName("Phu huynh A");
            client.setDateOfBirth(java.time.LocalDate.now().minusYears(age));
            when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(client));
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        }

        @Test
        @DisplayName("UTCID02 (A) - Phụ huynh đủ 18 tuổi, dữ liệu null -> 'Dữ liệu hồ sơ con là bắt buộc'")
        void utcid01_nullRequest() {
            loginAsClientAged(30);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createChild(null));
            assertEquals("Dữ liệu hồ sơ con là bắt buộc", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (A) - Tài khoản gia sư -> ForbiddenException (không quản lý hồ sơ con)")
        void utcid02_tutorCannotCreateChild() {
            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.createChild(null));
            assertEquals("Không có quyền truy cập", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (B) - Phụ huynh dưới 18 tuổi -> không được quản lý hồ sơ con")
        void utcid03_minorParentBlocked() {
            loginAsClientAged(16);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.createChild(null));
            assertEquals("Chỉ tài khoản phụ huynh từ 18 tuổi trở lên mới quản lý hồ sơ con", ex.getMessage());
        }
    }
    /** Anh PNG hop le toi thieu (chi can magic number de FileMagicDetector nhan dien). */
    private static byte[] pngBytes() {
        byte[] png = new byte[64];
        png[0] = (byte) 0x89;
        png[1] = 'P';
        png[2] = 'N';
        png[3] = 'G';
        png[4] = 0x0D;
        png[5] = 0x0A;
        png[6] = 0x1A;
        png[7] = 0x0A;
        return png;
    }

    // ===================================================================
    //  Sheet: uploadAvatar - cac ca con lai
    // ===================================================================
    @Nested
    @DisplayName("uploadAvatar")
    class UploadAvatarRemaining {

        @Test
        @DisplayName("UTCID01 (N) - Anh hop le, duoi 5MB, dung dinh dang -> luu anh va tra ve duong dan moi")
        void utcid01_uploadSuccessfully(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) {
            org.springframework.test.util.ReflectionTestUtils.setField(
                    service, "storagePath", tempDir.toString());
            MockMultipartFile file =
                    new MockMultipartFile("file", "avatar.png", "image/png", pngBytes());

            String avatarUrl = service.uploadAvatar(file);

            assertEquals("/uploads/avatars/user-" + TUTOR_USER_ID + ".png", avatarUrl);
            assertEquals(avatarUrl, tutor.getAvatar());
            verify(tutorRepository).save(tutor);
        }

        @Test
        @DisplayName("UTCID05 (A) - Loi khi doc file -> RuntimeException 'Không thể đọc file ảnh'")
        void utcid05_readError() throws Exception {
            org.springframework.web.multipart.MultipartFile broken =
                    org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
            when(broken.isEmpty()).thenReturn(false);
            when(broken.getSize()).thenReturn(1024L);
            when(broken.getInputStream()).thenThrow(new java.io.IOException("o dia loi"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.uploadAvatar(broken));
            assertEquals("Không thể đọc file ảnh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - Loi khi ghi file xuong dia -> RuntimeException 'Không thể lưu ảnh đại diện'")
        void utcid06_writeError(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
            // Bien thu muc luu tru thanh mot FILE thuong -> Files.createDirectories nem IOException.
            java.nio.file.Path notADirectory = tempDir.resolve("storage-as-file");
            java.nio.file.Files.writeString(notADirectory, "khong phai thu muc");
            org.springframework.test.util.ReflectionTestUtils.setField(
                    service, "storagePath", notADirectory.toString());
            MockMultipartFile file =
                    new MockMultipartFile("file", "avatar.png", "image/png", pngBytes());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.uploadAvatar(file));
            assertEquals("Không thể lưu ảnh đại diện", ex.getMessage());
        }
    }

    // ===================================================================
    //  Sheet: createChild - cac ca con lai
    // ===================================================================
    @Nested
    @DisplayName("createChild")
    class CreateChildRemaining {

        private com.tcs.module.profile.entity.Client parent;

        private void loginAsAdultParent() {
            parent = new com.tcs.module.profile.entity.Client();
            parent.setClientId(9L);
            parent.setUser(tutorUser);
            parent.setFullName("Phu huynh A");
            parent.setDateOfBirth(java.time.LocalDate.now().minusYears(30));
            when(clientRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.of(parent));
            when(tutorRepository.findByUser_UserId(TUTOR_USER_ID)).thenReturn(Optional.empty());
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        }

        private com.tcs.module.profile.dto.request.ChildProfileRequest childRequest() {
            var request = new com.tcs.module.profile.dto.request.ChildProfileRequest();
            request.setFullName("Nguyen Van Con");
            request.setDateOfBirth(java.time.LocalDate.now().minusYears(10));
            return request;
        }

        @Test
        @DisplayName("UTCID01 (N) - Phu huynh du 18 tuoi, du lieu hop le, chua trung -> tao ho so con gan voi tai khoan cha me")
        void utcid01_createSuccessfully() {
            loginAsAdultParent();
            when(parentChildLinkRepository
                    .findFirstByParentUser_UserIdAndChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
                            any(), any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(childProfileRepository.save(any(com.tcs.module.profile.entity.ChildProfile.class)))
                    .thenAnswer(i -> {
                        var saved = (com.tcs.module.profile.entity.ChildProfile) i.getArgument(0);
                        saved.setChildProfileId(500L);
                        return saved;
                    });

            service.createChild(childRequest());

            verify(childProfileRepository).save(any(com.tcs.module.profile.entity.ChildProfile.class));
            verify(parentChildLinkRepository)
                    .save(any(com.tcs.module.profile.entity.ParentChildLink.class));
        }

        @Test
        @DisplayName("UTCID05 (A) - Trung ho ten va ngay sinh voi ho so con da co -> chan")
        void utcid05_duplicateChild() {
            loginAsAdultParent();
            var existingChild = new com.tcs.module.profile.entity.ChildProfile();
            existingChild.setChildProfileId(501L);
            var existingLink = new com.tcs.module.profile.entity.ParentChildLink();
            existingLink.setChildProfile(existingChild);
            when(parentChildLinkRepository
                    .findFirstByParentUser_UserIdAndChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
                            any(), any(), any(), any()))
                    .thenReturn(Optional.of(existingLink));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.createChild(childRequest()));
            assertEquals("Đã tồn tại hồ sơ con với cùng họ tên và ngày sinh", ex.getMessage());
            verify(childProfileRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: updateMyProfile
    // ===================================================================
    @Nested
    @DisplayName("updateMyProfile")
    class UpdateMyProfile {

        @Test
        @DisplayName("UTCID01 (N) - vai tro co ho so tuong ung -> cap nhat theo vai tro va luu lai")
        void utcid01_updateTutorProfile() {
            var request = new com.tcs.module.profile.dto.request.UpdateProfileRequest();
            request.setFullName("Gia su moi");
            request.setBio("Toi day Toan 9");
            when(cccdService.getByUserId(any()))
                    .thenReturn(new com.tcs.module.profile.dto.CccdInfoDto());

            service.updateMyProfile(request);

            assertEquals("Gia su moi", tutor.getFullName());
            assertEquals("Toi day Toan 9", tutor.getBio());
            verify(tutorRepository).save(tutor);
        }

        @Test
        @DisplayName("UTCID02 (A) - vai tro khong co ho so de cap nhat -> ForbiddenException")
        void utcid02_roleWithoutProfile() {
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.PLATFORM_ADMIN);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.updateMyProfile(
                            new com.tcs.module.profile.dto.request.UpdateProfileRequest()));
            assertEquals("Không thể cập nhật hồ sơ cho vai trò này", ex.getMessage());
            verify(tutorRepository, never()).save(any());
        }
    }
}
