package com.fincore.fincorebe.Service;

import com.fincore.fincorebe.Model.Entity.User;
import com.fincore.fincorebe.Model.Entity.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletService {

    Wallet createDefaultWalletForUser(User user);

    Optional<Wallet> getWalletByUserId(UUID userId);
}
