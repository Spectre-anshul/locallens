package com.locallens.marketplace.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "reviews")
public class ReviewDocument {
    @Id private String id;
    private String bookingId;
    private String reviewerId;
    private String experienceId;
    private String creatorId;
    private int rating;
    private String title;
    private String body;
    private List<String> photos;
    private int helpfulCount;
    private ReviewResponse response;
    private boolean verified;
    @CreatedDate private Instant createdAt;

    @Data public static class ReviewResponse { private String body; private Instant respondedAt; }
}
