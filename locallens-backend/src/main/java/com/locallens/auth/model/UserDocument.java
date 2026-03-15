package com.locallens.auth.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "users")
public class UserDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String role; // TRAVELER, CREATOR, ADMIN
    private String authProvider; // LOCAL, GOOGLE, APPLE
    private String oauthProviderId;
    private String phone;
    private String currency;
    private String language;

    private UserPreferences preferences;
    private CreatorProfile creatorProfile;

    private String refreshTokenHash;
    private Instant lastLoginAt;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    @Data
    public static class UserPreferences {
        private List<String> interests;
        private String travelStyle;
        private List<String> accessibilityNeeds;
        private List<String> dietaryRestrictions;
        private boolean darkMode;
    }

    @Data
    public static class CreatorProfile {
        private String city;
        private List<String> expertise;
        private String bio;
        private boolean verified;
        private String idVerificationStatus; // PENDING, VERIFIED, REJECTED
        private String stripeAccountId;
        private long totalEarnings;
        private double rating;
        private int reviewCount;
        private int totalBookings;
        private GeoJsonPoint location;
    }
}
