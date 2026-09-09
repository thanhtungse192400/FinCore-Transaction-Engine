package com.fincore.fincorebe.Model.Entity;

import com.fincore.fincorebe.Model.Enum.TokenType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_blacklist", indexes = {
        @Index(name = "idx_token_blacklist_token", columnList = "token", unique = true),
        @Index(name = "idx_token_blacklist_expires_at", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenType tokenType;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant blacklistedAt;

    @PrePersist
    protected void onCreate() {
        this.blacklistedAt = Instant.now();
    }
}
