package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.entities.Vehicle;
import com.example.WordDocumentsFiller.entities.enums.PaidStatus;
import com.example.WordDocumentsFiller.entities.enums.TitlesStatus;
import com.example.WordDocumentsFiller.entities.enums.VehicleStatus;
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

    @Transactional
    public void updateVehicle(Long containerId,
                              Long vehicleId,
                              PaidStatus paid,
                              TitlesStatus titles,
                              VehicleStatus status,
                              String phone,
                              String email,
                              String notes) {

        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        if (v.getContainer() == null || v.getContainer().getId() == null || !v.getContainer().getId().equals(containerId)) {
            throw new IllegalArgumentException("Vehicle " + vehicleId + " does not belong to container " + containerId);
        }

        v.setPaid(paid);
        v.setTitles(titles);
        v.setStatus(status);

        v.setPhone(phone);
        v.setEmail(email);
        v.setNotes(notes);

        vehicleRepository.save(v);
    }

    @Transactional
    public void deleteVehicle(Long containerId, Long vehicleId) {
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        if (v.getContainer() == null || v.getContainer().getId() == null || !v.getContainer().getId().equals(containerId)) {
            throw new IllegalArgumentException("Vehicle " + vehicleId + " does not belong to container " + containerId);
        }

        vehicleRepository.delete(v);
    }

    @Transactional
    public Vehicle getVehicleInContainer(Long containerId, Long vehicleId) {
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        if (v.getContainer() == null || v.getContainer().getId() == null || !v.getContainer().getId().equals(containerId)) {
            throw new IllegalArgumentException("Vehicle " + vehicleId + " does not belong to container " + containerId);
        }
        return v;
    }




}
