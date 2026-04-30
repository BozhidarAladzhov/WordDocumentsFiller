package com.example.WordDocumentsFiller.entities.enums;

public enum FleetEventType {
    LIABILITY_INSURANCE,
    CASCO_INSURANCE,
    TECHNICAL_INSPECTION,
    VIGNETTE,
    SERVICE,
    TIRE_CHANGE,
    OTHER;

    public String getDisplayName() {
        return switch (this) {
            case LIABILITY_INSURANCE -> "Гражданска";
            case CASCO_INSURANCE -> "Каско";
            case TECHNICAL_INSPECTION -> "ГТП";
            case VIGNETTE -> "Винетка";
            case SERVICE -> "Обслужване";
            case TIRE_CHANGE -> "Смяна гуми";
            case OTHER -> "Друго";
        };
    }
}
