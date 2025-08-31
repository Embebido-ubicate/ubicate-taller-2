package com.georoute.feature.route.controller;

import com.georoute.feature.route.dto.RouteMapResponse;
import com.georoute.feature.route.model.Route;
import com.georoute.feature.route.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/routes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PublicRouteController {

    private final RouteRepository routeRepository;

    // ========== ENDPOINTS PÚBLICOS (Para Pasajeros) ==========

    @GetMapping
    public ResponseEntity<List<RouteMapResponse>> getAllPublicRoutes() {
        log.info("Public request: All active routes");

        List<Route> routes = routeRepository.findByActiveTrue();
        List<RouteMapResponse> response = routes.stream()
                .map(RouteMapResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteMapResponse> getPublicRoute(@PathVariable Long id) {
        log.info("Public request: Route details for ID {}", id);

        Route route = routeRepository.findById(id)
                .filter(Route::getActive)
                .orElseThrow(() -> new RuntimeException("Route not found or inactive"));

        return ResponseEntity.ok(RouteMapResponse.from(route));
    }

    @GetMapping("/search")
    public ResponseEntity<List<RouteMapResponse>> searchPublicRoutes(@RequestParam String name) {
        log.info("Public search: Routes containing '{}'", name);

        List<Route> routes = routeRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .filter(Route::getActive)
                .collect(Collectors.toList());

        List<RouteMapResponse> response = routes.stream()
                .map(RouteMapResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ========== BÚSQUEDAS GEOGRÁFICAS (Para Google Maps) ==========

    @GetMapping("/near")
    public ResponseEntity<List<RouteMapResponse>> getRoutesNearLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "0.01") Double radius) {

        log.info("Public search: Routes near [{}, {}] within {} radius", latitude, longitude, radius);

        // Calcular área de búsqueda
        Double minLat = latitude - radius;
        Double maxLat = latitude + radius;
        Double minLng = longitude - radius;
        Double maxLng = longitude + radius;

        List<Route> routes = routeRepository.findRoutesNearPoint(minLat, maxLat, minLng, maxLng);
        List<RouteMapResponse> response = routes.stream()
                .map(RouteMapResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/from-area")
    public ResponseEntity<List<RouteMapResponse>> getRoutesFromArea(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "0.005") Double radius) {

        log.info("Public search: Routes starting near [{}, {}]", latitude, longitude);

        Double minLat = latitude - radius;
        Double maxLat = latitude + radius;
        Double minLng = longitude - radius;
        Double maxLng = longitude + radius;

        List<Route> routes = routeRepository.findRoutesStartingInArea(minLat, maxLat, minLng, maxLng);
        List<RouteMapResponse> response = routes.stream()
                .map(RouteMapResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/to-area")
    public ResponseEntity<List<RouteMapResponse>> getRoutesToArea(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "0.005") Double radius) {

        log.info("Public search: Routes ending near [{}, {}]", latitude, longitude);

        Double minLat = latitude - radius;
        Double maxLat = latitude + radius;
        Double minLng = longitude - radius;
        Double maxLng = longitude + radius;

        List<Route> routes = routeRepository.findRoutesEndingInArea(minLat, maxLat, minLng, maxLng);
        List<RouteMapResponse> response = routes.stream()
                .map(RouteMapResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ========== ESTADÍSTICAS PÚBLICAS ==========

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPublicStats() {
        log.info("Public request: Route statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActiveRoutes", routeRepository.countActiveRoutes());
        stats.put("message", "Public transport routes available");

        return ResponseEntity.ok(stats);
    }

    // ========== TEST ==========

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Public Routes API working!");
        response.put("googleMapsReady", "Yes");
        response.put("features", "Search routes by location, view route details");
        return ResponseEntity.ok(response);
    }
}