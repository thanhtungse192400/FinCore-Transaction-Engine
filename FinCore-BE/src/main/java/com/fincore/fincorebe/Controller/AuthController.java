package com.fincore.fincorebe.Controller;

import com.fincore.fincorebe.Dto.Request.*;
import com.fincore.fincorebe.Dto.Response.ApiResponse;
import com.fincore.fincorebe.Dto.Response.AuthResponse;
import com.fincore.fincorebe.Dto.Response.UserProfileResponse;
import com.fincore.fincorebe.Security.UserPrincipal;
import com.fincore.fincorebe.Service.AuthService;
import com.fincore.fincorebe.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Authorization", description = "Endpoints for Registration, OTP Verification, Login, Token Refresh, Profile, and PIN Management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register new account and send 6-digit OTP to email")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, message));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify 6-digit OTP to activate account and receive JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Account verified and activated successfully"));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend a new 6-digit verification OTP to email")
    public ResponseEntity<ApiResponse<String>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        String message = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password to get Access and Refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Generate a new Access Token using a valid Refresh Token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Logout, revoke refresh tokens and blacklist the active access token")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(authHeader, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get current authenticated user profile and default wallet information")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse profile = userService.getCurrentUserProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(profile, "User profile retrieved successfully"));
    }

    @PostMapping("/set-pin")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Set or update 6-digit 2FA Transaction PIN")
    public ResponseEntity<ApiResponse<String>> setTransactionPin(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SetPinRequest request) {
        authService.setTransactionPin(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction PIN set successfully"));
    }
}
