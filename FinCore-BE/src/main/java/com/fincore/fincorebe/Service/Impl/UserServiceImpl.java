package com.fincore.fincorebe.Service.Impl;

import com.fincore.fincorebe.Dto.Response.UserProfileResponse;
import com.fincore.fincorebe.Dto.Response.WalletSummaryResponse;
import com.fincore.fincorebe.Exception.ResourceNotFoundException;
import com.fincore.fincorebe.Model.Entity.User;
import com.fincore.fincorebe.Model.Entity.Wallet;
import com.fincore.fincorebe.Repository.UserRepository;
import com.fincore.fincorebe.Service.UserService;
import com.fincore.fincorebe.Service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WalletService walletService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Wallet wallet = walletService.getWalletByUserId(userId).orElse(null);

        WalletSummaryResponse walletSummary = null;
        if (wallet != null) {
            walletSummary = WalletSummaryResponse.builder()
                    .id(wallet.getId())
                    .walletNumber(wallet.getWalletNumber())
                    .balance(wallet.getBalance())
                    .currency(wallet.getCurrency())
                    .status(wallet.getStatus())
                    .build();
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .isPinSet(user.getPinHash() != null && !user.getPinHash().isBlank())
                .defaultWallet(walletSummary)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
