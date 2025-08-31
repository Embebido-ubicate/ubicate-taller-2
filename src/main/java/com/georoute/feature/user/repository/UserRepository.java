package com.georoute.feature.user.repository;

import com.georoute.feature.user.model.User;
import com.georoute.feature.user.enums.Role;
import com.georoute.feature.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Búsquedas básicas
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Mantener compatibilidad con código existente
    default Optional<User> findByUsername(String username) {
        return findByEmail(username);
    }

    default Optional<User> findByCorreo(String correo) {
        return findByEmail(correo);
    }

    // Búsquedas por rol y estado
    List<User> findByRole(Role role);
    List<User> findByStatus(UserStatus status);
    List<User> findByRoleAndStatus(Role role, UserStatus status);

    // Búsquedas por empresa
    List<User> findByCompanyId(Long companyId);
    List<User> findByCompanyIdAndRole(Long companyId, Role role);
    List<User> findByCompanyIdAndStatus(Long companyId, UserStatus status);

    // Búsquedas de geolocalización
    @Query("SELECT u FROM User u WHERE u.shareLocation = true AND u.latitude IS NOT NULL AND u.longitude IS NOT NULL")
    List<User> findUsersWithLocation();

    @Query("SELECT u FROM User u WHERE u.shareLocation = true AND u.latitude IS NOT NULL AND u.longitude IS NOT NULL AND u.status = 'ACTIVE'")
    List<User> findActiveUsersWithLocation();

    // Búsquedas por rango de coordenadas (para encontrar usuarios cerca)
    @Query("SELECT u FROM User u WHERE u.shareLocation = true AND u.latitude BETWEEN :minLat AND :maxLat AND u.longitude BETWEEN :minLng AND :maxLng AND u.status = 'ACTIVE'")
    List<User> findUsersInBounds(@Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
                                 @Param("minLng") Double minLng, @Param("maxLng") Double maxLng);

    // Estadísticas
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    Long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.companyId = :companyId AND u.status = 'ACTIVE'")
    Long countActiveUsersByCompany(@Param("companyId") Long companyId);

    // Usuarios activos recientes
    @Query("SELECT u FROM User u WHERE u.lastLogin > :since AND u.status = 'ACTIVE' ORDER BY u.lastLogin DESC")
    List<User> findRecentActiveUsers(@Param("since") LocalDateTime since);
}