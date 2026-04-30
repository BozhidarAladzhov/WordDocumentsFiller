package com.example.WordDocumentsFiller.entities.enums;

public enum FleetEventStatus {
    OPEN,
    COMPLETED,
    CANCELLED;

    public String getDisplayName() {
        return switch (this) {
            case OPEN -> "Отворено";
            case COMPLETED -> "Приключено";
            case CANCELLED -> "Отказано";
        };
    }
}
