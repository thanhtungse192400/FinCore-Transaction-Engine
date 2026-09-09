package com.fincore.fincorebe.Repository;

import com.fincore.fincorebe.Model.Entity.EmailVerificationOtp;
import com.fincore.fincorebe.Model.Entity.User;
import com.fincore.fincorebe.Model.Enum.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, UUID> {
    Optional<EmailVerificationOtp> findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(User user, OtpType otpType);

    @Modifying
    @Query("DELETE FROM EmailVerificationOtp o WHERE o.expiresAt < :now OR o.used = true")
    int deleteByExpiresAtBeforeOrUsedTrue(@Param("now") Instant now);
}
