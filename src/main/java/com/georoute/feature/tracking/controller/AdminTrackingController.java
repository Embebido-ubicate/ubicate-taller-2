package com.georoute.feature.tracking.controller;

import com.georoute.feature.tracking.enums.TrackingStatus;
import com.georoute.feature.tracking.model.DriverTracking;
import com.georoute.feature.tracking.repository.DriverTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/tracking")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminTrackingController {

    private final DriverTrackingRepository trackingRepository;

    // ========== VER TODOS LOS CONDUCTORES ACTIVOS (TIEMPO REAL) ==========
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> getLiveTracking() {
        log.info("Admin requesting live tracking data");

        List<DriverTracking> activeTrips = trackingRepository.findAllActiveTrips();

        List<Map<String, Object>> liveData = activeTrips.stream()
                .map(this::mapToLiveTrackingData)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalActiveDrivers", liveData.size());
        response.put("timestamp", LocalDateTime.now());
        response.put("drivers", liveData);

        return ResponseEntity.ok(response);
    }

    // ========== VER CONDUCTORES EN ÁREA ESPECÍFICA (SIMPLIFICADO) ==========
    @GetMapping("/live/area")
    public ResponseEntity<Map<String, Object>> getLiveTrackingInArea(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "0.05") Double radius) {

        log.info("Admin requesting live tracking in area [{}, {}] with radius {}",
                latitude, longitude, radius);

        // SIMPLIFICADO: Por ahora devolvemos todos los activos
        // TODO: Implementar filtro geográfico después
        List<DriverTracking> activeTrips = trackingRepository.findAllActiveTrips();

        List<Map<String, Object>> liveData = activeTrips.stream()
                .map(this::mapToLiveTrackingData)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("driversInArea", liveData.size());
        response.put("searchArea", Map.of(
                "center", Map.of("lat", latitude, "lng", longitude),
                "radius", radius
        ));
        response.put("drivers", liveData);
        response.put("note", "Geographic filtering will be added in next version");

        return ResponseEntity.ok(response);
    }

    // ========== ESTADÍSTICAS GENERALES ==========
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTrackingStats() {
        log.info("Admin requesting tracking statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeTrips", trackingRepository.countByStatus(TrackingStatus.STARTED));
        stats.put("pausedTrips", trackingRepository.countByStatus(TrackingStatus.PAUSED));
        stats.put("finishedTrips", trackingRepository.countByStatus(TrackingStatus.FINISHED));
        stats.put("totalTrips", trackingRepository.count());

        // SIMPLIFICADO: Solo contamos los que están en STARTED
        List<DriverTracking> startedTrips = trackingRepository.findByStatus(TrackingStatus.STARTED);
        stats.put("driversWithoutSignal", 0); // TODO: Implementar lógica de señal después

        stats.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(stats);
    }

    // ========== VER DETALLE DE UN CONDUCTOR ESPECÍFICO ==========
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<Map<String, Object>> getDriverTracking(@PathVariable Long driverId) {
        log.info("Admin requesting tracking details for driver {}", driverId);

        var activeTrip = trackingRepository.findActiveByDriverId(driverId);

        if (!activeTrip.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("driverId", driverId);
            response.put("hasActiveTrip", false);
            response.put("message", "Driver has no active trip");
            return ResponseEntity.ok(response);
        }

        DriverTracking tracking = activeTrip.get();
        Map<String, Object> response = new HashMap<>();
        response.put("driverId", driverId);
        response.put("hasActiveTrip", true);
        response.put("trackingDetails", mapToDetailedTrackingData(tracking));

        return ResponseEntity.ok(response);
    }

    // ========== HISTORIAL COMPLETO ==========
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getTrackingHistory() {
        log.info("Admin requesting tracking history");

        List<DriverTracking> allTrips = trackingRepository.findAll();

        Map<String, Object> response = new HashMap<>();
        response.put("totalTrips", allTrips.size());
        response.put("trips", allTrips.stream()
                .map(this::mapToHistoryData)
                .toList());

        return ResponseEntity.ok(response);
    }

    // ========== MÉTODOS AUXILIARES (SIMPLES Y FUNCIONALES) ==========

    private Map<String, Object> mapToLiveTrackingData(DriverTracking tracking) {
        Map<String, Object> data = new HashMap<>();
        data.put("driverId", tracking.getDriverId());
        data.put("routeId", tracking.getRouteId());
        data.put("status", tracking.getStatus().name());
        data.put("statusDescription", tracking.getStatus().getDescription());

        // Ubicación actual
        Map<String, Object> location = new HashMap<>();
        location.put("lat", tracking.getCurrentLatitude() != null ? tracking.getCurrentLatitude() : 0.0);
        location.put("lng", tracking.getCurrentLongitude() != null ? tracking.getCurrentLongitude() : 0.0);
        data.put("currentLocation", location);

        // Datos del viaje
        data.put("speed", tracking.getSpeed() != null ? tracking.getSpeed() : 0.0);
        data.put("elapsedMinutes", tracking.getElapsedMinutes());
        data.put("distanceKm", tracking.getTotalDistanceKm() != null ? tracking.getTotalDistanceKm() : 0.0);
        data.put("passengersCount", tracking.getPassengersCount() != null ? tracking.getPassengersCount() : 0);
        data.put("lastUpdate", tracking.getLastUpdateTime());

        return data;
    }

    private Map<String, Object> mapToDetailedTrackingData(DriverTracking tracking) {
        Map<String, Object> data = new HashMap<>();
        data.put("trackingId", tracking.getId());
        data.put("driverId", tracking.getDriverId());
        data.put("routeId", tracking.getRouteId());
        data.put("status", tracking.getStatus().name());
        data.put("statusDescription", tracking.getStatus().getDescription());
        data.put("startTime", tracking.getStartTime());

        // Ubicación de inicio
        Map<String, Object> startLocation = new HashMap<>();
        startLocation.put("lat", tracking.getStartLatitude() != null ? tracking.getStartLatitude() : 0.0);
        startLocation.put("lng", tracking.getStartLongitude() != null ? tracking.getStartLongitude() : 0.0);
        data.put("startLocation", startLocation);

        // Ubicación actual
        Map<String, Object> currentLocation = new HashMap<>();
        currentLocation.put("lat", tracking.getCurrentLatitude() != null ? tracking.getCurrentLatitude() : 0.0);
        currentLocation.put("lng", tracking.getCurrentLongitude() != null ? tracking.getCurrentLongitude() : 0.0);
        data.put("currentLocation", currentLocation);

        // Estadísticas
        data.put("elapsedMinutes", tracking.getElapsedMinutes());
        data.put("totalDistanceKm", tracking.getTotalDistanceKm() != null ? tracking.getTotalDistanceKm() : 0.0);
        data.put("averageSpeed", tracking.getAverageSpeed());
        data.put("currentSpeed", tracking.getSpeed() != null ? tracking.getSpeed() : 0.0);
        data.put("passengersCount", tracking.getPassengersCount() != null ? tracking.getPassengersCount() : 0);
        data.put("lastUpdate", tracking.getLastUpdateTime());
        data.put("notes", tracking.getNotes());

        return data;
    }

    private Map<String, Object> mapToHistoryData(DriverTracking tracking) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", tracking.getId());
        data.put("driverId", tracking.getDriverId());
        data.put("routeId", tracking.getRouteId());
        data.put("status", tracking.getStatus().name());
        data.put("statusDescription", tracking.getStatus().getDescription());
        data.put("date", tracking.getStartTime().toLocalDate());
        data.put("startTime", tracking.getStartTime());
        data.put("endTime", tracking.getEndTime());
        data.put("totalDistanceKm", tracking.getTotalDistanceKm() != null ? tracking.getTotalDistanceKm() : 0.0);
        data.put("elapsedMinutes", tracking.getElapsedMinutes());
        data.put("averageSpeed", tracking.getAverageSpeed());

        return data;
    }

    // ========== TEST ==========
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Admin Tracking API working!");
        response.put("features", "Live tracking, driver monitoring, statistics");
        response.put("realTime", "Yes - see all drivers on map");
        response.put("status", "FUNCTIONAL");
        return ResponseEntity.ok(response);
    }
}