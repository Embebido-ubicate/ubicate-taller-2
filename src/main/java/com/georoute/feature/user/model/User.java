package com.georoute.feature.user.model;

import com.georoute.feature.user.enums.Role;
import com.georoute.feature.user.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === DATOS BÁSICOS ===
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    @Email
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 15)
    private String phone;

    // === TIPO Y ESTADO ===
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.PASSENGER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // === GEOLOCALIZACIÓN ===
    private Double latitude;
    private Double longitude;

    @Builder.Default
    private Boolean shareLocation = true;

    // === EMPRESA (solo para admins de empresa) ===
    private Long companyId;

    // === FECHAS ===
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLogin;

    // === UserDetails Implementation ===
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    // === MÉTODOS AUXILIARES ===
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isPassenger() {
        return role == Role.PASSENGER;
    }

    public boolean isDriver() {
        return role == Role.DRIVER;
    }

    public boolean isCompanyAdmin() {
        return role == Role.COMPANY_ADMIN;
    }

    public boolean isSystemAdmin() {
        return role == Role.SYSTEM_ADMIN;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}