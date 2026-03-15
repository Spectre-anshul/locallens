package com.locallens.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record CreateTripRequest(
    @NotBlank String title,
    @NotBlank String destinationCity,
    @NotBlank String destinationCountry,
    double lat,
    double lng,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @Positive int groupSize,
    @NotBlank String travelStyle,
    List<String> interests,
    List<String> accessibilityNeeds,
    @Positive long budgetTotal,
    @NotBlank String budgetCurrency
) {}
