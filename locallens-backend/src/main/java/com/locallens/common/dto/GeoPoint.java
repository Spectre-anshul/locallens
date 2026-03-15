package com.locallens.common.dto;

import lombok.Data;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

@Data
public class GeoPoint {
    private double lat;
    private double lng;

    public GeoJsonPoint toGeoJsonPoint() {
        return new GeoJsonPoint(lng, lat);
    }

    public static GeoPoint from(GeoJsonPoint point) {
        GeoPoint gp = new GeoPoint();
        gp.setLng(point.getX());
        gp.setLat(point.getY());
        return gp;
    }
}
