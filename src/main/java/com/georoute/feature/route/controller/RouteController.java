package com.georoute.feature.route.controller;

import com.georoute.feature.route.model.Route;
import com.georoute.feature.route.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RouteController {

    private final RouteRepository routeRepository;

    @GetMapping
    public ResponseEntity<List<Route>> getAllRoutes() {
        log.info("Fetching all active routes");
        List<Route> routes = routeRepository.findByActiveTrue();
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Route> getRoute(@PathVariable Long id) {
        log.info("Fetching route: {}", id);

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));

        return ResponseEntity.ok(route);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Route>> searchRoutes(@RequestParam String name) {
        log.info("Searching routes: {}", name);
        List<Route> routes = routeRepository.findByNameContainingIgnoreCase(name);
        return ResponseEntity.ok(routes);
    }

    // ========== ENDPOINTS DE ADMIN (Por ahora sin seguridad) ==========

    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestBody Route route) {
        log.info("Creating route: {}", route.getName());

        Route savedRoute = routeRepository.save(route);
        return ResponseEntity.ok(savedRoute);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Route> updateRoute(@PathVariable Long id, @RequestBody Route routeUpdate) {
        log.info("Updating route: {}", id);

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));

        route.setName(routeUpdate.getName());
        route.setDescription(routeUpdate.getDescription());
        route.setColor(routeUpdate.getColor());

        Route updatedRoute = routeRepository.save(route);
        return ResponseEntity.ok(updatedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRoute(@PathVariable Long id) {
        log.info("Deleting route: {}", id);

        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found: " + id));

        route.setActive(false); // Soft delete
        routeRepository.save(route);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Route deleted successfully");
        return ResponseEntity.ok(response);
    }

    // ========== TEST ==========

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Routes API working!");
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }
}