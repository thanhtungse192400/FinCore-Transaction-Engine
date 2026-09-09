package com.fincore.fincorebe.Service.Impl;

import com.fincore.fincorebe.Model.Entity.TokenBlacklist;
import com.fincore.fincorebe.Model.Enum.TokenType;
import com.fincore.fincorebe.Repository.TokenBlacklistRepository;
import com.fincore.fincorebe.Service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    @Transactional
    public void blacklistToken(String token, TokenType tokenType, Instant expiresAt) {
        if (token == null || token.isBlank()) {
            return;
        }

        if (tokenBlacklistRepository.existsByToken(token)) {
            return;
        }

        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                .token(token)
                .tokenType(tokenType)
                .expiresAt(expiresAt != null ? expiresAt : Instant.now().plusSeconds(86400))
                .build();

        tokenBlacklistRepository.save(blacklistedToken);
        log.info("Token blacklisted successfully (Type: {})", tokenType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return tokenBlacklistRepository.existsByToken(token);
    }

    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        Instant now = Instant.now();
        int deletedCount = tokenBlacklistRepository.deleteByExpiresAtBefore(now);
        log.info("Cleaned up {} expired blacklisted tokens from database", deletedCount);
        return deletedCount;
    }
}
