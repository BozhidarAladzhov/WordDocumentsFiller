package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.FleetMaintenanceRecord;
import com.example.WordDocumentsFiller.entities.FleetVehicle;
import com.example.WordDocumentsFiller.repositories.FleetMaintenanceRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetMaintenanceRecordServiceTest {

    @Test
    void shouldCreateMaintenanceRecordForVehicle() {
        FleetVehicle vehicle = new FleetVehicle();
        vehicle.setRegistrationNumber("CA1234AB");
        ReflectionTestUtils.setField(vehicle, "id", 7L);
        FleetMaintenanceRecordRepository repository = mock(FleetMaintenanceRecordRepository.class);
        FleetVehicleService vehicleService = mock(FleetVehicleService.class);

        when(vehicleService.getById(7L)).thenReturn(vehicle);
        when(repository.save(any(FleetMaintenanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FleetMaintenanceRecordService service = new FleetMaintenanceRecordService(repository, vehicleService);

        FleetMaintenanceRecord record = new FleetMaintenanceRecord();
        record.setServiceDate(LocalDate.of(2026, 4, 21));
        record.setMileageKm(125000);
        record.setDescription("  Смяна на масла и филтри  ");
        record.setVendorName("  Auto сервиз  ");
        record.setNote("  направена проверка  ");

        FleetMaintenanceRecord saved = service.create(7L, record);

        assertSame(vehicle, saved.getVehicle());
        assertEquals("Смяна на масла и филтри", saved.getDescription());
        assertEquals("Auto сервиз", saved.getVendorName());
        assertEquals("направена проверка", saved.getNote());
    }

    @Test
    void shouldUpdateAndDeleteOwnedMaintenanceRecord() {
        FleetVehicle vehicle = new FleetVehicle();
        vehicle.setRegistrationNumber("CA1234AB");
        ReflectionTestUtils.setField(vehicle, "id", 7L);

        FleetMaintenanceRecord existing = new FleetMaintenanceRecord();
        existing.setVehicle(vehicle);
        existing.setServiceDate(LocalDate.of(2026, 4, 1));
        existing.setMileageKm(100000);
        existing.setDescription("Старо");

        FleetMaintenanceRecordRepository repository = mock(FleetMaintenanceRecordRepository.class);
        FleetVehicleService vehicleService = mock(FleetVehicleService.class);
        when(repository.findById(3L)).thenReturn(java.util.Optional.of(existing));
        when(repository.save(any(FleetMaintenanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FleetMaintenanceRecordService service = new FleetMaintenanceRecordService(repository, vehicleService);

        FleetMaintenanceRecord source = new FleetMaintenanceRecord();
        source.setServiceDate(LocalDate.of(2026, 4, 22));
        source.setMileageKm(101500);
        source.setDescription("  Ново обслужване  ");
        source.setVendorName("  Сервиз  ");
        source.setNote("  бележка  ");

        FleetMaintenanceRecord updated = service.update(7L, 3L, source);

        assertEquals(LocalDate.of(2026, 4, 22), updated.getServiceDate());
        assertEquals(101500, updated.getMileageKm());
        assertEquals("Ново обслужване", updated.getDescription());
        assertEquals("Сервиз", updated.getVendorName());
        assertEquals("бележка", updated.getNote());

        service.delete(7L, 3L);

        verify(repository).delete(eq(existing));
    }
}
