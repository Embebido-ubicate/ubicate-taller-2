package com.georoute.feature.tracking.repository;

import com.georoute.feature.tracking.enums.TrackingStatus;
import com.georoute.feature.tracking.model.DriverTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverTrackingRepository extends JpaRepository<DriverTracking, Long> {

    // ========== MÉTODOS BÁSICOS SIMPLES ==========
    List<DriverTracking> findByDriverId(Long driverId);
    List<DriverTracking> findByDriverIdOrderByStartTimeDesc(Long driverId);

    Optional<DriverTracking> findByDriverIdAndStatus(Long driverId, TrackingStatus status);

    List<DriverTracking> findByStatus(TrackingStatus status);
    List<DriverTracking> findByRouteId(Long routeId);

    // ========== MÉTODOS SIMPLES CON QUERY ==========

    // Buscar recorrido activo de un conductor (STARTED o PAUSED)
    @Query("SELECT dt FROM DriverTracking dt WHERE dt.driverId = :driverId AND (dt.status = 'STARTED' OR dt.status = 'PAUSED')")
    Optional<DriverTracking> findActiveByDriverId(@Param("driverId") Long driverId);

    // Todos los recorridos activos
    @Query("SELECT dt FROM DriverTracking dt WHERE dt.status = 'STARTED' OR dt.status = 'PAUSED'")
    List<DriverTracking> findAllActiveTrips();

    // Contar por estado
    @Query("SELECT COUNT(dt) FROM DriverTracking dt WHERE dt.status = :status")
    Long countByStatus(@Param("status") TrackingStatus status);

    // ========== MÉTODOS ADICIONALES SIMPLES ==========

    // Buscar por conductor y estado específico
    List<DriverTracking> findByDriverIdAndStatusOrderByStartTimeDesc(Long driverId, TrackingStatus status);

    // Recorridos finalizados de un conductor
    @Query("SELECT dt FROM DriverTracking dt WHERE dt.driverId = :driverId AND dt.status = 'FINISHED' ORDER BY dt.startTime DESC")
    List<DriverTracking> findFinishedTripsByDriver(@Param("driverId") Long driverId);
}