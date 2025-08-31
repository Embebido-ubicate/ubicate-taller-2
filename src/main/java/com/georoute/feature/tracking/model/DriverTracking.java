package com.georoute.feature.tracking.model;

import com.georoute.feature.tracking.enums.TrackingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "driver_tracking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== CONDUCTOR Y RUTA ==========
    @Column(nullable = false)
    private Long driverId; // ID del conductor

    @Column(nullable = false)
    private Long routeId; // ID de la ruta que está recorriendo

    // ========== UBICACIÓN ACTUAL ==========
    private Double currentLatitude;
    private Double currentLongitude;
    private Double speed; // km/h
    private Double heading; // dirección 0-360°

    // ========== PUNTOS DE INICIO Y RECORRIDO ==========
    private Double startLatitude; // Donde inició el recorrido
    private Double startLongitude;
    private Double totalDistanceKm; // Kilómetros recorridos hasta ahora

    // ========== TIEMPOS ==========
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now(); // Cuándo inició
    private LocalDateTime lastUpdateTime; // Última vez que envió ubicación
    private LocalDateTime endTime; // Cuándo terminó (null si aún en curso)

    // ========== ESTADO DEL RECORRIDO ==========
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TrackingStatus status = TrackingStatus.STARTED;

    // ========== INFORMACIÓN ADICIONAL ==========
    private Integer passengersCount; // Pasajeros actuales (opcional)
    private String notes; // Notas del conductor

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ========== MÉTODOS CALCULADOS ==========

    public Long getElapsedMinutes() {
        if (startTime == null) return 0L;
        LocalDateTime endRef = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, endRef).toMinutes();
    }

    public String getFormattedElapsedTime() {
        Long minutes = getElapsedMinutes();
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    public boolean isActive() {
        return status == TrackingStatus.STARTED || status == TrackingStatus.PAUSED;
    }

    public double getAverageSpeed() {
        Long minutes = getElapsedMinutes();
        if (minutes == 0 || totalDistanceKm == null) return 0.0;
        return (totalDistanceKm * 60.0) / minutes; // km/h
    }

    // Calcular distancia desde última posición conocida
    public void updateLocation(Double newLat, Double newLng, Double currentSpeed) {
        if (currentLatitude != null && currentLongitude != null) {
            double distanceIncrement = calculateDistance(
                    currentLatitude, currentLongitude, newLat, newLng);
            totalDistanceKm = (totalDistanceKm != null ? totalDistanceKm : 0.0) + distanceIncrement;
        }

        this.currentLatitude = newLat;
        this.currentLongitude = newLng;
        this.speed = currentSpeed;
        this.lastUpdateTime = LocalDateTime.now();
    }

    // Fórmula de Haversine para calcular distancia
    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int R = 6371; // Radio de la Tierra en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return Math.round(distance * 1000.0) / 1000.0; // Redondear a 3 decimales
    }
}