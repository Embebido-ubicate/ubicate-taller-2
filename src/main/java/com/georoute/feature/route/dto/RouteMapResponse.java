package com.georoute.feature.route.dto;

import com.georoute.feature.route.model.Route;
import lombok.Data;

@Data
public class RouteMapResponse {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Boolean active;

    // Coordenadas para el mapa
    private Double startLatitude;
    private Double startLongitude;
    private String startAddress;

    private Double endLatitude;
    private Double endLongitude;
    private String endAddress;

    // Datos del mapa
    private String polyline;
    private Double distanceKm;
    private Integer durationMinutes;

    private Long companyId;
    private String createdAt;

    public static RouteMapResponse from(Route route) {
        RouteMapResponse response = new RouteMapResponse();
        response.setId(route.getId());
        response.setName(route.getName());
        response.setDescription(route.getDescription());
        response.setColor(route.getColor());
        response.setActive(route.getActive());
        response.setStartLatitude(route.getStartLatitude());
        response.setStartLongitude(route.getStartLongitude());
        response.setStartAddress(route.getStartAddress());
        response.setEndLatitude(route.getEndLatitude());
        response.setEndLongitude(route.getEndLongitude());
        response.setEndAddress(route.getEndAddress());
        response.setPolyline(route.getPolyline());
        response.setDistanceKm(route.getDistanceKm());
        response.setDurationMinutes(route.getDurationMinutes());
        response.setCompanyId(route.getCompanyId());
        response.setCreatedAt(route.getCreatedAt().toString());
        return response;
    }
}