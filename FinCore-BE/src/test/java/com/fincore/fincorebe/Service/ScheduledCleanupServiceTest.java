package com.fincore.fincorebe.Service;

import com.fincore.fincorebe.Service.Impl.ScheduledCleanupServiceImpl;

import com.fincore.fincorebe.Repository.EmailVerificationOtpRepository;
import com.fincore.fincorebe.Repository.RefreshTokenRepository;
import com.fincore.fincorebe.Repository.TokenBlacklistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledCleanupServiceTest {

    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationOtpRepository emailVerificationOtpRepository;

    @InjectMocks
    private ScheduledCleanupServiceImpl scheduledCleanupService;

    @Test
    @DisplayName("Should invoke purge methods on all 3 repositories during 2:00 AM nightly cleanup")
    void testExecuteNightlyCleanup() {
        scheduledCleanupService.executeNightlyCleanup();

        verify(tokenBlacklistRepository, times(1)).deleteByExpiresAtBefore(any(Instant.class));
        verify(refreshTokenRepository, times(1)).deleteByExpiresAtBeforeOrRevokedTrue(any(Instant.class));
        verify(emailVerificationOtpRepository, times(1)).deleteByExpiresAtBeforeOrUsedTrue(any(Instant.class));
    }
}
