package com.locallens.itinerary.controller;

import com.locallens.auth.security.UserPrincipal;
import com.locallens.itinerary.model.ItineraryDayDocument;
import com.locallens.itinerary.service.ItineraryAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryAIService itineraryAIService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateItinerary(@RequestBody Map<String, String> req,
                                                                   @AuthenticationPrincipal UserPrincipal user) {
        String tripId = req.get("tripId");
        itineraryAIService.generateItinerary(tripId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("tripId", tripId, "status", "GENERATING", "estimatedSeconds", 15));
    }

    @GetMapping("/{tripId}/days")
    public ResponseEntity<List<ItineraryDayDocument>> getItineraryDays(@PathVariable String tripId) {
        return ResponseEntity.ok(itineraryAIService.getItineraryDays(tripId));
    }

    @GetMapping("/{tripId}/days/{dayNumber}")
    public ResponseEntity<ItineraryDayDocument> getItineraryDay(@PathVariable String tripId,
                                                                  @PathVariable int dayNumber) {
        return ResponseEntity.ok(itineraryAIService.getItineraryDay(tripId, dayNumber));
    }
}
