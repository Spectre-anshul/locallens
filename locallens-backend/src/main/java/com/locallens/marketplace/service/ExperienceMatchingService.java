package com.locallens.marketplace.service;

import com.locallens.marketplace.model.ExperienceDocument;
import com.locallens.marketplace.repository.ExperienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExperienceMatchingService {

    private final ExperienceRepository experienceRepository;

    public List<ExperienceDocument> match(String city, List<String> interests, String travelStyle,
                                           LocalDate startDate, LocalDate endDate, int groupSize) {
        List<ExperienceDocument> experiences = experienceRepository
                .findByLocationCityAndStatusAndHyperlocalTrue(city, "ACTIVE");

        return experiences.stream()
                .map(exp -> new ScoredExperience(exp, computeScore(exp, interests, travelStyle, groupSize)))
                .sorted(Comparator.comparingDouble(ScoredExperience::score).reversed())
                .limit(15)
                .map(ScoredExperience::experience)
                .collect(Collectors.toList());
    }

    private double computeScore(ExperienceDocument exp, List<String> interests, String travelStyle, int groupSize) {
        double score = 0.0;

        // Interest tag overlap (weight 0.25)
        long overlap = exp.getTags().stream().filter(interests::contains).count();
        score += 0.25 * ((double) overlap / Math.max(interests.size(), 1));

        // Rating × log(bookingCount) (weight 0.15)
        score += 0.15 * (exp.getRating() / 5.0) * Math.log1p(exp.getBookingCount()) / 5.0;

        // Group size fit
        if (groupSize <= exp.getMaxGroupSize() && groupSize >= exp.getMinGroupSize()) {
            score += 0.15;
        }

        // Hyperlocal bonus (weight 0.10)
        if (exp.isHyperlocal()) {
            score += 0.10;
        }

        // Style compatibility (weight 0.15)
        score += 0.15 * styleScore(travelStyle, exp.getCategory(), exp.getPrice().getAmount());

        return score;
    }

    private double styleScore(String style, String category, long price) {
        return switch (style) {
            case "LUXURY" -> price > 5000 ? 1.0 : 0.3;
            case "BACKPACKER" -> price < 2000 ? 1.0 : 0.3;
            case "FAMILY" -> "CULTURE".equals(category) || "NATURE".equals(category) ? 0.8 : 0.4;
            default -> 0.5;
        };
    }

    private record ScoredExperience(ExperienceDocument experience, double score) {}
}
