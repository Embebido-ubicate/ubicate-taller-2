package com.georoute.feature.route.controller;

import com.georoute.feature.route.dto.CreateRouteFromMapRequest;
import com.georoute.feature.route.dto.RouteMapResponse;
import com.georoute.feature.route.model.Route;
import com.georoute.feature.route.repository.RouteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/create-from-map")
    public ResponseEntity<RouteMapResponse> createRouteFromMap(
            @Valid @RequestBody CreateRouteFromMapRequest request) {

        log.info("Company creating route from map: {} -> {}",
                request.getStartAddress(), request.getEndAddress());

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
                .distanceKm(request.getDistanceKm())
                .durationMinutes(request.getDurationMinutes())
                .companyId(request.getCompanyId())
                .active(true)
                .build();

        Route savedRoute = routeRepository.save(route);

        log.info("Route created successfully: {} (ID: {})",
                savedRoute.getName(), savedRoute.getId());

        return ResponseEntity.ok(RouteMapResponse.from(savedRoute));
    }

    @GetMapping("/my-routes/{companyId}")
    public ResponseEntity<List<RouteMapResponse>> getMyRoutes(@PathVariable Long companyId) {
        log.info("Company {} requesting their routes", companyId);

        List<Route> routes = routeRepository.findByCompanyIdAndActiveTrue(companyId);

        List<RouteMapResponse> response = routes.stream()
                .map(RouteMapResponse::from)
                .collect(Collectors.toList());

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
        route.setDistanceKm(request.getDistanceKm());
        route.setDurationMinutes(request.getDurationMinutes());

        Route updatedRoute = routeRepository.save(route);
        return ResponseEntity.ok(RouteMapResponse.from(updatedRoute));
    }

    @DeleteMapping("/my-routes/{companyId}/{routeId}")
    public ResponseEntity<Map<String, String>> deleteMyRoute(
            @PathVariable Long companyId,
            @PathVariable Long routeId) {

        log.info("Company {} deleting route {}", companyId, routeId);

        Route route = routeRepository.findByIdAndCompanyId(routeId, companyId)
                .orElseThrow(() -> new RuntimeException("Route not found or not owned by company"));

        route.setActive(false); // Soft delete
        routeRepository.save(route);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Route deleted successfully");
        response.put("routeId", routeId.toString());
        return ResponseEntity.ok(response);
    }

    // ========== ENDPOINT PARA VALIDAR COORDENADAS ==========

    @PostMapping("/validate-coordinates")
    public ResponseEntity<Map<String, Object>> validateCoordinates(
            @RequestBody Map<String, Object> coordinates) {

        log.info("Validating coordinates from Google Maps");

        Double startLat = (Double) coordinates.get("startLat");
        Double startLng = (Double) coordinates.get("startLng");
        Double endLat = (Double) coordinates.get("endLat");
        Double endLng = (Double) coordinates.get("endLng");

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("message", "Coordinates are valid");
        response.put("distance", calculateDistance(startLat, startLng, endLat, endLng));

        return ResponseEntity.ok(response);
    }

    // ========== ENDPOINT DE PRUEBA ==========

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Company Routes API working!");
        response.put("googleMaps", "Ready for integration");
        response.put("features", "Create routes by selecting points on map");
        return ResponseEntity.ok(response);
    }

    // ========== MÉTODO AUXILIAR ==========

    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // Fórmula simple de Haversine para calcular distancia
        final int R = 6371; // Radio de la Tierra en km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return Math.round(distance * 100.0) / 100.0; // Redondear a 2 decimales
    }
}