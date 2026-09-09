package com.fincore.fincorebe.Dto.Response;

import com.fincore.fincorebe.Model.Enum.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletSummaryResponse {
    private UUID id;
    private String walletNumber;
    private BigDecimal balance;
    private String currency;
    private WalletStatus status;
}
