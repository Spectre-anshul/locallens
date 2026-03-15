package com.locallens.notification.controller;

import com.locallens.auth.security.UserPrincipal;
import com.locallens.notification.model.NotificationDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final MongoTemplate mongoTemplate;

    @GetMapping
    public ResponseEntity<List<NotificationDocument>> getNotifications(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Query query = new Query(Criteria.where("userId").is(user.getId()));
        if (unreadOnly) query.addCriteria(Criteria.where("read").is(false));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(50);
        return ResponseEntity.ok(mongoTemplate.find(query, NotificationDocument.class));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        mongoTemplate.updateFirst(
                new Query(Criteria.where("id").is(id)),
                new Update().set("read", true),
                NotificationDocument.class);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal user) {
        long count = mongoTemplate.count(
                new Query(Criteria.where("userId").is(user.getId()).and("read").is(false)),
                NotificationDocument.class);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
