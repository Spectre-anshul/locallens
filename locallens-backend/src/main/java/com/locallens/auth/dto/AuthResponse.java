package com.locallens.auth.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserProfileResponse user
) {}
