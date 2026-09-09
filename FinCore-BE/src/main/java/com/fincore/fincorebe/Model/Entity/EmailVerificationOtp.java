package com.fincore.fincorebe.Model.Entity;

import com.fincore.fincorebe.Model.Enum.OtpType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_otps", indexes = {
        @Index(name = "idx_otps_user_id", columnList = "user_id"),
        @Index(name = "idx_otps_otp_code", columnList = "otpCode"),
        @Index(name = "idx_otps_expires_at", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OtpType otpType = OtpType.EMAIL_VERIFICATION;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.otpType == null) {
            this.otpType = OtpType.EMAIL_VERIFICATION;
        }
    }
}
