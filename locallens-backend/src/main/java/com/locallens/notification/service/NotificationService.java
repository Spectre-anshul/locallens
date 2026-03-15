package com.locallens.notification.service;

import com.locallens.notification.model.NotificationDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public void send(String userId, String type, String title, String body, Map<String, String> data) {
        // Persist notification
        NotificationDocument notification = new NotificationDocument();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setData(data);
        notification.setRead(false);
        notification.setExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS));
        mongoTemplate.save(notification);

        // Push via WebSocket
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications",
                Map.of("type", type, "title", title, "body", body, "data", data));

        log.info("Notification sent to user {}: {}", userId, type);
    }

    public void notifyBookingConfirmed(String userId, String bookingId, String experienceTitle) {
        send(userId, "BOOKING_CONFIRMED", "Booking Confirmed!",
                "Your booking for \"" + experienceTitle + "\" has been confirmed.",
                Map.of("bookingId", bookingId));
    }

    public void notifyReplanTriggered(String userId, String tripId, int dayNumber, String reason) {
        send(userId, "REPLAN_ALERT", "Itinerary Updated",
                "Day " + dayNumber + " of your trip has been updated: " + reason,
                Map.of("tripId", tripId, "dayNumber", String.valueOf(dayNumber)));
    }
}
