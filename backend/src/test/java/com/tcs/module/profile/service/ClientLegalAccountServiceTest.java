package com.tcs.module.profile.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.ParentChildLink;
import com.tcs.module.profile.enums.ParentChildLinkStatus;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.ParentChildLinkRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link ClientLegalAccountService#requirePaymentEligibility} — chot chan
 * hoc sinh vi thanh nien tu thanh toan khi chua lien ket phu huynh.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: sheet clRequirePayEligible.</p>
 */
@ExtendWith(MockitoExtension.class)
class ClientLegalAccountServiceTest {

    private static final String FULL_NAME = "Nguyen Van A";

    @Mock private AuthHelper authHelper;
    @Mock private ClientRepository clientRepository;
    @Mock private ParentChildLinkRepository parentChildLinkRepository;

    @InjectMocks private ClientLegalAccountService clientLegalAccountService;

    private Client client(LocalDate dateOfBirth, String fullName) {
        Client c = new Client();
        c.setDateOfBirth(dateOfBirth);
        c.setFullName(fullName);
        return c;
    }

    private LocalDate minorDob() {
        return LocalDate.now().minusYears(15);
    }

    private LocalDate adultDob() {
        return LocalDate.now().minusYears(25);
    }

    private void givenGuardianLink(LocalDate dob, Optional<ParentChildLink> link) {
        when(parentChildLinkRepository
                .findFirstByChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
                        FULL_NAME, dob, ParentChildLinkStatus.ACTIVE))
                .thenReturn(link);
    }

    @Nested
    @DisplayName("clRequirePayEligible")
    class ClRequirePayEligible {

        @Test
        @DisplayName("UTCID01 (N) - client tu 18 tuoi tro len -> tra ve ngay, bo qua moi kiem tra phu huynh")
        void utcid01_adultPassesImmediately() {
            assertDoesNotThrow(() -> clientLegalAccountService
                    .requirePaymentEligibility(client(adultDob(), null)));
        }

        @Test
        @DisplayName("UTCID02 (N) - vi thanh nien, co ho ten va da lien ket phu huynh ACTIVE -> hop le")
        void utcid02_minorWithGuardianLink() {
            LocalDate dob = minorDob();
            givenGuardianLink(dob, Optional.of(new ParentChildLink()));

            assertDoesNotThrow(() -> clientLegalAccountService
                    .requirePaymentEligibility(client(dob, FULL_NAME)));
        }

        @Test
        @DisplayName("UTCID03 (A) - client = null -> 'Vui lòng cập nhật ngày sinh và hoàn tất liên kết hồ sơ trước khi thanh toán'")
        void utcid03_nullClient() {
            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> clientLegalAccountService.requirePaymentEligibility(null));
            assertEquals("Vui lòng cập nhật ngày sinh và hoàn tất liên kết hồ sơ trước khi thanh toán",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (A) - chua co ngay sinh -> cung thong bao voi client = null")
        void utcid04_missingDateOfBirth() {
            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> clientLegalAccountService.requirePaymentEligibility(client(null, FULL_NAME)));
            assertEquals("Vui lòng cập nhật ngày sinh và hoàn tất liên kết hồ sơ trước khi thanh toán",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID05 (A) - vi thanh nien chua co ho ten -> 'Vui lòng cập nhật họ tên trước khi liên kết phụ huynh và thanh toán'")
        void utcid05_minorWithoutFullName() {
            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> clientLegalAccountService.requirePaymentEligibility(client(minorDob(), "  ")));
            assertEquals("Vui lòng cập nhật họ tên trước khi liên kết phụ huynh và thanh toán",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID06 (A) - vi thanh nien chua lien ket phu huynh -> chan thanh toan")
        void utcid06_minorWithoutGuardianLink() {
            LocalDate dob = minorDob();
            givenGuardianLink(dob, Optional.empty());

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> clientLegalAccountService.requirePaymentEligibility(client(dob, FULL_NAME)));
            assertEquals("Học sinh vị thành niên cần liên kết hồ sơ phụ huynh trước khi thanh toán hoặc tạo hợp đồng",
                    ex.getMessage());
        }

        @Test
        @DisplayName("UTCID07 (B) - tron 18 tuoi dung hom nay -> coi la nguoi lon, cho qua")
        void utcid07_exactlyEighteenToday() {
            assertDoesNotThrow(() -> clientLegalAccountService
                    .requirePaymentEligibility(client(LocalDate.now().minusYears(18), null)));
        }
    }
}
