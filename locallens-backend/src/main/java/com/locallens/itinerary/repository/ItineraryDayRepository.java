package com.locallens.itinerary.repository;

import com.locallens.itinerary.model.ItineraryDayDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ItineraryDayRepository extends MongoRepository<ItineraryDayDocument, String> {
    List<ItineraryDayDocument> findByTripIdOrderByDayNumber(String tripId);
    Optional<ItineraryDayDocument> findByTripIdAndDayNumber(String tripId, int dayNumber);
    void deleteByTripId(String tripId);
}
