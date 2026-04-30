package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.FleetDashboardDto;
import com.example.WordDocumentsFiller.entities.FleetEvent;
import com.example.WordDocumentsFiller.entities.FleetVehicle;
import com.example.WordDocumentsFiller.entities.enums.FleetEventStatus;
import com.example.WordDocumentsFiller.entities.enums.FleetEventType;
import com.example.WordDocumentsFiller.repositories.FleetEventRepository;
import com.example.WordDocumentsFiller.repositories.FleetVehicleRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetVehicleServiceTest {

    @Test
    void shouldBuildDashboardWithOverdueAndUpcomingDeadlines() {
        FleetVehicle overdueVehicle = new FleetVehicle();
        overdueVehicle.setActive(true);
        overdueVehicle.setRegistrationNumber("CB1234AB");
        overdueVehicle.setMakeModel("Ford Transit");
        overdueVehicle.setLiabilityInsuranceExpiresOn(LocalDate.of(2026, 4, 10));

        FleetVehicle upcomingVehicle = new FleetVehicle();
        upcomingVehicle.setActive(true);
        upcomingVehicle.setRegistrationNumber("CB5678CD");
        upcomingVehicle.setMakeModel("Renault Master");
        upcomingVehicle.setTechnicalInspectionExpiresOn(LocalDate.of(2026, 4, 25));
        upcomingVehicle.setVignetteExpiresOn(LocalDate.of(2026, 5, 15));

        FleetEvent customEvent = new FleetEvent();
        customEvent.setVehicle(upcomingVehicle);
        customEvent.setType(FleetEventType.OTHER);
        customEvent.setStatus(FleetEventStatus.OPEN);
        customEvent.setTitle("Договор с подизпълнител");
        customEvent.setDueOn(LocalDate.of(2026, 4, 28));

        FleetVehicleRepository repository = mock(FleetVehicleRepository.class);
        FleetEventRepository eventRepository = mock(FleetEventRepository.class);
        when(repository.findByActiveTrueOrderByRegistrationNumberAsc())
                .thenReturn(List.of(overdueVehicle, upcomingVehicle));
        when(eventRepository.findAll())
                .thenReturn(List.of(customEvent));

        FleetVehicleService service = new FleetVehicleService(repository, eventRepository);

        FleetDashboardDto dashboard = service.buildDashboard(LocalDate.of(2026, 4, 21));

        assertEquals(2, dashboard.activeVehicleCount());
        assertEquals(1, dashboard.overdueItems().size());
        assertEquals("Гражданска", dashboard.overdueItems().get(0).deadlineType());
        assertEquals(-11, dashboard.overdueItems().get(0).daysUntilDue());

        assertEquals(3, dashboard.upcomingItems().size());
        assertEquals("ГТП", dashboard.upcomingItems().get(0).deadlineType());
        assertEquals(4, dashboard.upcomingItems().get(0).daysUntilDue());
        assertEquals("Договор с подизпълнител", dashboard.upcomingItems().get(1).deadlineType());
        assertEquals(7, dashboard.upcomingItems().get(1).daysUntilDue());
        assertEquals("Винетка", dashboard.upcomingItems().get(2).deadlineType());
        assertEquals(24, dashboard.upcomingItems().get(2).daysUntilDue());
    }
}
