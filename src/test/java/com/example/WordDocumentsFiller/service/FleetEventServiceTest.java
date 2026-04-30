package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.FleetEvent;
import com.example.WordDocumentsFiller.entities.FleetVehicle;
import com.example.WordDocumentsFiller.entities.enums.FleetEventStatus;
import com.example.WordDocumentsFiller.entities.enums.FleetEventType;
import com.example.WordDocumentsFiller.repositories.FleetEventRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FleetEventServiceTest {

    @Test
    void shouldSyncKnownEventTypesToVehicleSnapshot() {
        FleetVehicle vehicle = new FleetVehicle();

        FleetEventRepository eventRepository = mock(FleetEventRepository.class);
        FleetVehicleService vehicleService = mock(FleetVehicleService.class);
        when(vehicleService.getById(42L)).thenReturn(vehicle);
        when(eventRepository.save(any(FleetEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FleetEventService service = new FleetEventService(eventRepository, vehicleService);

        FleetEvent event = new FleetEvent();
        event.setType(FleetEventType.SERVICE);
        event.setStatus(FleetEventStatus.OPEN);
        event.setDueOn(LocalDate.of(2026, 5, 20));
        event.setCompletedOn(LocalDate.of(2026, 4, 18));

        service.create(42L, event);

        assertEquals(LocalDate.of(2026, 5, 20), vehicle.getServiceDueOn());
        assertEquals(LocalDate.of(2026, 4, 18), vehicle.getServiceLastDoneOn());
        assertEquals(FleetEventStatus.COMPLETED, event.getStatus());
        assertEquals("Обслужване", event.getTitle());
    }

    @Test
    void shouldDefaultDueDateFromScheduledDateForOpenEvents() {
        FleetVehicle vehicle = new FleetVehicle();

        FleetEventRepository eventRepository = mock(FleetEventRepository.class);
        FleetVehicleService vehicleService = mock(FleetVehicleService.class);
        when(vehicleService.getById(42L)).thenReturn(vehicle);
        when(eventRepository.save(any(FleetEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FleetEventService service = new FleetEventService(eventRepository, vehicleService);

        FleetEvent event = new FleetEvent();
        event.setScheduledOn(LocalDate.of(2026, 5, 20));

        service.create(42L, event);

        assertEquals(LocalDate.of(2026, 5, 20), event.getDueOn());
        assertEquals(FleetEventStatus.OPEN, event.getStatus());
        assertNull(event.getCompletedOn());
    }

    @Test
    void shouldCompleteOwnedEvent() throws Exception {
        FleetVehicle vehicle = new FleetVehicle();
        Field idField = FleetVehicle.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(vehicle, 42L);

        FleetEvent event = new FleetEvent();
        event.setVehicle(vehicle);
        event.setStatus(FleetEventStatus.OPEN);

        FleetEventRepository eventRepository = mock(FleetEventRepository.class);
        FleetVehicleService vehicleService = mock(FleetVehicleService.class);
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(FleetEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FleetEventService service = new FleetEventService(eventRepository, vehicleService);

        service.complete(42L, 7L);

        assertEquals(FleetEventStatus.COMPLETED, event.getStatus());
        assertEquals(LocalDate.now(), event.getCompletedOn());
        verify(eventRepository).save(event);
    }
}
