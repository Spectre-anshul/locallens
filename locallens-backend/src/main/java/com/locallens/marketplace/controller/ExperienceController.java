package com.locallens.marketplace.controller;

import com.locallens.auth.security.UserPrincipal;
import com.locallens.marketplace.model.ExperienceDocument;
import com.locallens.marketplace.repository.ExperienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/experiences")
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceRepository experienceRepository;

    @GetMapping
    public ResponseEntity<Page<ExperienceDocument>> searchExperiences(
            @RequestParam String city,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        Page<ExperienceDocument> results;
        if (category != null) {
            results = experienceRepository.findByLocationCityAndStatusAndCategory(city, "ACTIVE", category, pageable);
        } else {
            results = experienceRepository.findByLocationCityAndStatus(city, "ACTIVE", pageable);
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExperienceDocument> getExperience(@PathVariable String id) {
        return experienceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
