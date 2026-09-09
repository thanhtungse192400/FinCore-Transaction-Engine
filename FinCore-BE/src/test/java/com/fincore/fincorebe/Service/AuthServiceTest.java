package com.fincore.fincorebe.Service;

import com.fincore.fincorebe.Service.Impl.AuthServiceImpl;

import com.fincore.fincorebe.Dto.Request.LoginRequest;
import com.fincore.fincorebe.Dto.Request.RegisterRequest;
import com.fincore.fincorebe.Dto.Request.VerifyOtpRequest;
import com.fincore.fincorebe.Dto.Response.AuthResponse;
import com.fincore.fincorebe.Exception.BadRequestException;
import com.fincore.fincorebe.Exception.UnauthorizedException;
import com.fincore.fincorebe.Model.Entity.EmailVerificationOtp;
import com.fincore.fincorebe.Model.Entity.RefreshToken;
import com.fincore.fincorebe.Model.Entity.User;
import com.fincore.fincorebe.Model.Enum.OtpType;
import com.fincore.fincorebe.Model.Enum.Role;
import com.fincore.fincorebe.Model.Enum.TokenType;
import com.fincore.fincorebe.Model.Enum.UserStatus;
import com.fincore.fincorebe.Repository.EmailVerificationOtpRepository;
import com.fincore.fincorebe.Repository.RefreshTokenRepository;
import com.fincore.fincorebe.Repository.UserRepository;
import com.fincore.fincorebe.Security.JwtTokenProvider;
import com.fincore.fincorebe.Security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationOtpRepository emailVerificationOtpRepository;
    @Mock private WalletService walletService;
    @Mock private EmailService emailService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                emailVerificationOtpRepository,
                walletService,
                emailService,
                tokenBlacklistService,
                jwtTokenProvider,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Register: Should create pending user and send OTP")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("test@fincore.internal", "Password123", "John", "Doe", "0912345678");

        when(userRepository.findByEmail("test@fincore.internal")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(emailService.generate6DigitOtp()).thenReturn("123456");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = authService.register(request);

        assertNotNull(result);
        assertTrue(result.contains("Registration successful"));
        verify(emailService, times(1)).sendVerificationOtpEmail(eq("test@fincore.internal"), eq("John Doe"), eq("123456"), anyInt());
        verify(emailVerificationOtpRepository, times(1)).save(any(EmailVerificationOtp.class));
    }

    @Test
    @DisplayName("Verify OTP: Should activate user, create default wallet and return tokens")
    void testVerifyOtpSuccess() {
        UUID userId = UUID.randomUUID();
        User pendingUser = User.builder()
                .id(userId)
                .email("test@fincore.internal")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("0912345678")
                .role(Role.ROLE_USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        EmailVerificationOtp otpRecord = EmailVerificationOtp.builder()
                .user(pendingUser)
                .otpCode("654321")
                .otpType(OtpType.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plusSeconds(600))
                .used(false)
                .build();

        when(userRepository.findByEmail("test@fincore.internal")).thenReturn(Optional.of(pendingUser));
        when(emailVerificationOtpRepository.findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(pendingUser, OtpType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otpRecord));
        when(walletService.getWalletByUserId(userId)).thenReturn(Optional.empty());
        when(jwtTokenProvider.generateAccessToken(pendingUser)).thenReturn("mock.access.token");
        when(jwtTokenProvider.generateSecureRefreshTokenString()).thenReturn("mock-refresh-token-123");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        VerifyOtpRequest request = new VerifyOtpRequest("test@fincore.internal", "654321");
        AuthResponse response = authService.verifyOtp(request);

        assertNotNull(response);
        assertEquals("mock.access.token", response.getAccessToken());
        assertEquals("mock-refresh-token-123", response.getRefreshToken());
        assertEquals(UserStatus.ACTIVE, pendingUser.getStatus());
        assertTrue(otpRecord.isUsed());
        verify(walletService, times(1)).createDefaultWalletForUser(pendingUser);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Verify OTP: Should throw BadRequestException on wrong OTP")
    void testVerifyOtpWrongCode() {
        User pendingUser = User.builder()
                .email("test@fincore.internal")
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        EmailVerificationOtp otpRecord = EmailVerificationOtp.builder()
                .user(pendingUser)
                .otpCode("654321")
                .expiresAt(Instant.now().plusSeconds(600))
                .used(false)
                .build();

        when(userRepository.findByEmail("test@fincore.internal")).thenReturn(Optional.of(pendingUser));
        when(emailVerificationOtpRepository.findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(pendingUser, OtpType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otpRecord));

        VerifyOtpRequest request = new VerifyOtpRequest("test@fincore.internal", "000000");

        assertThrows(BadRequestException.class, () -> authService.verifyOtp(request));
    }

    @Test
    @DisplayName("Login: Should authenticate active user and return JWT tokens")
    void testLoginSuccess() {
        User activeUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@fincore.internal")
                .passwordHash("hashed_pass")
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.findByEmail("test@fincore.internal")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password123", "hashed_pass")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(activeUser)).thenReturn("mock.access.token");
        when(jwtTokenProvider.generateSecureRefreshTokenString()).thenReturn("mock-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        LoginRequest request = new LoginRequest("test@fincore.internal", "Password123");
        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.access.token", response.getAccessToken());
    }

    @Test
    @DisplayName("Login: Should throw UnauthorizedException when user is not verified")
    void testLoginUnverifiedUser() {
        User pendingUser = User.builder()
                .email("test@fincore.internal")
                .passwordHash("hashed_pass")
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        when(userRepository.findByEmail("test@fincore.internal")).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.matches("Password123", "hashed_pass")).thenReturn(true);

        LoginRequest request = new LoginRequest("test@fincore.internal", "Password123");

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Logout: Should blacklist access token and revoke refresh tokens")
    void testLogoutSuccess() {
        UUID userId = UUID.randomUUID();
        User activeUser = User.builder().id(userId).email("test@fincore.internal").build();
        UserPrincipal principal = UserPrincipal.create(activeUser);

        when(jwtTokenProvider.validateToken("valid.jwt")).thenReturn(true);
        when(jwtTokenProvider.getExpirationFromToken("valid.jwt")).thenReturn(Instant.now().plusSeconds(300));
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        authService.logout("Bearer valid.jwt", principal);

        verify(tokenBlacklistService, times(1)).blacklistToken(eq("valid.jwt"), eq(TokenType.ACCESS_TOKEN), any(Instant.class));
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(activeUser);
    }
}
