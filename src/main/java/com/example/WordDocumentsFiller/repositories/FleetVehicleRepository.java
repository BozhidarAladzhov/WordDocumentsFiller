package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.FleetVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, Long> {

    List<FleetVehicle> findByActiveTrueOrderByRegistrationNumberAsc();

    Optional<FleetVehicle> findByRegistrationNumber(String registrationNumber);
}
