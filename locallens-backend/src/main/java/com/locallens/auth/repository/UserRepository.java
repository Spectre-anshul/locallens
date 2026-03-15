package com.locallens.auth.repository;

import com.locallens.auth.model.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<UserDocument> findByOauthProviderId(String oauthProviderId);
}
