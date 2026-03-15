package com.locallens.marketplace.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "experiences")
public class ExperienceDocument {

    @Id private String id;
    private String creatorId;
    private String title;
    private String description;
    private String category;
    private Price price;
    private int duration;
    private int maxGroupSize;
    private int minGroupSize;
    private ExperienceLocation location;
    private List<Availability> availability;
    private List<Media> media;
    private List<String> tags;
    private boolean hyperlocal;
    private List<String> languages;
    private String accessibilityInfo;
    private String cancellationPolicy;
    private double rating;
    private int reviewCount;
    private int bookingCount;
    private String status;
    private double commissionRate;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    @Data public static class Price { private long amount; private String currency; private String pricingType; }
    @Data public static class ExperienceLocation { private String city; private String address; private String meetingPoint; private GeoJsonPoint coordinates; }
    @Data public static class Availability { private String type; private Integer dayOfWeek; private String startTime; private String endTime; private String specificDate; private int maxBookings; private int currentBookings; }
    @Data public static class Media { private String type; private String url; private String gridFsId; private String caption; private boolean primary; }
}
