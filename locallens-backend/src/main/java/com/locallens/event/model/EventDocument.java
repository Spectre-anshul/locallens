package com.locallens.event.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "events")
public class EventDocument {
    @Id private String id;
    private String source; // CREATOR, EVENTBRITE, FACEBOOK, INSTAGRAM
    private String externalId;
    private String title;
    private String description;
    private String category;
    private Instant startTime;
    private Instant endTime;
    private EventLocation location;
    private EventPrice price;
    private Integer capacity;
    private Integer ticketsRemaining;
    private List<String> tags;
    private String externalUrl;
    private String imageUrl;
    private String creatorId;
    private boolean familyFriendly;
    private Integer ageRestriction;
    private Instant lastSyncedAt;
    private String status; // UPCOMING, ONGOING, PAST, CANCELLED
    @CreatedDate private Instant createdAt;

    @Data public static class EventLocation { private String city; private String venueName; private String address; private GeoJsonPoint coordinates; }
    @Data public static class EventPrice { private long amount; private String currency; }
}
