package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.FleetEvent;
import com.example.WordDocumentsFiller.entities.enums.FleetEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FleetEventRepository extends JpaRepository<FleetEvent, Long> {

    List<FleetEvent> findByVehicleIdOrderByDueOnAscScheduledOnAscIdAsc(Long vehicleId);

    List<FleetEvent> findByVehicleIdAndStatusOrderByDueOnAscScheduledOnAscIdAsc(Long vehicleId, FleetEventStatus status);

    List<FleetEvent> findByVehicleIdAndStatusOrderByScheduledOnAscDueOnAscIdAsc(Long vehicleId, FleetEventStatus status);
}
