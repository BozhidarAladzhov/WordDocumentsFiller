package com.example.WordDocumentsFiller.dto;

import com.example.WordDocumentsFiller.entities.FleetVehicle;

import java.time.LocalDate;

public record FleetVehicleListItemDto(
        FleetVehicle vehicle,
        String nextDeadlineLabel,
        LocalDate nextDeadlineDate,
        boolean overdue
) {
}
