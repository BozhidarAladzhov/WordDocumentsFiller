package com.example.WordDocumentsFiller.dto;

import java.util.List;

public record FleetDashboardDto(
        long activeVehicleCount,
        List<FleetReminderItem> overdueItems,
        List<FleetReminderItem> upcomingItems
) {
}
