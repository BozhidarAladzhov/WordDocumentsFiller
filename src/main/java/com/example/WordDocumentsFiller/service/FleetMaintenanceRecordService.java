package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.FleetMaintenanceRecord;
import com.example.WordDocumentsFiller.entities.FleetVehicle;
import com.example.WordDocumentsFiller.repositories.FleetMaintenanceRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FleetMaintenanceRecordService {

    private final FleetMaintenanceRecordRepository fleetMaintenanceRecordRepository;
    private final FleetVehicleService fleetVehicleService;

    public FleetMaintenanceRecordService(FleetMaintenanceRecordRepository fleetMaintenanceRecordRepository,
                                         FleetVehicleService fleetVehicleService) {
        this.fleetMaintenanceRecordRepository = fleetMaintenanceRecordRepository;
        this.fleetVehicleService = fleetVehicleService;
    }

    @Transactional
    public List<FleetMaintenanceRecord> getByVehicleId(Long vehicleId) {
        return fleetMaintenanceRecordRepository.findByVehicleIdOrderByServiceDateDescIdDesc(vehicleId);
    }

    @Transactional
    public FleetMaintenanceRecord create(Long vehicleId, FleetMaintenanceRecord record) {
        FleetVehicle vehicle = fleetVehicleService.getById(vehicleId);
        record.setVehicle(vehicle);
        normalizeRecord(record);
        return fleetMaintenanceRecordRepository.save(record);
    }

    @Transactional
    public FleetMaintenanceRecord update(Long vehicleId, Long recordId, FleetMaintenanceRecord source) {
        FleetMaintenanceRecord target = getOwnedRecord(vehicleId, recordId);
        target.setServiceDate(source.getServiceDate());
        target.setMileageKm(source.getMileageKm());
        target.setDescription(source.getDescription());
        target.setVendorName(source.getVendorName());
        target.setNote(source.getNote());
        normalizeRecord(target);
        return fleetMaintenanceRecordRepository.save(target);
    }

    @Transactional
    public void delete(Long vehicleId, Long recordId) {
        FleetMaintenanceRecord target = getOwnedRecord(vehicleId, recordId);
        fleetMaintenanceRecordRepository.delete(target);
    }

    private FleetMaintenanceRecord getOwnedRecord(Long vehicleId, Long recordId) {
        FleetMaintenanceRecord record = fleetMaintenanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Fleet maintenance record not found: " + recordId));
        if (record.getVehicle() == null || record.getVehicle().getId() == null || !record.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Fleet maintenance record does not belong to vehicle: " + recordId);
        }
        return record;
    }

    private void normalizeRecord(FleetMaintenanceRecord record) {
        record.setDescription(normalize(record.getDescription()));
        record.setVendorName(normalize(record.getVendorName()));
        record.setNote(normalize(record.getNote()));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
