package com.fincore.fincorebe.Repository;

import com.fincore.fincorebe.Model.Entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByUserId(UUID userId);
    Optional<Wallet> findByWalletNumber(String walletNumber);
    boolean existsByWalletNumber(String walletNumber);
}
