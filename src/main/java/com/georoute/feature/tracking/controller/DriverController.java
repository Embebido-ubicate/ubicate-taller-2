package com.georoute.feature.tracking.controller;

import com.georoute.feature.tracking.enums.TrackingStatus;
import com.georoute.feature.tracking.model.DriverTracking;
import com.georoute.feature.tracking.repository.DriverTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DriverController {

    private final DriverTrackingRepository trackingRepository;

    // ========== INICIAR RECORRIDO ==========
    @PostMapping("/start-trip")
    public ResponseEntity<Map<String, Object>> startTrip(@RequestBody Map<String, Object> request) {
        // TODO: Obtener driverId del JWT token
        Long driverId = ((Number) request.get("driverId")).longValue();
        Long routeId = ((Number) request.get("routeId")).longValue();
        Double startLat = (Double) request.get("latitude");
        Double startLng = (Double) request.get("longitude");

        log.info("Driver {} starting trip on route {} at [{}, {}]",
                driverId, routeId, startLat, startLng);

        // Verificar si ya tiene un recorrido activo
        Optional<DriverTracking> activeTrip = trackingRepository
                .findActiveByDriverId(driverId);

        if (activeTrip.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Ya tienes un recorrido activo");
            response.put("activeTripId", activeTrip.get().getId());
            return ResponseEntity.badRequest().body(response);
        }

        // Crear nuevo tracking
        DriverTracking tracking = DriverTracking.builder()
                .driverId(driverId)
                .routeId(routeId)
                .startLatitude(startLat)
                .startLongitude(startLng)
                .currentLatitude(startLat)
                .currentLongitude(startLng)
                .totalDistanceKm(0.0)
                .speed(0.0)
                .status(TrackingStatus.STARTED)
                .build();

        DriverTracking savedTracking = trackingRepository.save(tracking);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Recorrido iniciado exitosamente");
        response.put("trackingId", savedTracking.getId());
        response.put("startTime", savedTracking.getStartTime());
        response.put("status", savedTracking.getStatus());

        return ResponseEntity.ok(response);
    }

    // ========== ENVIAR UBICACIÓN EN TIEMPO REAL ==========
    @PostMapping("/send-location")
    public ResponseEntity<Map<String, Object>> sendLocation(@RequestBody Map<String, Object> request) {
        Long driverId = ((Number) request.get("driverId")).longValue();
        Double currentLat = (Double) request.get("latitude");
        Double currentLng = (Double) request.get("longitude");
        Double currentSpeed = (Double) request.get("speed"); // km/h desde GPS
        Integer passengers = request.get("passengers") != null ?
                ((Number) request.get("passengers")).intValue() : null;

        log.debug("Driver {} sending location: [{}, {}] at {} km/h",
                driverId, currentLat, currentLng, currentSpeed);

        // Buscar recorrido activo
        DriverTracking tracking = trackingRepository
                .findActiveByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("No tienes un recorrido activo"));

        // Actualizar ubicación y calcular distancia
        tracking.updateLocation(currentLat, currentLng, currentSpeed);
        if (passengers != null) {
            tracking.setPassengersCount(passengers);
        }

        DriverTracking updatedTracking = trackingRepository.save(tracking);

        // Respuesta con estadísticas actualizadas
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ubicación actualizada");
        response.put("currentLocation", Map.of("lat", currentLat, "lng", currentLng));
        response.put("totalDistanceKm", updatedTracking.getTotalDistanceKm());
        response.put("elapsedTime", updatedTracking.getFormattedElapsedTime());
        response.put("averageSpeed", updatedTracking.getAverageSpeed());
        response.put("currentSpeed", currentSpeed);

        return ResponseEntity.ok(response);
    }

    // ========== VER ESTADO ACTUAL DEL RECORRIDO ==========
    @GetMapping("/current-trip/{driverId}")
    public ResponseEntity<Map<String, Object>> getCurrentTrip(@PathVariable Long driverId) {
        log.info("Driver {} requesting current trip status", driverId);

        Optional<DriverTracking> activeTrip = trackingRepository
                .findActiveByDriverId(driverId);

        if (!activeTrip.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("hasActiveTrip", false);
            response.put("message", "No tienes recorridos activos");
            return ResponseEntity.ok(response);
        }

        DriverTracking tracking = activeTrip.get();
        Map<String, Object> response = new HashMap<>();
        response.put("hasActiveTrip", true);
        response.put("trackingId", tracking.getId());
        response.put("routeId", tracking.getRouteId());
        response.put("status", tracking.getStatus());
        response.put("startTime", tracking.getStartTime());
        response.put("elapsedTime", tracking.getFormattedElapsedTime());
        response.put("totalDistanceKm", tracking.getTotalDistanceKm());
        response.put("averageSpeed", tracking.getAverageSpeed());
        response.put("currentSpeed", tracking.getSpeed());
        response.put("currentLocation", Map.of(
                "lat", tracking.getCurrentLatitude(),
                "lng", tracking.getCurrentLongitude()
        ));
        response.put("passengersCount", tracking.getPassengersCount());

        return ResponseEntity.ok(response);
    }

    // ========== PAUSAR RECORRIDO ==========
    @PostMapping("/pause-trip/{driverId}")
    public ResponseEntity<Map<String, Object>> pauseTrip(@PathVariable Long driverId) {
        log.info("Driver {} pausing trip", driverId);

        DriverTracking tracking = trackingRepository
                .findByDriverIdAndStatus(driverId, TrackingStatus.STARTED)
                .orElseThrow(() -> new RuntimeException("No tienes un recorrido activo para pausar"));

        tracking.setStatus(TrackingStatus.PAUSED);
        trackingRepository.save(tracking);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Recorrido pausado");
        response.put("status", tracking.getStatus());
        response.put("elapsedTime", tracking.getFormattedElapsedTime());

        return ResponseEntity.ok(response);
    }

    // ========== REANUDAR RECORRIDO ==========
    @PostMapping("/resume-trip/{driverId}")
    public ResponseEntity<Map<String, Object>> resumeTrip(@PathVariable Long driverId) {
        log.info("Driver {} resuming trip", driverId);

        DriverTracking tracking = trackingRepository
                .findByDriverIdAndStatus(driverId, TrackingStatus.PAUSED)
                .orElseThrow(() -> new RuntimeException("No tienes un recorrido pausado para reanudar"));

        tracking.setStatus(TrackingStatus.STARTED);
        trackingRepository.save(tracking);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Recorrido reanudado");
        response.put("status", tracking.getStatus());

        return ResponseEntity.ok(response);
    }

    // ========== FINALIZAR RECORRIDO ==========
    @PostMapping("/finish-trip/{driverId}")
    public ResponseEntity<Map<String, Object>> finishTrip(
            @PathVariable Long driverId,
            @RequestBody(required = false) Map<String, Object> request) {

        log.info("Driver {} finishing trip", driverId);

        DriverTracking tracking = trackingRepository
                .findActiveByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("No tienes un recorrido activo para finalizar"));

        tracking.setStatus(TrackingStatus.FINISHED);
        tracking.setEndTime(java.time.LocalDateTime.now());

        if (request != null && request.get("notes") != null) {
            tracking.setNotes((String) request.get("notes"));
        }

        DriverTracking finishedTracking = trackingRepository.save(tracking);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Recorrido finalizado exitosamente");
        response.put("summary", Map.of(
                "totalDistanceKm", finishedTracking.getTotalDistanceKm(),
                "totalTime", finishedTracking.getFormattedElapsedTime(),
                "averageSpeed", finishedTracking.getAverageSpeed(),
                "startTime", finishedTracking.getStartTime(),
                "endTime", finishedTracking.getEndTime()
        ));

        return ResponseEntity.ok(response);
    }

    // ========== HISTORIAL DE RECORRIDOS ==========
    @GetMapping("/trip-history/{driverId}")
    public ResponseEntity<Map<String, Object>> getTripHistory(@PathVariable Long driverId) {
        log.info("Driver {} requesting trip history", driverId);

        var trips = trackingRepository.findByDriverIdOrderByStartTimeDesc(driverId);

        Map<String, Object> response = new HashMap<>();
        response.put("totalTrips", trips.size());
        response.put("trips", trips.stream().map(trip -> Map.of(
                "id", trip.getId(),
                "routeId", trip.getRouteId(),
                "date", trip.getStartTime().toLocalDate(),
                "startTime", trip.getStartTime(),
                "endTime", trip.getEndTime(),
                "status", trip.getStatus(),
                "distanceKm", trip.getTotalDistanceKm(),
                "elapsedTime", trip.getFormattedElapsedTime(),
                "averageSpeed", trip.getAverageSpeed()
        )).toList());

        return ResponseEntity.ok(response);
    }

    // ========== TEST PARA CONDUCTOR ==========
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Driver API working!");
        response.put("features", "Real-time tracking, trip management");
        response.put("device", "Optimized for mobile phones");
        return ResponseEntity.ok(response);
    }
}