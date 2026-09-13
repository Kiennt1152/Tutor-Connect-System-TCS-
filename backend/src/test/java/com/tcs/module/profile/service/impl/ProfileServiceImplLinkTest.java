package com.tcs.module.profile.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.identity.dto.request.VerificationRequestDto;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.VerificationService;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.dto.request.LinkChildAccountRequest;
import com.tcs.module.profile.dto.request.LinkChildRequest;
import com.tcs.module.profile.dto.request.LinkGuardianRequest;
import com.tcs.module.profile.entity.ChildProfile;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.ParentChildLink;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.ParentChildLinkStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ChildProfileRepository;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.ParentChildLinkRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorAvailabilityRepository;
import com.tcs.module.profile.repository.TutorCertificateRepository;
import com.tcs.module.profile.repository.TutorEducationRepository;
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.CccdService;
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test module Profile — lien ket phu huynh / con va nop ho so xac minh.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: cac sheet linkChild, linkChildAccount,
 * linkGuardian va pfSubmitVerification.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceImplLinkTest {

    private static final Long PARENT_USER_ID = 100L;
    private static final Long CHILD_USER_ID = 101L;
    private static final Long CHILD_PROFILE_ID = 700L;
    private static final String PARENT_EMAIL = "phuhuynh@tcs.vn";
    private static final String CHILD_EMAIL = "hocsinh@tcs.vn";

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

    private User parentUser;
    private Client parentClient;

    @BeforeEach
    void setUp() {
        parentUser = user(PARENT_USER_ID, PARENT_EMAIL);
        parentClient = client(parentUser, "Nguyen Van Bo", LocalDate.now().minusYears(40));

        // Mac dinh: dang nhap bang tai khoan phu huynh (CLIENT, da du 18 tuoi).
        when(authHelper.currentUserId()).thenReturn(PARENT_USER_ID);
        when(userRepository.findById(PARENT_USER_ID)).thenReturn(Optional.of(parentUser));
        when(platformAdminRepository.findByUser_UserId(PARENT_USER_ID)).thenReturn(Optional.empty());
        when(tutorRepository.findByUser_UserId(PARENT_USER_ID)).thenReturn(Optional.empty());
        when(tutorCenterRepository.findByUser_UserId(PARENT_USER_ID)).thenReturn(Optional.empty());
        when(clientRepository.findByUser_UserId(PARENT_USER_ID)).thenReturn(Optional.of(parentClient));
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
    }

    private User user(Long id, String email) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        return u;
    }

    private Client client(User owner, String fullName, LocalDate dateOfBirth) {
        Client c = new Client();
        c.setClientId(owner.getUserId());
        c.setUser(owner);
        c.setFullName(fullName);
        c.setDateOfBirth(dateOfBirth);
        return c;
    }

    private ChildProfile childProfile(Long id, String fullName, LocalDate dateOfBirth) {
        ChildProfile child = new ChildProfile();
        child.setChildProfileId(id);
        child.setFullName(fullName);
        child.setDateOfBirth(dateOfBirth);
        return child;
    }

    private ParentChildLink link(User parent, ChildProfile child) {
        ParentChildLink l = new ParentChildLink();
        l.setLinkId(900L);
        l.setParentUser(parent);
        l.setChildProfile(child);
        l.setStatus(ParentChildLinkStatus.ACTIVE);
        return l;
    }

    // ===================================================================
    //  Sheet: linkChild (phu huynh lien ket ho so con co san)
    // ===================================================================
    @Nested
    @DisplayName("linkChild")
    class LinkChild {

        private LinkChildRequest request() {
            LinkChildRequest r = new LinkChildRequest();
            r.setChildProfileId(CHILD_PROFILE_ID);
            return r;
        }

        @Test
        @DisplayName("UTCID01 (N) - CLIENT lien ket ho so con chua tung lien ket -> luu ParentChildLink ACTIVE")
        void utcid01_linkSuccessfully() {
            ChildProfile child = childProfile(CHILD_PROFILE_ID, "Nguyen Van Con", LocalDate.now().minusYears(12));
            when(childProfileRepository.findById(CHILD_PROFILE_ID)).thenReturn(Optional.of(child));
            when(parentChildLinkRepository
                    .existsByParentUser_UserIdAndChildProfile_ChildProfileIdAndStatus(
                            PARENT_USER_ID, CHILD_PROFILE_ID, ParentChildLinkStatus.ACTIVE))
                    .thenReturn(false);

            service.linkChild(request());

            ArgumentCaptor<ParentChildLink> captor = ArgumentCaptor.forClass(ParentChildLink.class);
            verify(parentChildLinkRepository).save(captor.capture());
            assertEquals(ParentChildLinkStatus.ACTIVE, captor.getValue().getStatus());
            assertEquals(PARENT_USER_ID, captor.getValue().getParentUser().getUserId());
            verify(auditLogService).record(
                    org.mockito.ArgumentMatchers.eq(PARENT_USER_ID),
                    org.mockito.ArgumentMatchers.eq("LINK_CHILD_PROFILE"),
                    org.mockito.ArgumentMatchers.eq("ChildProfile"),
                    org.mockito.ArgumentMatchers.eq(CHILD_PROFILE_ID),
                    org.mockito.ArgumentMatchers.isNull(),
                    any());
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong co vai tro CLIENT -> ForbiddenException")
        void utcid02_notAClient() {
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.TUTOR);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.linkChild(request()));
            assertEquals("Không có quyền truy cập", ex.getMessage());
            verify(parentChildLinkRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - childProfileId khong khop ho so con nao -> 'Không tìm thấy hồ sơ con'")
        void utcid03_childProfileNotFound() {
            when(childProfileRepository.findById(CHILD_PROFILE_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.linkChild(request()));
            assertEquals("Không tìm thấy hồ sơ con", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Da co lien ket ACTIVE giua phu huynh nay va ho so con -> 'Hồ sơ con đã được liên kết'")
        void utcid04_alreadyLinked() {
            ChildProfile child = childProfile(CHILD_PROFILE_ID, "Nguyen Van Con", LocalDate.now().minusYears(12));
            when(childProfileRepository.findById(CHILD_PROFILE_ID)).thenReturn(Optional.of(child));
            when(parentChildLinkRepository
                    .existsByParentUser_UserIdAndChildProfile_ChildProfileIdAndStatus(
                            PARENT_USER_ID, CHILD_PROFILE_ID, ParentChildLinkStatus.ACTIVE))
                    .thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkChild(request()));
            assertEquals("Hồ sơ con đã được liên kết", ex.getMessage());
            verify(parentChildLinkRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: linkChildAccount (phu huynh lien ket TAI KHOAN con vi thanh nien)
    // ===================================================================
    @Nested
    @DisplayName("linkChildAccount")
    class LinkChildAccount {

        private User childUser;
        private Client childClient;

        @BeforeEach
        void initChildAccount() {
            childUser = user(CHILD_USER_ID, CHILD_EMAIL);
            childClient = client(childUser, "Nguyen Van Con", LocalDate.now().minusYears(12));
            when(userRepository.findByEmail(CHILD_EMAIL)).thenReturn(Optional.of(childUser));
            when(clientRepository.findByUser_UserId(CHILD_USER_ID)).thenReturn(Optional.of(childClient));
            when(clientLegalAccountService.findGuardianLinkForMinor(childClient)).thenReturn(Optional.empty());
            when(childProfileRepository.findFirstByFullNameAndDateOfBirth(
                    childClient.getFullName(), childClient.getDateOfBirth()))
                    .thenReturn(Optional.of(childProfile(CHILD_PROFILE_ID,
                            childClient.getFullName(), childClient.getDateOfBirth())));
        }

        private LinkChildAccountRequest request(String email) {
            LinkChildAccountRequest r = new LinkChildAccountRequest();
            r.setChildEmail(email);
            return r;
        }

        @Test
        @DisplayName("UTCID01 (N) - Phu huynh du 18, tai khoan con vi thanh nien chua lien ket -> luu lien ket ACTIVE")
        void utcid01_linkSuccessfully() {
            service.linkChildAccount(request(CHILD_EMAIL));

            ArgumentCaptor<ParentChildLink> captor = ArgumentCaptor.forClass(ParentChildLink.class);
            verify(parentChildLinkRepository).save(captor.capture());
            assertEquals(ParentChildLinkStatus.ACTIVE, captor.getValue().getStatus());
            assertEquals(PARENT_USER_ID, captor.getValue().getParentUser().getUserId());
            assertEquals(CHILD_PROFILE_ID, captor.getValue().getChildProfile().getChildProfileId());
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong co vai tro CLIENT -> ForbiddenException")
        void utcid02_notAClient() {
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.TUTOR);

            assertThrows(ForbiddenException.class, () -> service.linkChildAccount(request(CHILD_EMAIL)));
            verify(parentChildLinkRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Phu huynh thieu ngay sinh hoac duoi 18 -> 'Chỉ tài khoản phụ huynh từ 18 tuổi trở lên ...'")
        void utcid03_parentNotAdult() {
            parentClient.setDateOfBirth(null);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.linkChildAccount(request(CHILD_EMAIL)));
            assertEquals("Chỉ tài khoản phụ huynh từ 18 tuổi trở lên mới quản lý hồ sơ con", ex.getMessage());

            parentClient.setDateOfBirth(LocalDate.now().minusYears(15));
            assertThrows(ForbiddenException.class, () -> service.linkChildAccount(request(CHILD_EMAIL)));
        }

        @Test
        @DisplayName("UTCID04 (A) - childEmail rong -> 'Email tài khoản con là bắt buộc'")
        void utcid04_missingChildEmail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkChildAccount(request("  ")));
            assertEquals("Email tài khoản con là bắt buộc", ex.getMessage());

            assertThrows(IllegalArgumentException.class, () -> service.linkChildAccount(request(null)));
        }

        @Test
        @DisplayName("UTCID05 (A) - childEmail khong khop tai khoan nao -> 'Không tìm thấy tài khoản với email này'")
        void utcid05_accountNotFound() {
            when(userRepository.findByEmail("khongton@tcs.vn")).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.linkChildAccount(request("khongton@tcs.vn")));
            assertEquals("Không tìm thấy tài khoản với email này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - childEmail chinh la tai khoan cua nguoi goi -> 'Không thể liên kết chính tài khoản của bạn'")
        void utcid06_linkOwnAccount() {
            when(userRepository.findByEmail(PARENT_EMAIL)).thenReturn(Optional.of(parentUser));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkChildAccount(request(PARENT_EMAIL)));
            assertEquals("Không thể liên kết chính tài khoản của bạn", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (A) - Tai khoan tim thay khong co ho so Client -> 'Tài khoản này không phải khách hàng hợp lệ'")
        void utcid07_notAClientAccount() {
            when(clientRepository.findByUser_UserId(CHILD_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.linkChildAccount(request(CHILD_EMAIL)));
            assertEquals("Tài khoản này không phải khách hàng hợp lệ", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - Tai khoan con chua co ngay sinh -> 'Tài khoản con cần cập nhật ngày sinh trước khi liên kết'")
        void utcid08_childWithoutDateOfBirth() {
            childClient.setDateOfBirth(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkChildAccount(request(CHILD_EMAIL)));
            assertEquals("Tài khoản con cần cập nhật ngày sinh trước khi liên kết", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Tai khoan con chua co ho ten -> 'Tài khoản con cần cập nhật họ tên trước khi liên kết'")
        void utcid09_childWithoutFullName() {
            childClient.setFullName("  ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkChildAccount(request(CHILD_EMAIL)));
            assertEquals("Tài khoản con cần cập nhật họ tên trước khi liên kết", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (B) - Tai khoan con da du 18 tuoi -> chi lien ket duoc tai khoan vi thanh nien")
        void utcid10_childIsAdult() {
            childClient.setDateOfBirth(LocalDate.now().minusYears(18));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkChildAccount(request(CHILD_EMAIL)));
            assertEquals("Chỉ có thể liên kết tài khoản học sinh vị thành niên (dưới 18 tuổi)", ex.getMessage());
            verify(parentChildLinkRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID11 (A) - Con da lien ket voi chinh phu huynh nay -> 'Tài khoản con đã được liên kết'")
        void utcid11_alreadyLinkedToSameParent() {
            ChildProfile existingChild = childProfile(CHILD_PROFILE_ID,
                    childClient.getFullName(), childClient.getDateOfBirth());
            when(clientLegalAccountService.findGuardianLinkForMinor(childClient))
                    .thenReturn(Optional.of(link(parentUser, existingChild)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkChildAccount(request(CHILD_EMAIL)));
            assertEquals("Tài khoản con đã được liên kết", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID12 (A) - Con da lien ket voi phu huynh khac -> 'Học sinh đã liên kết với phụ huynh khác'")
        void utcid12_alreadyLinkedToAnotherParent() {
            User otherParent = user(999L, "phuhuynhkhac@tcs.vn");
            ChildProfile existingChild = childProfile(CHILD_PROFILE_ID,
                    childClient.getFullName(), childClient.getDateOfBirth());
            when(clientLegalAccountService.findGuardianLinkForMinor(childClient))
                    .thenReturn(Optional.of(link(otherParent, existingChild)));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkChildAccount(request(CHILD_EMAIL)));
            assertEquals("Học sinh đã liên kết với phụ huynh khác", ex.getMessage());
            verify(parentChildLinkRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: linkGuardian (hoc sinh vi thanh nien lien ket phu huynh)
    // ===================================================================
    @Nested
    @DisplayName("linkGuardian")
    class LinkGuardian {

        private Client minorClient;

        @BeforeEach
        void loginAsMinorStudent() {
            // Nguoi dang nhap la hoc sinh vi thanh nien.
            minorClient = client(parentUser, "Nguyen Van Con", LocalDate.now().minusYears(12));
            when(clientRepository.findByUser_UserId(PARENT_USER_ID)).thenReturn(Optional.of(minorClient));

            User adultParent = user(CHILD_USER_ID, CHILD_EMAIL);
            Client adultParentClient = client(adultParent, "Nguyen Van Bo", LocalDate.now().minusYears(40));
            when(userRepository.findByEmail(CHILD_EMAIL)).thenReturn(Optional.of(adultParent));
            when(clientRepository.findByUser_UserId(CHILD_USER_ID)).thenReturn(Optional.of(adultParentClient));
            when(parentChildLinkRepository
                    .findFirstByParentUser_UserIdAndChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
                            CHILD_USER_ID, minorClient.getFullName(), minorClient.getDateOfBirth(),
                            ParentChildLinkStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(childProfileRepository.findFirstByFullNameAndDateOfBirth(
                    minorClient.getFullName(), minorClient.getDateOfBirth()))
                    .thenReturn(Optional.of(childProfile(CHILD_PROFILE_ID,
                            minorClient.getFullName(), minorClient.getDateOfBirth())));
            when(parentChildLinkRepository.save(any(ParentChildLink.class)))
                    .thenAnswer(i -> i.getArgument(0));
        }

        private LinkGuardianRequest request(String parentEmail) {
            LinkGuardianRequest r = new LinkGuardianRequest();
            r.setParentEmail(parentEmail);
            return r;
        }

        @Test
        @DisplayName("UTCID01 (N) - Hoc sinh vi thanh nien du ho ten/ngay sinh, chua lien ket -> luu lien ket ACTIVE")
        void utcid01_linkSuccessfully() {
            service.linkGuardian(request(CHILD_EMAIL));

            ArgumentCaptor<ParentChildLink> captor = ArgumentCaptor.forClass(ParentChildLink.class);
            verify(parentChildLinkRepository).save(captor.capture());
            assertEquals(ParentChildLinkStatus.ACTIVE, captor.getValue().getStatus());
            assertEquals(CHILD_USER_ID, captor.getValue().getParentUser().getUserId());
        }

        @Test
        @DisplayName("UTCID02 (A) - Nguoi goi khong co vai tro CLIENT -> ForbiddenException")
        void utcid02_notAClient() {
            when(platformMapper.resolveRole(any())).thenReturn(UserRole.TUTOR);

            assertThrows(ForbiddenException.class, () -> service.linkGuardian(request(CHILD_EMAIL)));
            verify(parentChildLinkRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - Nguoi goi chua co ngay sinh -> 'Vui lòng cập nhật ngày sinh trước khi liên kết phụ huynh'")
        void utcid03_missingDateOfBirth() {
            minorClient.setDateOfBirth(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkGuardian(request(CHILD_EMAIL)));
            assertEquals("Vui lòng cập nhật ngày sinh trước khi liên kết phụ huynh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - Nguoi goi chua co ho ten -> 'Vui lòng cập nhật họ tên trước khi liên kết phụ huynh'")
        void utcid04_missingFullName() {
            minorClient.setFullName("  ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkGuardian(request(CHILD_EMAIL)));
            assertEquals("Vui lòng cập nhật họ tên trước khi liên kết phụ huynh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (B) - Nguoi goi da du 18 tuoi -> 'Chỉ tài khoản học sinh vị thành niên cần liên kết phụ huynh'")
        void utcid05_callerIsAdult() {
            minorClient.setDateOfBirth(LocalDate.now().minusYears(18));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.linkGuardian(request(CHILD_EMAIL)));
            assertEquals("Chỉ tài khoản học sinh vị thành niên cần liên kết phụ huynh", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - parentEmail rong -> 'Email phụ huynh là bắt buộc'")
        void utcid06_missingParentEmail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkGuardian(request("   ")));
            assertEquals("Email phụ huynh là bắt buộc", ex.getMessage());

            assertThrows(IllegalArgumentException.class, () -> service.linkGuardian(request(null)));
        }

        @Test
        @DisplayName("UTCID07 (A) - parentEmail khong khop tai khoan nao -> 'Không tìm thấy tài khoản phụ huynh với email này'")
        void utcid07_parentAccountNotFound() {
            when(userRepository.findByEmail("khongton@tcs.vn")).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.linkGuardian(request("khongton@tcs.vn")));
            assertEquals("Không tìm thấy tài khoản phụ huynh với email này", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID08 (A) - parentEmail chinh la tai khoan cua nguoi goi -> 'Không thể liên kết chính tài khoản của bạn'")
        void utcid08_linkOwnAccount() {
            when(userRepository.findByEmail(PARENT_EMAIL)).thenReturn(Optional.of(parentUser));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkGuardian(request(PARENT_EMAIL)));
            assertEquals("Không thể liên kết chính tài khoản của bạn", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID09 (A) - Tai khoan phu huynh khong co ho so Client -> 'Tài khoản phụ huynh không phải khách hàng hợp lệ'")
        void utcid09_parentIsNotAClient() {
            when(clientRepository.findByUser_UserId(CHILD_USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.linkGuardian(request(CHILD_EMAIL)));
            assertEquals("Tài khoản phụ huynh không phải khách hàng hợp lệ", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID10 (B) - Phu huynh thieu ngay sinh hoac chua du 18 -> 'Phụ huynh liên kết phải từ 18 tuổi trở lên'")
        void utcid10_parentNotAdult() {
            User youngParent = user(CHILD_USER_ID, CHILD_EMAIL);
            Client youngParentClient = client(youngParent, "Nguoi than", LocalDate.now().minusYears(17));
            when(clientRepository.findByUser_UserId(CHILD_USER_ID)).thenReturn(Optional.of(youngParentClient));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkGuardian(request(CHILD_EMAIL)));
            assertEquals("Phụ huynh liên kết phải từ 18 tuổi trở lên", ex.getMessage());

            youngParentClient.setDateOfBirth(null);
            assertThrows(IllegalArgumentException.class, () -> service.linkGuardian(request(CHILD_EMAIL)));
        }

        @Test
        @DisplayName("UTCID11 (A) - Da co lien kết ACTIVE toi phu huynh nay -> 'Phụ huynh này đã được liên kết'")
        void utcid11_alreadyLinked() {
            when(parentChildLinkRepository
                    .findFirstByParentUser_UserIdAndChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
                            CHILD_USER_ID, minorClient.getFullName(), minorClient.getDateOfBirth(),
                            ParentChildLinkStatus.ACTIVE))
                    .thenReturn(Optional.of(link(user(CHILD_USER_ID, CHILD_EMAIL),
                            childProfile(CHILD_PROFILE_ID, minorClient.getFullName(),
                                    minorClient.getDateOfBirth()))));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.linkGuardian(request(CHILD_EMAIL)));
            assertEquals("Phụ huynh này đã được liên kết", ex.getMessage());
            verify(parentChildLinkRepository, never()).save(any());
        }
    }

    // ===================================================================
    //  Sheet: pfSubmitVerification (ProfileService.submitVerification)
    // ===================================================================
    @Nested
    @DisplayName("pfSubmitVerification")
    class PfSubmitVerification {

        private void loginAs(UserRole role) {
            when(platformMapper.resolveRole(any())).thenReturn(role);
            if (role == UserRole.TUTOR) {
                when(tutorRepository.findByUser_UserId(PARENT_USER_ID)).thenReturn(Optional.of(new Tutor()));
            } else if (role == UserRole.TUTOR_CENTER) {
                when(tutorCenterRepository.findByUser_UserId(PARENT_USER_ID))
                        .thenReturn(Optional.of(new TutorCenter()));
            }
        }

        @Test
        @DisplayName("UTCID01 (N) - Vai tro TUTOR va verificationType null -> mac dinh TUTOR_PROFILE roi uy quyen")
        void utcid01_tutorDefaultsToTutorProfile() {
            loginAs(UserRole.TUTOR);
            VerificationRequestDto request = new VerificationRequestDto();

            service.submitVerification(request);

            assertEquals(VerificationType.TUTOR_PROFILE, request.getVerificationType());
            verify(verificationService).submitVerification(request);
        }

        @Test
        @DisplayName("UTCID02 (N) - Vai tro TUTOR_CENTER va verificationType null -> mac dinh TUTOR_CENTER_LICENSE")
        void utcid02_centerDefaultsToLicense() {
            loginAs(UserRole.TUTOR_CENTER);
            VerificationRequestDto request = new VerificationRequestDto();

            service.submitVerification(request);

            assertEquals(VerificationType.TUTOR_CENTER_LICENSE, request.getVerificationType());
            verify(verificationService).submitVerification(request);
        }

        @Test
        @DisplayName("UTCID03 (A) - Vai tro khac (vi du CLIENT) -> 'Chỉ gia sư hoặc trung tâm mới nộp xác minh'")
        void utcid03_otherRoleRejected() {
            loginAs(UserRole.CLIENT);

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> service.submitVerification(new VerificationRequestDto()));
            assertEquals("Chỉ gia sư hoặc trung tâm mới nộp xác minh", ex.getMessage());
            verify(verificationService, never()).submitVerification(any());
        }

        @Test
        @DisplayName("UTCID04 (N) - verificationType da duoc dat san -> giu nguyen, khong ghi de")
        void utcid04_explicitTypeIsPreserved() {
            loginAs(UserRole.TUTOR);
            VerificationRequestDto request = new VerificationRequestDto();
            request.setVerificationType(VerificationType.TUTOR_CENTER_LICENSE);

            service.submitVerification(request);

            assertEquals(VerificationType.TUTOR_CENTER_LICENSE, request.getVerificationType(),
                    "Loai xac minh nguoi dung chon phai duoc giu nguyen");
            verify(verificationService).submitVerification(request);
        }

        @Test
        @DisplayName("UTCID05 (A) - VerificationService tu choi -> loi duoc nem ra nguyen ven")
        void utcid05_delegatedFailurePropagates() {
            loginAs(UserRole.TUTOR);
            VerificationRequestDto request = new VerificationRequestDto();
            when(verificationService.submitVerification(request))
                    .thenThrow(new IllegalArgumentException("Hồ sơ xác minh thiếu tài liệu bắt buộc"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.submitVerification(request));
            assertEquals("Hồ sơ xác minh thiếu tài liệu bắt buộc", ex.getMessage());
        }
    }
}
