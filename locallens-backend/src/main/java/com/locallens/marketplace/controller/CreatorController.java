package com.locallens.marketplace.controller;

import com.locallens.auth.security.UserPrincipal;
import com.locallens.marketplace.model.ExperienceDocument;
import com.locallens.marketplace.repository.BookingRepository;
import com.locallens.marketplace.repository.ExperienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creator")
@PreAuthorize("hasRole('CREATOR')")
@RequiredArgsConstructor
public class CreatorController {

    private final ExperienceRepository experienceRepository;
    private final BookingRepository bookingRepository;

    @PostMapping("/experiences")
    public ResponseEntity<ExperienceDocument> createExperience(@RequestBody ExperienceDocument experience,
                                                                 @AuthenticationPrincipal UserPrincipal user) {
        experience.setCreatorId(user.getId());
        experience.setStatus("ACTIVE");
        experience.setRating(0);
        experience.setReviewCount(0);
        experience.setBookingCount(0);
        return ResponseEntity.status(HttpStatus.CREATED).body(experienceRepository.save(experience));
    }

    @GetMapping("/experiences")
    public ResponseEntity<List<ExperienceDocument>> getMyExperiences(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(experienceRepository.findByCreatorId(user.getId()));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal UserPrincipal user) {
        var bookings = bookingRepository.findByCreatorIdAndStatus(user.getId(), "CONFIRMED");
        var experiences = experienceRepository.findByCreatorId(user.getId());
        return ResponseEntity.ok(Map.of(
                "upcomingBookings", bookings.size(),
                "activeExperiences", experiences.size()
        ));
    }
}
