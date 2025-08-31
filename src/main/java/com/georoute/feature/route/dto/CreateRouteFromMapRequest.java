package com.georoute.feature.route.dto;

import com.georoute.feature.route.model.Route;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateRouteFromMapRequest {

    @NotBlank(message = "Route name is required")
    private String name;

    private String description;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Invalid color format")
    private String color = "#2196F3";

    // ========== PARADA DE INICIO (desde Google Maps) ==========
    @NotNull(message = "Start latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double startLatitude;

    @NotNull(message = "Start longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double startLongitude;

    private String startAddress; // "Plaza de Armas, Trujillo, Perú"

    // ========== PARADA DE FIN (desde Google Maps) ==========
    @NotNull(message = "End latitude is required")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double endLatitude;

    @NotNull(message = "End longitude is required")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double endLongitude;

    private String endAddress; // "Hospital Regional, Trujillo, Perú"

    // ========== DATOS DE GOOGLE MAPS ==========
    private String polyline; // Polyline encodado de Google Directions
    private Double distanceKm; // Calculado por Google Maps
    private Integer durationMinutes; // Calculado por Google Maps

    @NotNull(message = "Company ID is required")
    private Long companyId;
}