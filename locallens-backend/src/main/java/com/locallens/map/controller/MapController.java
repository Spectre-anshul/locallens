package com.locallens.map.controller;

import com.locallens.map.model.FootfallGridDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MongoTemplate mongoTemplate;

    @GetMapping("/heatmap")
    public ResponseEntity<Map<String, Object>> getHeatmap(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam double radiusKm, @RequestParam(defaultValue = "-1") int hour) {
        Query query = new Query();
        query.addCriteria(Criteria.where("centroid")
                .nearSphere(new org.springframework.data.geo.Point(lng, lat))
                .maxDistance(radiusKm / 6378.1)); // Convert km to radians
        if (hour >= 0) query.addCriteria(Criteria.where("hour").is(hour));
        query.limit(500);

        List<FootfallGridDocument> grids = mongoTemplate.find(query, FootfallGridDocument.class);
        return ResponseEntity.ok(Map.of("gridCells", grids, "filterHour", hour));
    }

    @GetMapping("/pois")
    public ResponseEntity<List<Map<String, Object>>> getPOIs(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam double radiusKm, @RequestParam(required = false) String category) {
        // TODO: Query experiences + attractions within radius
        return ResponseEntity.ok(List.of());
    }
}
