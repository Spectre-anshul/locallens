package com.locallens.replan.service;

import com.locallens.replan.model.ReplanLogDocument;
import com.locallens.trip.model.TripDocument;
import com.locallens.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplanningService {

    private final TripService tripService;
    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 1800000) // every 30 minutes
    public void weatherCheckJob() {
        log.info("Running weather check for active trips...");
        List<TripDocument> activeTrips = tripService.getActiveTrips(LocalDate.now());
        for (TripDocument trip : activeTrips) {
            checkWeatherForTrip(trip);
        }
    }

    @Scheduled(fixedRate = 900000) // every 15 minutes
    public void trafficCheckJob() {
        log.info("Running traffic check for active trips...");
        // TODO: Poll TomTom Traffic API for upcoming activities
    }

    @Scheduled(fixedRate = 3600000) // every 60 minutes
    public void venueStatusCheckJob() {
        log.info("Running venue status check...");
        // TODO: Check Google Places for venue closures
    }

    private void checkWeatherForTrip(TripDocument trip) {
        // TODO: Call OpenWeatherMap, check for adverse conditions, trigger replan
    }

    public void triggerReplan(String tripId, int dayNumber, String trigger, String reason) {
        log.info("Replanning trip {} day {} due to {}: {}", tripId, dayNumber, trigger, reason);

        // TODO: Fetch current day, call Claude replan, compute diff, store log
        // Push WebSocket event
        messagingTemplate.convertAndSend("/topic/itinerary/" + tripId + "/replan",
                Map.of("type", "REPLAN_TRIGGERED", "tripId", tripId, "dayNumber", dayNumber,
                        "triggeredBy", trigger, "reasonDetail", reason));
    }
}
