package com.fincore.fincorebe.Service.Impl;

import com.fincore.fincorebe.Repository.EmailVerificationOtpRepository;
import com.fincore.fincorebe.Repository.RefreshTokenRepository;
import com.fincore.fincorebe.Repository.TokenBlacklistRepository;
import com.fincore.fincorebe.Service.ScheduledCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledCleanupServiceImpl implements ScheduledCleanupService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationOtpRepository emailVerificationOtpRepository;

    /**
     * Nightly cleanup job running every day at 2:00 AM (0 0 2 * * *)
     * Automatically purges expired blacklist tokens, revoked/expired refresh tokens, and stale OTPs.
     */
    @Override
    @Scheduled(cron = "${application.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void executeNightlyCleanup() {
        log.info("Starting scheduled nightly database cleanup at {}", Instant.now());
        Instant now = Instant.now();

        int blacklistsDeleted = tokenBlacklistRepository.deleteByExpiresAtBefore(now);
        log.info("Purged {} expired tokens from blacklist", blacklistsDeleted);

        int refreshTokensDeleted = refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedTrue(now);
        log.info("Purged {} expired/revoked refresh tokens from DB", refreshTokensDeleted);

        int otpsDeleted = emailVerificationOtpRepository.deleteByExpiresAtBeforeOrUsedTrue(now);
        log.info("Purged {} stale/used email verification OTPs from DB", otpsDeleted);

        log.info("Scheduled nightly database cleanup completed successfully");
    }
}
