package com.tcs.module.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.module.platform.entity.UserPenalty;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.repository.UserPenaltyRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link PenaltyAccessServiceImpl#requireFeature} — cong chan nguoi dung
 * dang bi han che tinh nang.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: sheet penaltyRequireFeature.</p>
 */
@ExtendWith(MockitoExtension.class)
class PenaltyAccessServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String FEATURE = "WITHDRAWAL";

    @Mock private UserPenaltyRepository repository;

    @InjectMocks private PenaltyAccessServiceImpl penaltyAccessService;

    private UserPenalty penalty(UserPenaltyType type, String details, LocalDateTime expiresAt) {
        UserPenalty p = new UserPenalty();
        p.setPenaltyType(type);
        p.setRestrictionDetails(details);
        p.setExpiresAt(expiresAt);
        p.setStatus(UserPenaltyStatus.ACTIVE);
        return p;
    }

    private void givenActivePenalties(UserPenalty... items) {
        when(repository.findByUser_UserIdAndStatus(USER_ID, UserPenaltyStatus.ACTIVE))
                .thenReturn(List.of(items));
    }

    @Nested
    @DisplayName("penaltyRequireFeature")
    class PenaltyRequireFeature {

        @Test
        @DisplayName("UTCID01 (N) - nguoi dung khong co an phat ACTIVE -> khong chan")
        void utcid01_noActivePenalty() {
            givenActivePenalties();

            assertDoesNotThrow(() -> penaltyAccessService.requireFeature(USER_ID, FEATURE));
        }

        @Test
        @DisplayName("UTCID02 (A) - co han che tinh nang dung ma -> ForbiddenException")
        void utcid02_featureRestricted() {
            givenActivePenalties(penalty(UserPenaltyType.FEATURE_RESTRICTION,
                    "{\"features\":[\"WITHDRAWAL\"]}", LocalDateTime.now().plusDays(7)));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> penaltyAccessService.requireFeature(USER_ID, FEATURE));
            assertEquals("Tính năng WITHDRAWAL đang bị hạn chế trên tài khoản này.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID03 (B) - featureCode viet thuong -> duoc chuan hoa thanh hoa roi so khop")
        void utcid03_lowerCaseFeatureCode() {
            givenActivePenalties(penalty(UserPenaltyType.FEATURE_RESTRICTION,
                    "{\"features\":[\"WITHDRAWAL\"]}", null));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> penaltyAccessService.requireFeature(USER_ID, "withdrawal"));
            assertEquals("Tính năng WITHDRAWAL đang bị hạn chế trên tài khoản này.", ex.getMessage());
        }

        @Test
        @DisplayName("UTCID04 (B) - an phat da het han -> bi loc bo, khong chan")
        void utcid04_expiredPenaltyIgnored() {
            givenActivePenalties(penalty(UserPenaltyType.FEATURE_RESTRICTION,
                    "{\"features\":[\"WITHDRAWAL\"]}", LocalDateTime.now().minusDays(1)));

            assertDoesNotThrow(() -> penaltyAccessService.requireFeature(USER_ID, FEATURE));
        }

        @Test
        @DisplayName("UTCID05 (B) - expiresAt = null (vinh vien) -> van chan")
        void utcid05_permanentPenaltyBlocks() {
            givenActivePenalties(penalty(UserPenaltyType.FEATURE_RESTRICTION,
                    "{\"features\":[\"WITHDRAWAL\"]}", null));

            assertThrows(ForbiddenException.class,
                    () -> penaltyAccessService.requireFeature(USER_ID, FEATURE));
        }

        @Test
        @DisplayName("UTCID06 (N) - an phat loai khac (TEMPORARY_BAN) -> khong chan tinh nang")
        void utcid06_otherPenaltyTypeIgnored() {
            givenActivePenalties(penalty(UserPenaltyType.TEMPORARY_BAN,
                    "{\"features\":[\"WITHDRAWAL\"]}", null));

            assertDoesNotThrow(() -> penaltyAccessService.requireFeature(USER_ID, FEATURE));
        }

        @Test
        @DisplayName("UTCID07 (N) - restrictionDetails = null -> coi nhu rong, khong chan")
        void utcid07_nullRestrictionDetails() {
            givenActivePenalties(penalty(UserPenaltyType.FEATURE_RESTRICTION, null, null));

            assertDoesNotThrow(() -> penaltyAccessService.requireFeature(USER_ID, FEATURE));
        }

        @Test
        @DisplayName("UTCID08 (N) - restrictionDetails chua ma khac -> khong chan")
        void utcid08_differentFeatureCode() {
            givenActivePenalties(penalty(UserPenaltyType.FEATURE_RESTRICTION,
                    "{\"features\":[\"MESSAGING\"]}", null));

            assertDoesNotThrow(() -> penaltyAccessService.requireFeature(USER_ID, FEATURE));
        }
    }
}
