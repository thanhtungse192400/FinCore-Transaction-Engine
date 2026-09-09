package com.fincore.fincorebe.Security;

import com.fincore.fincorebe.Model.Entity.User;
import com.fincore.fincorebe.Model.Enum.Role;
import com.fincore.fincorebe.Model.Enum.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long accessTokenExpirationMs = 900000; // 15 mins
    private final long refreshTokenExpirationMs = 604800000; // 7 days

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, accessTokenExpirationMs, refreshTokenExpirationMs);
    }

    @Test
    @DisplayName("Should generate valid JWT access token and extract claims correctly")
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("user@fincore.internal")
                .firstName("Van A")
                .lastName("Nguyen")
                .phoneNumber("0987654321")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("user@fincore.internal", jwtTokenProvider.getEmailFromToken(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("Should generate secure refresh token string")
    void testGenerateSecureRefreshTokenString() {
        String refreshToken1 = jwtTokenProvider.generateSecureRefreshTokenString();
        String refreshToken2 = jwtTokenProvider.generateSecureRefreshTokenString();

        assertNotNull(refreshToken1);
        assertNotNull(refreshToken2);
        assertNotEquals(refreshToken1, refreshToken2);
        assertTrue(refreshToken1.length() >= 32);
    }

    @Test
    @DisplayName("Should return false for invalid or tampered JWT token")
    void testValidateInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}
