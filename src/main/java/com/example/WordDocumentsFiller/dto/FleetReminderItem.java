package com.example.WordDocumentsFiller.dto;

import java.time.LocalDate;

public record FleetReminderItem(
        Long vehicleId,
        String registrationNumber,
        String makeModel,
        String deadlineType,
        LocalDate dueDate,
        long daysUntilDue,
        boolean overdue
) {
}
