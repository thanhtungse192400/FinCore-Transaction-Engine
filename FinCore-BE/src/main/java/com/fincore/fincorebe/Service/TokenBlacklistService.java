package com.fincore.fincorebe.Service;

import com.fincore.fincorebe.Model.Enum.TokenType;

import java.time.Instant;

public interface TokenBlacklistService {

    void blacklistToken(String token, TokenType tokenType, Instant expiresAt);

    boolean isTokenBlacklisted(String token);

    int cleanupExpiredTokens();
}
