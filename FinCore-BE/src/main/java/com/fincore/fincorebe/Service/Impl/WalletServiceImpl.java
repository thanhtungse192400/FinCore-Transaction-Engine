package com.fincore.fincorebe.Service.Impl;

import com.fincore.fincorebe.Model.Entity.User;
import com.fincore.fincorebe.Model.Entity.Wallet;
import com.fincore.fincorebe.Model.Enum.WalletStatus;
import com.fincore.fincorebe.Repository.WalletRepository;
import com.fincore.fincorebe.Service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public Wallet createDefaultWalletForUser(User user) {
        String walletNumber = generateUniqueWalletNumber();

        Wallet wallet = Wallet.builder()
                .user(user)
                .walletNumber(walletNumber)
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .status(WalletStatus.ACTIVE)
                .version(0L)
                .build();

        Wallet saved = walletRepository.save(wallet);
        log.info("Created default wallet {} for user {}", walletNumber, user.getEmail());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Wallet> getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId);
    }

    private String generateUniqueWalletNumber() {
        String number;
        int attempts = 0;
        do {
            long random8Digits = 10_000_000L + secureRandom.nextInt(90_000_000);
            number = "10" + random8Digits;
            attempts++;
            if (attempts > 50) {
                throw new IllegalStateException("Unable to generate unique wallet number after multiple attempts");
            }
        } while (walletRepository.existsByWalletNumber(number));

        return number;
    }
}
