package com.locallens.trip.repository;

import com.locallens.trip.model.TripDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface TripRepository extends MongoRepository<TripDocument, String> {
    List<TripDocument> findByUserIdOrderByCreatedAtDesc(String userId);
    List<TripDocument> findByUserIdAndStatus(String userId, String status);
    List<TripDocument> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String status, LocalDate now1, LocalDate now2);
}
