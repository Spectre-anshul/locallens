package com.locallens.event.repository;

import com.locallens.event.model.EventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends MongoRepository<EventDocument, String> {
    Page<EventDocument> findByLocationCityAndStartTimeBetweenAndStatus(
            String city, Instant start, Instant end, String status, Pageable pageable);
    List<EventDocument> findByLocationCityAndStartTimeBetween(String city, Instant start, Instant end);
}
