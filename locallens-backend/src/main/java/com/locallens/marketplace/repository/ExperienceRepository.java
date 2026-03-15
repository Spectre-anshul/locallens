package com.locallens.marketplace.repository;

import com.locallens.marketplace.model.ExperienceDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExperienceRepository extends MongoRepository<ExperienceDocument, String> {
    Page<ExperienceDocument> findByLocationCityAndStatusAndCategory(String city, String status, String category, Pageable pageable);
    Page<ExperienceDocument> findByLocationCityAndStatus(String city, String status, Pageable pageable);
    List<ExperienceDocument> findByCreatorId(String creatorId);
    List<ExperienceDocument> findByLocationCityAndStatusAndHyperlocalTrue(String city, String status);
}
