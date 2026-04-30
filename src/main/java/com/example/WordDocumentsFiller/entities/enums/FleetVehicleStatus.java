package com.example.WordDocumentsFiller.entities.enums;

public enum FleetVehicleStatus {
    ACTIVE,
    IN_SERVICE,
    OUT_OF_SERVICE;

    public String getDisplayName() {
        return switch (this) {
            case ACTIVE -> "Активен";
            case IN_SERVICE -> "В сервиз";
            case OUT_OF_SERVICE -> "Извън експлоатация";
        };
    }
}
