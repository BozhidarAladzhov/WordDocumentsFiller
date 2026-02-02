package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.entities.Vehicle;
import com.example.WordDocumentsFiller.repositories.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional()
    public List<Vehicle> getByContainerId(Long containerId) {
        return vehicleRepository.findByContainerIdOrderByIdDesc(containerId);
    }

    @Transactional
    public Vehicle addToContainer(Container container, Vehicle vehicle) {
        vehicle.setContainer(container);
        return vehicleRepository.save(vehicle);
    }



}
