package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.FleetMaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FleetMaintenanceRecordRepository extends JpaRepository<FleetMaintenanceRecord, Long> {

    List<FleetMaintenanceRecord> findByVehicleIdOrderByServiceDateDescIdDesc(Long vehicleId);
}
