package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.CanadianVehicle;
import com.example.WordDocumentsFiller.repositories.CanadianVehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CanadianVehicleService {

    private final CanadianVehicleRepository canadianVehicleRepository;

    public CanadianVehicleService(CanadianVehicleRepository canadianVehicleRepository) {
        this.canadianVehicleRepository = canadianVehicleRepository;
    }

    @Transactional
    public List<CanadianVehicle> getAll() {
        return canadianVehicleRepository.findAllForOverview();
    }

    @Transactional
    public CanadianVehicle getById(Long id) {
        return canadianVehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Canadian vehicle not found: " + id));
    }

    @Transactional
    public CanadianVehicle create(CanadianVehicle canadianVehicle) {
        return canadianVehicleRepository.save(canadianVehicle);
    }

    @Transactional
    public CanadianVehicle update(Long id, CanadianVehicle form) {
        CanadianVehicle vehicle = getById(id);
        vehicle.setClientName(form.getClientName());
        vehicle.setVehicleName(form.getVehicleName());
        vehicle.setVin(form.getVin());
        vehicle.setContainerNo(form.getContainerNo());
        vehicle.setTrackingLink(form.getTrackingLink());
        vehicle.setEta(form.getEta());
        vehicle.setTalonStatus(form.getTalonStatus());
        vehicle.setInvoiceNumber(form.getInvoiceNumber());
        vehicle.setAmount(form.getAmount());
        vehicle.setPaymentDate(form.getPaymentDate());
        vehicle.setNotes(form.getNotes());
        return canadianVehicleRepository.save(vehicle);
    }

    @Transactional
    public CanadianVehicle markPaymentOrdersSent(Long id, String sentTo) {
        CanadianVehicle vehicle = getById(id);
        vehicle.setPaymentOrdersSentAt(LocalDateTime.now());
        vehicle.setPaymentOrdersSentTo(sentTo);
        return canadianVehicleRepository.save(vehicle);
    }

    @Transactional
    public void delete(Long id) {
        canadianVehicleRepository.delete(getById(id));
    }
}
