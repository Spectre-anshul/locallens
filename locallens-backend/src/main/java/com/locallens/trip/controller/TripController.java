package com.locallens.trip.controller;

import com.locallens.auth.security.UserPrincipal;
import com.locallens.trip.dto.CreateTripRequest;
import com.locallens.trip.model.TripDocument;
import com.locallens.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@PreAuthorize("hasRole('TRAVELER')")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripDocument> createTrip(@Valid @RequestBody CreateTripRequest req,
                                                    @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tripService.createTrip(user.getId(), req));
    }

    @GetMapping
    public ResponseEntity<List<TripDocument>> getUserTrips(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(tripService.getUserTrips(user.getId()));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripDocument> getTripById(@PathVariable String tripId) {
        return ResponseEntity.ok(tripService.getTripById(tripId));
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(@PathVariable String tripId) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tripId}/documents")
    public ResponseEntity<String> uploadDocument(@PathVariable String tripId,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam("type") String docType) throws IOException {
        return ResponseEntity.ok(tripService.uploadDocument(tripId, file, docType));
    }
}
