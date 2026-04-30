package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.FleetEvent;
import com.example.WordDocumentsFiller.entities.FleetVehicle;
import com.example.WordDocumentsFiller.entities.enums.FleetEventStatus;
import com.example.WordDocumentsFiller.entities.enums.FleetEventType;
import com.example.WordDocumentsFiller.repositories.FleetEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FleetEventService {

    private final FleetEventRepository fleetEventRepository;
    private final FleetVehicleService fleetVehicleService;

    public FleetEventService(FleetEventRepository fleetEventRepository,
                             FleetVehicleService fleetVehicleService) {
        this.fleetEventRepository = fleetEventRepository;
        this.fleetVehicleService = fleetVehicleService;
    }

    @Transactional
    public List<FleetEvent> getByVehicleId(Long vehicleId) {
        return fleetEventRepository.findByVehicleIdOrderByDueOnAscScheduledOnAscIdAsc(vehicleId);
    }

    @Transactional
    public List<FleetEvent> getOpenByVehicleId(Long vehicleId) {
        return fleetEventRepository.findByVehicleIdAndStatusOrderByScheduledOnAscDueOnAscIdAsc(vehicleId, FleetEventStatus.OPEN);
    }

    @Transactional
    public FleetEvent create(Long vehicleId, FleetEvent event) {
        FleetVehicle vehicle = fleetVehicleService.getById(vehicleId);

        event.setVehicle(vehicle);
        normalizeEvent(event);
        applyDefaultTitle(event);
        syncCompletedState(event);

        FleetEvent saved = fleetEventRepository.save(event);
        syncVehicleSnapshot(vehicle, saved);
        return saved;
    }

    private void normalizeEvent(FleetEvent event) {
        event.setTitle(normalize(event.getTitle()));
        event.setVendor(normalize(event.getVendor()));
        event.setContactPhone(normalize(event.getContactPhone()));
        event.setLocation(normalize(event.getLocation()));
        event.setNotes(normalize(event.getNotes()));

        if (event.getType() == null) {
            event.setType(FleetEventType.OTHER);
        }
        if (event.getStatus() == null) {
            event.setStatus(FleetEventStatus.OPEN);
        }
        if (event.getScheduledOn() != null && event.getDueOn() == null) {
            event.setDueOn(event.getScheduledOn());
        }
        if (event.getNotifyDaysBefore() == null) {
            event.setNotifyDaysBefore(30);
        }
    }

    private void applyDefaultTitle(FleetEvent event) {
        if (event.getTitle() != null) {
            return;
        }

        event.setTitle(switch (event.getType()) {
            case LIABILITY_INSURANCE -> "Гражданска";
            case CASCO_INSURANCE -> "Каско";
            case TECHNICAL_INSPECTION -> "ГТП";
            case VIGNETTE -> "Винетка";
            case SERVICE -> "Обслужване";
            case TIRE_CHANGE -> "Смяна гуми";
            case OTHER -> "Събитие";
        });
    }

    private void syncCompletedState(FleetEvent event) {
        if (event.getCompletedOn() != null && event.getStatus() == FleetEventStatus.OPEN) {
            event.setStatus(FleetEventStatus.COMPLETED);
        }
    }

    @Transactional
    public void complete(Long vehicleId, Long eventId) {
        FleetEvent event = getOwnedEvent(vehicleId, eventId);
        event.setStatus(FleetEventStatus.COMPLETED);
        event.setCompletedOn(LocalDate.now());
        fleetEventRepository.save(event);
    }

    private FleetEvent getOwnedEvent(Long vehicleId, Long eventId) {
        FleetEvent event = fleetEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Fleet event not found: " + eventId));
        if (event.getVehicle() == null || event.getVehicle().getId() == null || !event.getVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Fleet event does not belong to vehicle: " + eventId);
        }
        return event;
    }

    private void syncVehicleSnapshot(FleetVehicle vehicle, FleetEvent event) {
        switch (event.getType()) {
            case LIABILITY_INSURANCE -> vehicle.setLiabilityInsuranceExpiresOn(event.getDueOn());
            case CASCO_INSURANCE -> vehicle.setCascoInsuranceExpiresOn(event.getDueOn());
            case TECHNICAL_INSPECTION -> vehicle.setTechnicalInspectionExpiresOn(event.getDueOn());
            case VIGNETTE -> vehicle.setVignetteExpiresOn(event.getDueOn());
            case SERVICE -> {
                vehicle.setServiceDueOn(event.getDueOn());
                if (event.getCompletedOn() != null) {
                    vehicle.setServiceLastDoneOn(event.getCompletedOn());
                }
            }
            case TIRE_CHANGE -> vehicle.setTireChangeDueOn(event.getDueOn());
            case OTHER -> {
            }
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
