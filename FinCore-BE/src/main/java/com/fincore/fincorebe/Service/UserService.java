package com.fincore.fincorebe.Service;

import com.fincore.fincorebe.Dto.Response.UserProfileResponse;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getCurrentUserProfile(UUID userId);
}
