package com.locallens.auth.dto;

import com.locallens.auth.model.UserDocument;

public record UserProfileResponse(
    String id,
    String email,
    String firstName,
    String lastName,
    String role,
    String avatarUrl,
    UserDocument.UserPreferences preferences,
    UserDocument.CreatorProfile creatorProfile
) {
    public static UserProfileResponse from(UserDocument user) {
        return new UserProfileResponse(
            user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
            user.getRole(), user.getAvatarUrl(), user.getPreferences(), user.getCreatorProfile()
        );
    }
}
