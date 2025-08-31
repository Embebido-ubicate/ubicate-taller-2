package com.georoute.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class LocationService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 0.0;
        }

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round(EARTH_RADIUS_KM * c * 100.0) / 100.0;
    }

    public boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    public Map<String, Double> calculateBounds(Double centerLat, Double centerLng, Double radiusKm) {
        double latRadius = radiusKm / 111.0;
        double lngRadius = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));

        return Map.of(
                "minLat", centerLat - latRadius,
                "maxLat", centerLat + latRadius,
                "minLng", centerLng - lngRadius,
                "maxLng", centerLng + lngRadius
        );
    }
}