package com.fincore.fincorebe.Dto.Response;

import com.fincore.fincorebe.Model.Enum.Role;
import com.fincore.fincorebe.Model.Enum.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
    private boolean isPinSet;
}
