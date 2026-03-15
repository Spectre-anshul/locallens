package com.locallens.trip.model;

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
@Document(collection = "trips")
public class TripDocument {

    @Id
    private String id;
    private String userId;
    private String title;
    private Destination destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private int durationDays;
    private int groupSize;
    private String travelStyle;
    private List<String> interests = new ArrayList<>();
    private List<String> accessibilityNeeds = new ArrayList<>();
    private Budget budget;
    private List<Flight> flights = new ArrayList<>();
    private List<Accommodation> accommodations = new ArrayList<>();
    private List<TripDocument.TripDoc> documents = new ArrayList<>();
    private String status; // DRAFT, PLANNED, ACTIVE, COMPLETED, CANCELLED
    private int itineraryVersion;

    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    @Data
    public static class Destination {
        private String city;
        private String country;
        private GeoJsonPoint location;
    }

    @Data
    public static class Budget {
        private long total;
        private long spent;
        private String currency;
        private BudgetBreakdown breakdown;
    }

    @Data
    public static class BudgetBreakdown {
        private long accommodation;
        private long food;
        private long transport;
        private long activities;
        private long shopping;
        private long miscellaneous;
    }

    @Data
    public static class Flight {
        private String type; // OUTBOUND, RETURN, LAYOVER
        private String airline;
        private String flightNumber;
        private FlightEndpoint departure;
        private FlightEndpoint arrival;
        private String bookingRef;
        private String seatNumber;
    }

    @Data
    public static class FlightEndpoint {
        private String airport;
        private String terminal;
        private String gate;
        private Instant time;
    }

    @Data
    public static class Accommodation {
        private String name;
        private String type; // HOTEL, HOSTEL, AIRBNB, RESORT
        private String address;
        private GeoJsonPoint location;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private String bookingRef;
        private long pricePerNight;
        private long totalPrice;
        private String contactPhone;
    }

    @Data
    public static class TripDoc {
        private String type; // PASSPORT, VISA, INSURANCE, TICKET, OTHER
        private String fileName;
        private String gridFsFileId;
        private Instant uploadedAt;
    }
}
