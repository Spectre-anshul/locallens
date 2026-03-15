package com.locallens.itinerary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallens.event.model.EventDocument;
import com.locallens.marketplace.model.ExperienceDocument;
import com.locallens.trip.model.TripDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final ObjectMapper objectMapper;

    public String buildSystemPrompt() {
        return """
            You are LocalLens AI, an expert travel itinerary planner specializing in hyperlocal experiences.
            
            Generate detailed day-by-day travel itineraries that blend popular attractions with authentic local experiences.
            You prioritize hidden gems over tourist traps and always include hyperlocal experiences from verified local creators.
            
            You MUST respond with valid JSON matching the itinerary schema with fields:
            days[] -> dayNumber, date, theme, slots[] -> startTime, endTime, activity{}, transport{}
            
            Rules:
            1. Include at least one hyperlocal experience per day marked with isHyperlocal: true
            2. Schedule realistic time gaps for transport between locations
            3. Respect the user's budget
            4. Match activities to travel style
            5. Include proper meal slots
            6. Transport suggestions must include specific station/route names
            """;
    }

    public String buildGenerationPrompt(TripDocument trip, List<ExperienceDocument> experiences,
                                         List<EventDocument> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Trip Details\n");
        sb.append("- Destination: ").append(trip.getDestination().getCity())
          .append(", ").append(trip.getDestination().getCountry()).append("\n");
        sb.append("- Dates: ").append(trip.getStartDate()).append(" to ").append(trip.getEndDate())
          .append(" (").append(trip.getDurationDays()).append(" days)\n");
        sb.append("- Budget: ").append(trip.getBudget().getTotal()).append(" ").append(trip.getBudget().getCurrency()).append("\n");
        sb.append("- Travel Style: ").append(trip.getTravelStyle()).append("\n");
        sb.append("- Interests: ").append(String.join(", ", trip.getInterests())).append("\n");
        sb.append("- Group Size: ").append(trip.getGroupSize()).append("\n");

        sb.append("\n## Available Hyperlocal Experiences\n");
        for (ExperienceDocument exp : experiences) {
            sb.append("- ").append(exp.getTitle()).append(" (").append(exp.getCategory())
              .append(", ").append(exp.getPrice().getAmount()).append(" ").append(exp.getPrice().getCurrency())
              .append(", ").append(exp.getDuration()).append("min, ID: ").append(exp.getId()).append(")\n");
        }

        sb.append("\n## Local Events\n");
        for (EventDocument evt : events) {
            sb.append("- ").append(evt.getTitle()).append(" (").append(evt.getStartTime()).append(")\n");
        }

        sb.append("\nGenerate a complete ").append(trip.getDurationDays())
          .append("-day itinerary as valid JSON.");
        return sb.toString();
    }

    public String buildReplanPrompt(String currentDayJson, String reason, String context) {
        return """
            A change requires replanning this day. Keep as many original activities as possible.
            Only change what's directly affected.
            
            Reason: %s
            Context: %s
            
            Current Day Plan:
            %s
            
            Return the COMPLETE updated day as JSON plus a replanSummary field.
            """.formatted(reason, context, currentDayJson);
    }
}
