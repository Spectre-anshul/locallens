package com.locallens.marketplace.repository;

import com.locallens.marketplace.model.BookingDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookingRepository extends MongoRepository<BookingDocument, String> {
    Page<BookingDocument> findByTravelerIdOrderByCreatedAtDesc(String travelerId, Pageable pageable);
    Page<BookingDocument> findByCreatorIdOrderByDateDesc(String creatorId, Pageable pageable);
    List<BookingDocument> findByCreatorIdAndStatus(String creatorId, String status);
}
