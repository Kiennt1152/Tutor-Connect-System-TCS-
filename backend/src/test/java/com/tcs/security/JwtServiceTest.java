package com.tcs.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcs.module.profile.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test cho {@link JwtService} — sinh và kiểm tra JWT.
 *
 * <p>Bám bộ test case trong Report_5.1_UnitTest: sheet jwtGenerateToken và sheet jwtParseClaims.</p>
 *
 * <p>Hai trường {@code secret} và {@code expirationMs} được nạp bằng {@code @Value} nên ở
 * unit test phải gán trực tiếp qua reflection. Khoá HMAC phải dài tối thiểu 32 byte.</p>
 */
class JwtServiceTest {

    private static final String SECRET = "tcs-unit-test-secret-key-32-bytes-minimum-length";
    private static final String OTHER_SECRET = "another-secret-key-for-foreign-token-32bytes!!";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private static final Long USER_ID = 42L;
    private static final String EMAIL = "gia.su@example.com";

    private JwtService jwtService;

    private JwtService newService(String secret, long expirationMs) {
        JwtService s = new JwtService();
        ReflectionTestUtils.setField(s, "secret", secret);
        ReflectionTestUtils.setField(s, "expirationMs", expirationMs);
        return s;
    }

    @BeforeEach
    void setUp() {
        jwtService = newService(SECRET, ONE_HOUR_MS);
    }

    // ===================================================================
    //  Sheet: jwtGenerateToken
    // ===================================================================
    @Nested
    @DisplayName("jwtGenerateToken")
    class JwtGenerateToken {

        @Test
        @DisplayName("UTCID01 (N) - tham số hợp lệ -> subject = userId, đủ claim email/role/tokenVersion")
        void utcid01_generateSuccessfully() {
            String token = jwtService.generateToken(USER_ID, EMAIL, UserRole.TUTOR, 7L);

            assertNotNull(token);
            Claims claims = jwtService.parseClaims(token);
            assertEquals(String.valueOf(USER_ID), claims.getSubject());
            assertEquals(EMAIL, claims.get("email", String.class));
            assertEquals(UserRole.TUTOR.name(), claims.get("role", String.class));
            assertEquals(7L, jwtService.extractTokenVersion(claims));

            // exp = iat + expirationMs
            long diff = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
            assertEquals(ONE_HOUR_MS, diff, "hạn dùng phải bằng iat + app.jwt.expiration-ms");
        }

        @Test
        @DisplayName("UTCID02 (B) - tokenVersion = 0 -> vẫn được ghi thành claim")
        void utcid02_tokenVersionZero() {
            String token = jwtService.generateToken(USER_ID, EMAIL, UserRole.CLIENT, 0L);

            Claims claims = jwtService.parseClaims(token);
            assertEquals(0L, jwtService.extractTokenVersion(claims));
        }

        @Test
        @DisplayName("UTCID03 (A) - role = null -> NullPointerException (hàm không kiểm tra đầu vào)")
        void utcid03_nullRole() {
            assertThrows(NullPointerException.class,
                    () -> jwtService.generateToken(USER_ID, EMAIL, null, 1L));
        }

        @Test
        @DisplayName("UTCID04 (B) - expirationMs rất nhỏ -> token hết hạn gần như tức thì")
        void utcid04_tinyExpiration() throws Exception {
            JwtService shortLived = newService(SECRET, 1L);

            String token = shortLived.generateToken(USER_ID, EMAIL, UserRole.TUTOR, 1L);
            Thread.sleep(50);

            assertThrows(ExpiredJwtException.class, () -> shortLived.parseClaims(token));
        }

        @Test
        @DisplayName("UTCID05 (N) - email = null -> claim email rỗng, token vẫn sinh được")
        void utcid05_nullEmail() {
            String token = jwtService.generateToken(USER_ID, null, UserRole.PLATFORM_ADMIN, 1L);

            Claims claims = jwtService.parseClaims(token);
            assertNull(claims.get("email", String.class));
            assertEquals(UserRole.PLATFORM_ADMIN.name(), claims.get("role", String.class));
        }
    }

    // ===================================================================
    //  Sheet: jwtParseClaims
    // ===================================================================
    @Nested
    @DisplayName("jwtParseClaims")
    class JwtParseClaims {

        @Test
        @DisplayName("UTCID01 (N) - token hợp lệ, còn hạn -> trả về Claims đầy đủ")
        void utcid01_parseValidToken() {
            String token = jwtService.generateToken(USER_ID, EMAIL, UserRole.TUTOR_CENTER, 3L);

            Claims claims = jwtService.parseClaims(token);

            assertEquals(String.valueOf(USER_ID), claims.getSubject());
            assertEquals(USER_ID, jwtService.extractUserId(token));
            assertEquals(UserRole.TUTOR_CENTER, jwtService.extractRole(token));
            assertTrue(claims.getExpiration().after(new Date()));
        }

        @Test
        @DisplayName("UTCID02 (A) - token đã hết hạn -> ExpiredJwtException")
        void utcid02_expiredToken() throws Exception {
            JwtService shortLived = newService(SECRET, 1L);
            String token = shortLived.generateToken(USER_ID, EMAIL, UserRole.TUTOR, 1L);
            Thread.sleep(50);

            assertThrows(ExpiredJwtException.class, () -> jwtService.parseClaims(token));
        }

        @Test
        @DisplayName("UTCID03 (A) - token ký bằng secret khác -> SignatureException")
        void utcid03_wrongSignature() {
            JwtService foreign = newService(OTHER_SECRET, ONE_HOUR_MS);
            String token = foreign.generateToken(USER_ID, EMAIL, UserRole.TUTOR, 1L);

            assertThrows(SignatureException.class, () -> jwtService.parseClaims(token));
        }

        @Test
        @DisplayName("UTCID04 (A) - chuỗi không đúng định dạng JWT -> MalformedJwtException")
        void utcid04_malformedToken() {
            assertThrows(MalformedJwtException.class, () -> jwtService.parseClaims("abc.def.ghi"));
        }

        @Test
        @DisplayName("UTCID05 (A) - token null hoặc rỗng -> IllegalArgumentException")
        void utcid05_nullOrEmptyToken() {
            assertThrows(IllegalArgumentException.class, () -> jwtService.parseClaims(null));
            assertThrows(IllegalArgumentException.class, () -> jwtService.parseClaims(""));
        }
    }
}
