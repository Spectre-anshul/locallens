package com.locallens.itinerary.service;

import com.locallens.common.exception.ResourceNotFoundException;
import com.locallens.itinerary.model.ItineraryDayDocument;
import com.locallens.itinerary.repository.ItineraryDayRepository;
import com.locallens.marketplace.model.ExperienceDocument;
import com.locallens.marketplace.service.ExperienceMatchingService;
import com.locallens.event.service.EventAggregatorService;
import com.locallens.event.model.EventDocument;
import com.locallens.trip.model.TripDocument;
import com.locallens.trip.service.TripService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryAIService {

    private final ClaudeApiClient claudeApiClient;
    private final PromptBuilder promptBuilder;
    private final ItineraryDayRepository itineraryDayRepository;
    private final TripService tripService;
    private final ExperienceMatchingService matchingService;
    private final EventAggregatorService eventService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Async
    public void generateItinerary(String tripId) {
        try {
            TripDocument trip = tripService.getTripById(tripId);

            // Fetch matching experiences and events
            List<ExperienceDocument> experiences = matchingService.match(
                    trip.getDestination().getCity(), trip.getInterests(),
                    trip.getTravelStyle(), trip.getStartDate(), trip.getEndDate(), trip.getGroupSize());

            List<EventDocument> events = eventService.getEventsForItinerary(
                    trip.getDestination().getCity(), trip.getStartDate(), trip.getEndDate());

            // Build prompts
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildGenerationPrompt(trip, experiences, events);

            // Call Claude API
            String response = claudeApiClient.callClaude(systemPrompt, userPrompt, 8000);

            // Parse response into itinerary days
            List<ItineraryDayDocument> days = parseItineraryResponse(response, tripId, trip);

            // Save to MongoDB
            itineraryDayRepository.deleteByTripId(tripId);
            itineraryDayRepository.saveAll(days);

            // Update trip status
            trip.setStatus("PLANNED");
            trip.setItineraryVersion(1);

            // Push WebSocket event
            messagingTemplate.convertAndSend("/topic/itinerary/" + tripId + "/replan",
                    Map.of("type", "ITINERARY_READY", "tripId", tripId));

            log.info("Itinerary generated for trip {}", tripId);
        } catch (Exception e) {
            log.error("Failed to generate itinerary for trip {}", tripId, e);
            messagingTemplate.convertAndSend("/topic/itinerary/" + tripId + "/replan",
                    Map.of("type", "GENERATION_FAILED", "tripId", tripId, "error", e.getMessage()));
        }
    }

    public List<ItineraryDayDocument> getItineraryDays(String tripId) {
        return itineraryDayRepository.findByTripIdOrderByDayNumber(tripId);
    }

    public ItineraryDayDocument getItineraryDay(String tripId, int dayNumber) {
        return itineraryDayRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary day not found"));
    }

    private List<ItineraryDayDocument> parseItineraryResponse(String json, String tripId, TripDocument trip) {
        List<ItineraryDayDocument> days = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode daysNode = root.get("days");
            if (daysNode != null && daysNode.isArray()) {
                for (JsonNode dayNode : daysNode) {
                    ItineraryDayDocument day = new ItineraryDayDocument();
                    day.setTripId(tripId);
                    day.setDayNumber(dayNode.get("dayNumber").asInt());
                    day.setDate(trip.getStartDate().plusDays(day.getDayNumber() - 1));
                    day.setTheme(dayNode.path("theme").asText(""));
                    day.setVersion(1);

                    List<ItineraryDayDocument.Slot> slots = new ArrayList<>();
                    JsonNode slotsNode = dayNode.get("slots");
                    if (slotsNode != null && slotsNode.isArray()) {
                        for (JsonNode slotNode : slotsNode) {
                            ItineraryDayDocument.Slot slot = objectMapper.treeToValue(
                                    slotNode, ItineraryDayDocument.Slot.class);
                            slot.setStatus("SCHEDULED");
                            slots.add(slot);
                        }
                    }
                    day.setSlots(slots);
                    days.add(day);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse itinerary response", e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
        return days;
    }
}
