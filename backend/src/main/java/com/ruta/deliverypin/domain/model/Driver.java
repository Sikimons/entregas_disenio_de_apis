package com.ruta.deliverypin.domain.model;

import java.time.Instant;

/**
 * Usuario del middleware: puede ser un conductor (usa la app de entregas)
 * o un administrador (gestiona conductores y consulta el historial).
 * Es un objeto de dominio puro, sin anotaciones de persistencia ni de framework.
 */
public class Driver {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final Role role;
    private final boolean active;
    private final Instant createdAt;

    public Driver(Long id, String username, String passwordHash, String fullName, Role role, boolean active, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static Driver createNew(String username, String encodedPassword, String fullName, Role role) {
        return new Driver(null, username, encodedPassword, fullName, role, true, Instant.now());
    }

    public Driver withUpdatedProfile(String fullName, boolean active, String passwordHash) {
        return new Driver(this.id, this.username, passwordHash, fullName, this.role, active, this.createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
