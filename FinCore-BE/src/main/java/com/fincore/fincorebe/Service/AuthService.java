package com.fincore.fincorebe.Service;

import com.fincore.fincorebe.Dto.Request.*;
import com.fincore.fincorebe.Dto.Response.AuthResponse;
import com.fincore.fincorebe.Security.UserPrincipal;

import java.util.UUID;

public interface AuthService {

    String register(RegisterRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);

    String resendOtp(ResendOtpRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String authHeader, UserPrincipal principal);

    void setTransactionPin(UUID userId, SetPinRequest request);
}
