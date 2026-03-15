package com.locallens.marketplace.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "bookings")
public class BookingDocument {

    @Id private String id;
    private String travelerId;
    private String creatorId;
    private String experienceId;
    private String tripId;
    private LocalDate date;
    private TimeSlot timeSlot;
    private int groupSize;
    private long totalAmount;
    private long platformFee;
    private long creatorPayout;
    private String currency;
    private String status; // PENDING, CONFIRMED, DECLINED, CANCELLED, COMPLETED, REFUNDED
    private Payment payment;
    private String specialRequests;
    private boolean injectedByAI;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    @Data public static class TimeSlot { private String start; private String end; }
    @Data public static class Payment { private String stripePaymentIntentId; private String stripeTransferId; private String status; private Instant paidAt; private Instant refundedAt; }
}
