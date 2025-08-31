package com.georoute.feature.route.dto;

import lombok.Data;

@Data
public class MapCoordinate {
    private Double latitude;
    private Double longitude;
    private String address;
    private String name;
}