package com.georoute.feature.route.controller;

import com.georoute.feature.route.dto.CreateRouteFromMapRequest;
import com.georoute.feature.route.dto.RouteMapResponse;
import com.georoute.feature.route.model.Route;
import com.georoute.feature.route.repository.RouteRepository;
import com.georoute.shared.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/company/routes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CompanyRouteController {

    private final RouteRepository routeRepository;
    private final LocationService locationService;

    @Value("${georoute.maps.max-routes-per-company:10}")
    private Integer maxRoutesPerCompany;

    @PostMapping("/create-from-map")
    public ResponseEntity<RouteMapResponse> createRouteFromMap(
            @Valid @RequestBody CreateRouteFromMapRequest request) {

        log.info("Company creating route from map: {} -> {}",
                request.getStartAddress(), request.getEndAddress());

        // Validar límite de rutas por empresa
        Long currentRoutesCount = routeRepository.countByCompanyIdAndActiveTrue(request.getCompanyId());
        if (currentRoutesCount >= maxRoutesPerCompany) {
            throw new RuntimeException("Company has reached maximum routes limit (" + maxRoutesPerCompany + ")");
        }

        // Validar y calcular distancia si no viene calculada
        Double distanceKm = request.getDistanceKm();
        if (distanceKm == null || distanceKm == 0) {
            distanceKm = locationService.calculateDistance(
                    request.getStartLatitude(), request.getStartLongitude(),
                    request.getEndLatitude(), request.getEndLongitude()
            );
        }

        // Crear ruta con datos de Google Maps
        Route route = Route.builder()
                .name(request.getName())
                .description(request.getDescription())
                .color(request.getColor())
                .startLatitude(request.getStartLatitude())
                .startLongitude(request.getStartLongitude())
                .startAddress(request.getStartAddress())
                .endLatitude(request.getEndLatitude())
                .endLongitude(request.getEndLongitude())
                .endAddress(request.getEndAddress())
                .polyline(request.getPolyline())
                .distanceKm(distanceKm)
                .durationMinutes(request.getDurationMinutes())
                .companyId(request.getCompanyId())
                .active(true)
                .build();

        Route savedRoute = routeRepository.save(route);

        log.info("Route created successfully: {} (ID: {}, Distance: {} km)",
                savedRoute.getName(), savedRoute.getId(), savedRoute.getDistanceKm());

        return ResponseEntity.ok(RouteMapResponse.from(savedRoute));
    }

    @GetMapping("/my-routes/{companyId}")
    public ResponseEntity<Map<String, Object>> getMyRoutes(@PathVariable Long companyId) {
        log.info("Company {} requesting their routes", companyId);

        List<Route> routes = routeRepository.findByCompanyIdAndActiveTrue(companyId);

        Map<String, Object> response = new HashMap<>();
        response.put("routes", routes.stream()
                .map(RouteMapResponse::from)
                .collect(Collectors.toList()));
        response.put("totalRoutes", routes.size());
        response.put("maxRoutesAllowed", maxRoutesPerCompany);
        response.put("remainingSlots", maxRoutesPerCompany - routes.size());
        response.put("companyId", companyId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-routes/{companyId}/{routeId}")
    public ResponseEntity<RouteMapResponse> getMyRoute(
            @PathVariable Long companyId,
            @PathVariable Long routeId) {

        log.info("Company {} requesting route {}", companyId, routeId);

        Route route = routeRepository.findByIdAndCompanyId(routeId, companyId)
                .orElseThrow(() -> new RuntimeException("Route not found or not owned by company"));

        return ResponseEntity.ok(RouteMapResponse.from(route));
    }

    @PutMapping("/my-routes/{companyId}/{routeId}")
    public ResponseEntity<RouteMapResponse> updateMyRoute(
            @PathVariable Long companyId,
            @PathVariable Long routeId,
            @Valid @RequestBody CreateRouteFromMapRequest request) {

        log.info("Company {} updating route {}", companyId, routeId);

        Route route = routeRepository.findByIdAndCompanyId(routeId, companyId)
                .orElseThrow(() -> new RuntimeException("Route not found or not owned by company"));

        // Recalcular distancia si las coordenadas cambiaron
        Double newDistanceKm = request.getDistanceKm();
        if (!route.getStartLatitude().equals(request.getStartLatitude()) ||
                !route.getStartLongitude().equals(request.getStartLongitude()) ||
                !route.getEndLatitude().equals(request.getEndLatitude()) ||
                !route.getEndLongitude().equals(request.getEndLongitude())) {

            newDistanceKm = locationService.calculateDistance(
                    request.getStartLatitude(), request.getStartLongitude(),
                    request.getEndLatitude(), request.getEndLongitude()
            );
        }

        // Actualizar con nuevos datos del mapa
        route.setName(request.getName());
        route.setDescription(request.getDescription());
        route.setColor(request.getColor());
        route.setStartLatitude(request.getStartLatitude());
        route.setStartLongitude(request.getStartLongitude());
        route.setStartAddress(request.getStartAddress());
        route.setEndLatitude(request.getEndLatitude());
        route.setEndLongitude(request.getEndLongitude());
        route.setEndAddress(request.getEndAddress());
        route.setPolyline(request.getPolyline());
        route.setDistanceKm(newDistanceKm);
        route.setDurationMinutes(request.getDurationMinutes());

        Route updatedRoute = routeRepository.save(route);

        log.info("Route updated: {} (Distance: {} km)", updatedRoute.getName(), updatedRoute.getDistanceKm());

        return ResponseEntity.ok(RouteMapResponse.from(updatedRoute));
    }

    @DeleteMapping("/my-routes/{companyId}/{routeId}")
    public ResponseEntity<Map<String, Object>> deleteMyRoute(
            @PathVariable Long companyId,
            @PathVariable Long routeId) {

        log.info("Company {} deleting route {}", companyId, routeId);

        Route route = routeRepository.findByIdAndCompanyId(routeId, companyId)
                .orElseThrow(() -> new RuntimeException("Route not found or not owned by company"));

        // Soft delete
        route.setActive(false);
        routeRepository.save(route);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Route deleted successfully");
        response.put("routeId", routeId);
        response.put("routeName", route.getName());
        response.put("deletedAt", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    // ========== VALIDAR COORDENADAS (UN SOLO MÉTODO) ==========

    @PostMapping("/validate-coordinates")
    public ResponseEntity<Map<String, Object>> validateCoordinates(
            @RequestBody Map<String, Object> coordinates) {

        log.info("Validating coordinates from Google Maps");

        Double startLat = (Double) coordinates.get("startLat");
        Double startLng = (Double) coordinates.get("startLng");
        Double endLat = (Double) coordinates.get("endLat");
        Double endLng = (Double) coordinates.get("endLng");

        // Validar coordenadas usando LocationService
        boolean validStart = locationService.isValidCoordinate(startLat, startLng);
        boolean validEnd = locationService.isValidCoordinate(endLat, endLng);
        boolean allValid = validStart && validEnd;

        // Calcular distancia solo si las coordenadas son válidas
        Double distance = null;
        if (allValid) {
            distance = locationService.calculateDistance(startLat, startLng, endLat, endLng);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("valid", allValid);
        response.put("validStart", validStart);
        response.put("validEnd", validEnd);
        response.put("distance", distance);
        response.put("message", allValid ? "Coordinates are valid" : "Invalid coordinates provided");

        if (distance != null) {
            response.put("distanceFormatted", String.format("%.2f km", distance));
            response.put("estimatedDuration", Math.round(distance * 2)); // ~2 minutos por km
        }

        return ResponseEntity.ok(response);
    }

    // ========== ESTADÍSTICAS ==========

    @GetMapping("/stats/{companyId}")
    public ResponseEntity<Map<String, Object>> getCompanyRouteStats(@PathVariable Long companyId) {
        log.info("Company {} requesting route statistics", companyId);

        List<Route> companyRoutes = routeRepository.findByCompanyIdAndActiveTrue(companyId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRoutes", companyRoutes.size());
        stats.put("maxRoutesAllowed", maxRoutesPerCompany);
        stats.put("usagePercentage", (companyRoutes.size() * 100.0) / maxRoutesPerCompany);

        if (!companyRoutes.isEmpty()) {
            double totalDistance = companyRoutes.stream()
                    .mapToDouble(route -> route.getDistanceKm() != null ? route.getDistanceKm() : 0.0)
                    .sum();

            double averageDistance = totalDistance / companyRoutes.size();

            stats.put("totalNetworkDistance", Math.round(totalDistance * 100.0) / 100.0);
            stats.put("averageRouteDistance", Math.round(averageDistance * 100.0) / 100.0);
            stats.put("shortestRoute", companyRoutes.stream()
                    .mapToDouble(route -> route.getDistanceKm() != null ? route.getDistanceKm() : 0.0)
                    .min().orElse(0.0));
            stats.put("longestRoute", companyRoutes.stream()
                    .mapToDouble(route -> route.getDistanceKm() != null ? route.getDistanceKm() : 0.0)
                    .max().orElse(0.0));
        }

        return ResponseEntity.ok(stats);
    }

    // ========== ENDPOINT DE PRUEBA ==========

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Company Routes API working!");
        response.put("version", "2.0 - Fixed");
        response.put("features", List.of(
                "Create routes from Google Maps",
                "Validate coordinates with LocationService",
                "Route management",
                "Company statistics"
        ));
        response.put("locationService", "Active");
        response.put("maxRoutesPerCompany", maxRoutesPerCompany);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}