package com.georoute.feature.route.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // "Línea A - Centro Norte"

    private String description;

    private String color; // Para mostrar en el mapa

    // ========== GOOGLE MAPS DATA ==========

    // Parada de INICIO
    @Column(nullable = false)
    private Double startLatitude;

    @Column(nullable = false)
    private Double startLongitude;

    private String startAddress; // Dirección legible desde Google Maps

    // Parada de FIN
    @Column(nullable = false)
    private Double endLatitude;

    @Column(nullable = false)
    private Double endLongitude;

    private String endAddress; // Dirección legible desde Google Maps

    // Ruta completa (polyline de Google Maps)
    @Column(columnDefinition = "TEXT")
    private String polyline; // Polyline encodado de Google Maps

    // Distancia y duración calculada por Google Maps
    private Double distanceKm; // Distancia en kilómetros
    private Integer durationMinutes; // Duración en minutos

    // ========== EMPRESA Y ESTADO ==========
    @Column(nullable = false)
    private Long companyId; // ID de la empresa que registró esta ruta

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    // ========== MÉTODOS AUXILIARES ==========

    public String getRouteInfo() {
        return String.format("%s: %s → %s", name, startAddress, endAddress);
    }

    public boolean hasValidCoordinates() {
        return startLatitude != null && startLongitude != null &&
                endLatitude != null && endLongitude != null;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}