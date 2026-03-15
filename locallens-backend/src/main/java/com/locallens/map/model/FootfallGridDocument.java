package com.locallens.map.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "footfall_grid")
public class FootfallGridDocument {
    @Id private String id;
    private String city;
    private GeoJsonPolygon gridCell;
    private GeoJsonPoint centroid;
    private int hour;
    private int dayOfWeek;
    private double footfallScore;
    private String crowdLevel; // LOW, MODERATE, HIGH, VERY_HIGH
    private int dataPoints;
    private String bestTimeToVisit;
    private Instant lastComputedAt;
    private Instant expiresAt;
}
