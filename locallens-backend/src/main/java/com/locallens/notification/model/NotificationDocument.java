package com.locallens.notification.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "notifications")
public class NotificationDocument {
    @Id private String id;
    private String userId;
    private String type; // BOOKING_CONFIRMED, REPLAN_ALERT, REVIEW_RECEIVED, etc.
    private String title;
    private String body;
    private Map<String, String> data;
    private boolean read;
    private Instant emailSentAt;
    private Instant pushSentAt;
    @CreatedDate private Instant createdAt;
    private Instant expiresAt;
}
