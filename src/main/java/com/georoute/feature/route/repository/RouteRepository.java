package com.georoute.feature.route.repository;

import com.georoute.feature.route.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    // ========== BÚSQUEDAS BÁSICAS ==========
    List<Route> findByActiveTrue();
    List<Route> findByNameContainingIgnoreCase(String name);

    // ========== BÚSQUEDAS POR EMPRESA ==========
    List<Route> findByCompanyId(Long companyId);
    List<Route> findByCompanyIdAndActiveTrue(Long companyId);

    @Query("SELECT r FROM Route r WHERE r.id = :id AND r.companyId = :companyId")
    Optional<Route> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    // ========== BÚSQUEDAS GEOGRÁFICAS (Para Google Maps) ==========

    @Query("SELECT r FROM Route r WHERE r.active = true AND " +
            "r.startLatitude BETWEEN :minLat AND :maxLat AND " +
            "r.startLongitude BETWEEN :minLng AND :maxLng")
    List<Route> findRoutesStartingInArea(@Param("minLat") Double minLat,
                                         @Param("maxLat") Double maxLat,
                                         @Param("minLng") Double minLng,
                                         @Param("maxLng") Double maxLng);

    @Query("SELECT r FROM Route r WHERE r.active = true AND " +
            "r.endLatitude BETWEEN :minLat AND :maxLat AND " +
            "r.endLongitude BETWEEN :minLng AND :maxLng")
    List<Route> findRoutesEndingInArea(@Param("minLat") Double minLat,
                                       @Param("maxLat") Double maxLat,
                                       @Param("minLng") Double minLng,
                                       @Param("maxLng") Double maxLng);

    // Rutas que pasen cerca de un punto (útil para "¿qué rutas pasan por aquí?")
    @Query("SELECT r FROM Route r WHERE r.active = true AND " +
            "((r.startLatitude BETWEEN :minLat AND :maxLat AND r.startLongitude BETWEEN :minLng AND :maxLng) OR " +
            "(r.endLatitude BETWEEN :minLat AND :maxLat AND r.endLongitude BETWEEN :minLng AND :maxLng))")
    List<Route> findRoutesNearPoint(@Param("minLat") Double minLat,
                                    @Param("maxLat") Double maxLat,
                                    @Param("minLng") Double minLng,
                                    @Param("maxLng") Double maxLng);

    // ========== ESTADÍSTICAS ==========
    Long countByCompanyIdAndActiveTrue(Long companyId);

    @Query("SELECT COUNT(r) FROM Route r WHERE r.active = true")
    Long countActiveRoutes();
}