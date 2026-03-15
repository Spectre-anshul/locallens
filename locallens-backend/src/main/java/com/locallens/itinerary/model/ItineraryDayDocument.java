package com.locallens.itinerary.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "itinerary_days")
public class ItineraryDayDocument {

    @Id
    private String id;
    private String tripId;
    private int dayNumber;
    private LocalDate date;
    private String theme;
    private List<Slot> slots = new ArrayList<>();
    private WeatherForecast weatherForecast;
    private int version;

    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    @Data
    public static class Slot {
        private String slotId;
        private String startTime;
        private String endTime;
        private Activity activity;
        private Transport transport;
        private String status; // SCHEDULED, IN_PROGRESS, COMPLETED, SKIPPED, REPLANNED
        private String replanNote;
    }

    @Data
    public static class Activity {
        private String type; // ATTRACTION, EXPERIENCE, MEAL, TRANSPORT, FREE_TIME, CHECK_IN, CHECK_OUT
        private String title;
        private String description;
        private ActivityLocation location;
        private int duration;
        private Cost cost;
        private String bookingUrl;
        private String experienceId;
        private boolean hyperlocal;
        private List<String> tags;
        private String imageUrl;
    }

    @Data
    public static class ActivityLocation {
        private String name;
        private String address;
        private GeoJsonPoint coordinates;
    }

    @Data
    public static class Cost {
        private long amount;
        private String currency;
    }

    @Data
    public static class Transport {
        private String mode; // WALK, METRO, BUS, TAXI, RENTAL, BIKE, FERRY
        private String from;
        private String to;
        private int duration;
        private long cost;
        private String notes;
    }

    @Data
    public static class WeatherForecast {
        private String condition;
        private double tempHighC;
        private double tempLowC;
        private int precipitation;
        private String icon;
    }
}
