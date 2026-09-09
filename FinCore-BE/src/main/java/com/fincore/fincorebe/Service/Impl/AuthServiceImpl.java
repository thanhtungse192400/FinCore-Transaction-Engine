package com.fincore.fincorebe.Service.Impl;

import com.fincore.fincorebe.Dto.Request.*;
import com.fincore.fincorebe.Dto.Response.AuthResponse;
import com.fincore.fincorebe.Dto.Response.UserSummaryResponse;
import com.fincore.fincorebe.Exception.BadRequestException;
import com.fincore.fincorebe.Exception.ResourceAlreadyExistsException;
import com.fincore.fincorebe.Exception.ResourceNotFoundException;
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
import com.fincore.fincorebe.Service.AuthService;
import com.fincore.fincorebe.Service.EmailService;
import com.fincore.fincorebe.Service.TokenBlacklistService;
import com.fincore.fincorebe.Service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final WalletService walletService;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.security.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        var existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getStatus() == UserStatus.ACTIVE) {
                throw new ResourceAlreadyExistsException("An account with email " + email + " already exists");
            }
            existingUser.setFirstName(request.getFirstName().trim());
            existingUser.setLastName(request.getLastName().trim());
            existingUser.setPhoneNumber(request.getPhoneNumber().trim());
            existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            userRepository.save(existingUser);
            generateAndSendOtp(existingUser);
            return "A new 6-digit verification code has been sent to " + email;
        }

        User newUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .role(Role.ROLE_USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .build();

        User savedUser = userRepository.save(newUser);
        generateAndSendOtp(savedUser);

        log.info("User registered successfully: {}", email);
        return "Registration successful. Please verify your email with the 6-digit code sent to " + email;
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + email));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("Account is already verified and active. Please log in.");
        }

        EmailVerificationOtp otpRecord = emailVerificationOtpRepository
                .findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new BadRequestException("No active OTP found. Please request a new verification code."));

        if (otpRecord.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Verification OTP has expired. Please request a new code.");
        }

        if (!otpRecord.getOtpCode().equals(request.getOtpCode().trim())) {
            throw new BadRequestException("Invalid OTP code. Please try again.");
        }

        // Mark OTP as used
        otpRecord.setUsed(true);
        emailVerificationOtpRepository.save(otpRecord);

        // Activate User
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Create Default Wallet for the user if not exists
        if (walletService.getWalletByUserId(user.getId()).isEmpty()) {
            walletService.createDefaultWalletForUser(user);
        }

        log.info("User activated successfully via OTP: {}", email);
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public String resendOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("Account is already verified and active.");
        }

        generateAndSendOtp(user);
        return "A new 6-digit verification code has been sent to " + email;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new UnauthorizedException("Account is not verified yet. Please check your email for the verification code.");
        }

        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.DISABLED) {
            throw new UnauthorizedException("Account is locked or disabled. Please contact support.");
        }

        log.info("User logged in successfully: {}", email);
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenStr = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid or non-existent refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked. Please log in again.");
        }

        if (tokenBlacklistService.isTokenBlacklisted(tokenStr)) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        User user = refreshToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is inactive or locked");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000;

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(buildUserSummary(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String authHeader, UserPrincipal principal) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(jwt)) {
                Instant expiration = jwtTokenProvider.getExpirationFromToken(jwt);
                tokenBlacklistService.blacklistToken(jwt, TokenType.ACCESS_TOKEN, expiration);
            }
        }

        if (principal != null) {
            User user = userRepository.findById(principal.getId()).orElse(null);
            if (user != null) {
                refreshTokenRepository.revokeAllUserTokens(user);
            }
        }
        log.info("User logged out successfully: {}", principal != null ? principal.getEmail() : "unknown");
    }

    @Override
    @Transactional
    public void setTransactionPin(UUID userId, SetPinRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPinHash() != null && !user.getPinHash().isBlank()) {
            if (request.getOldPin() == null || !passwordEncoder.matches(request.getOldPin(), user.getPinHash())) {
                throw new BadRequestException("Current transaction PIN is incorrect");
            }
        }

        user.setPinHash(passwordEncoder.encode(request.getPin()));
        userRepository.save(user);
        log.info("Transaction PIN updated for user: {}", user.getEmail());
    }

    private void generateAndSendOtp(User user) {
        String otpCode = emailService.generate6DigitOtp();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpExpirationMinutes));

        EmailVerificationOtp otpRecord = EmailVerificationOtp.builder()
                .user(user)
                .otpCode(otpCode)
                .otpType(OtpType.EMAIL_VERIFICATION)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        emailVerificationOtpRepository.save(otpRecord);
        emailService.sendVerificationOtpEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName(), otpCode, otpExpirationMinutes);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenStr = jwtTokenProvider.generateSecureRefreshTokenString();

        Instant refreshTokenExpiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(refreshTokenExpiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .user(buildUserSummary(user))
                .build();
    }

    private UserSummaryResponse buildUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .isPinSet(user.getPinHash() != null && !user.getPinHash().isBlank())
                .build();
    }
}
