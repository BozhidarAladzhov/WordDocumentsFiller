package com.example.WordDocumentsFiller.repositories;

import com.example.WordDocumentsFiller.entities.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByContainerIdOrderByIdDesc(Long containerId);


}
