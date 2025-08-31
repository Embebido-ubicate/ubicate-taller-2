package com.georoute.feature.tracking.enums;

public enum TrackingStatus {
    STARTED("Recorrido iniciado"),
    PAUSED("Recorrido pausado"),
    FINISHED("Recorrido finalizado"),
    CANCELLED("Recorrido cancelado");

    private final String description;

    TrackingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return switch (this) {
            case STARTED -> "#4CAF50";   // Verde
            case PAUSED -> "#FF9800";    // Naranja
            case FINISHED -> "#2196F3";  // Azul
            case CANCELLED -> "#F44336"; // Rojo
        };
    }
}