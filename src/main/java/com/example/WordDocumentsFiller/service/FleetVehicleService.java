package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.FleetDashboardDto;
import com.example.WordDocumentsFiller.dto.FleetReminderItem;
import com.example.WordDocumentsFiller.dto.FleetVehicleListItemDto;
import com.example.WordDocumentsFiller.entities.FleetEvent;
import com.example.WordDocumentsFiller.entities.FleetVehicle;
import com.example.WordDocumentsFiller.entities.enums.FleetEventStatus;
import com.example.WordDocumentsFiller.entities.enums.FleetEventType;
import com.example.WordDocumentsFiller.entities.enums.FleetVehicleStatus;
import com.example.WordDocumentsFiller.entities.enums.InsurancePaymentPlan;
import com.example.WordDocumentsFiller.repositories.FleetEventRepository;
import com.example.WordDocumentsFiller.repositories.FleetVehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FleetVehicleService {

    private final FleetVehicleRepository fleetVehicleRepository;
    private final FleetEventRepository fleetEventRepository;

    public FleetVehicleService(FleetVehicleRepository fleetVehicleRepository,
                               FleetEventRepository fleetEventRepository) {
        this.fleetVehicleRepository = fleetVehicleRepository;
        this.fleetEventRepository = fleetEventRepository;
    }

    @Transactional
    public List<FleetVehicle> getActiveVehicles() {
        return fleetVehicleRepository.findByActiveTrueOrderByRegistrationNumberAsc();
    }

    @Transactional
    public List<FleetVehicleListItemDto> getVehicleListItems(LocalDate today) {
        List<FleetVehicleListItemDto> items = new ArrayList<>();
        for (FleetVehicle vehicle : getActiveVehicles()) {
            FleetReminderItem nextReminder = collectVehicleReminders(vehicle, today).stream()
                    .min(Comparator.comparing(FleetReminderItem::dueDate))
                    .orElse(null);

            items.add(new FleetVehicleListItemDto(
                    vehicle,
                    nextReminder != null ? nextReminder.deadlineType() : null,
                    nextReminder != null ? nextReminder.dueDate() : null,
                    nextReminder != null && nextReminder.overdue()
            ));
        }
        return items;
    }

    @Transactional
    public FleetVehicle getById(Long id) {
        return fleetVehicleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fleet vehicle not found: " + id));
    }

    @Transactional
    public FleetVehicle create(FleetVehicle fleetVehicle) {
        normalizeEntity(fleetVehicle);
        if (fleetVehicle.getStatus() == null) {
            fleetVehicle.setStatus(FleetVehicleStatus.ACTIVE);
        }
        if (fleetVehicle.getLiabilityPaymentPlan() == null) {
            fleetVehicle.setLiabilityPaymentPlan(InsurancePaymentPlan.FULL);
        }
        if (fleetVehicle.getCascoPaymentPlan() == null) {
            fleetVehicle.setCascoPaymentPlan(InsurancePaymentPlan.FULL);
        }
        return fleetVehicleRepository.save(fleetVehicle);
    }

    @Transactional
    public FleetVehicle update(Long id, FleetVehicle source) {
        FleetVehicle target = getById(id);
        applyCoreFields(target, source);
        applyDetailFields(target, source);
        return fleetVehicleRepository.save(target);
    }

    @Transactional
    public FleetVehicle updateCore(Long id, FleetVehicle source) {
        FleetVehicle target = getById(id);
        applyCoreFields(target, source);
        return fleetVehicleRepository.save(target);
    }

    @Transactional
    public FleetVehicle updateDetails(Long id, FleetVehicle source) {
        FleetVehicle target = getById(id);
        applyDetailFields(target, source);
        return fleetVehicleRepository.save(target);
    }

    private void applyCoreFields(FleetVehicle target, FleetVehicle source) {
        target.setRegistrationNumber(normalize(source.getRegistrationNumber()));
        target.setMakeModel(normalize(source.getMakeModel()));
        target.setVin(normalize(source.getVin()));
        target.setStatus(source.getStatus());
        target.setVehicleUserName(normalize(source.getVehicleUserName()));
        target.setVehicleUserEmail(normalize(source.getVehicleUserEmail()));
        target.setFleetManagerName(normalize(source.getFleetManagerName()));
        target.setFleetManagerEmail(normalize(source.getFleetManagerEmail()));
    }

    private void applyDetailFields(FleetVehicle target, FleetVehicle source) {
        target.setLiabilityInsuranceExpiresOn(source.getLiabilityInsuranceExpiresOn());
        target.setLiabilityInsurer(normalize(source.getLiabilityInsurer()));
        target.setLiabilityPrice(source.getLiabilityPrice());
        target.setLiabilityPaymentPlan(source.getLiabilityPaymentPlan());
        target.setLiabilityInstallment1DueOn(source.getLiabilityInstallment1DueOn());
        target.setLiabilityInstallment1Price(source.getLiabilityInstallment1Price());
        target.setLiabilityInstallment1Paid(source.isLiabilityInstallment1Paid());
        target.setLiabilityInstallment2DueOn(source.getLiabilityInstallment2DueOn());
        target.setLiabilityInstallment2Price(source.getLiabilityInstallment2Price());
        target.setLiabilityInstallment2Paid(source.isLiabilityInstallment2Paid());
        target.setLiabilityInstallment3DueOn(source.getLiabilityInstallment3DueOn());
        target.setLiabilityInstallment3Price(source.getLiabilityInstallment3Price());
        target.setLiabilityInstallment3Paid(source.isLiabilityInstallment3Paid());
        target.setLiabilityInstallment4DueOn(source.getLiabilityInstallment4DueOn());
        target.setLiabilityInstallment4Price(source.getLiabilityInstallment4Price());
        target.setLiabilityInstallment4Paid(source.isLiabilityInstallment4Paid());

        target.setCascoInsuranceExpiresOn(source.getCascoInsuranceExpiresOn());
        target.setCascoInsurer(normalize(source.getCascoInsurer()));
        target.setCascoPrice(source.getCascoPrice());
        target.setCascoPaymentPlan(source.getCascoPaymentPlan());
        target.setCascoInstallment1DueOn(source.getCascoInstallment1DueOn());
        target.setCascoInstallment1Price(source.getCascoInstallment1Price());
        target.setCascoInstallment1Paid(source.isCascoInstallment1Paid());
        target.setCascoInstallment2DueOn(source.getCascoInstallment2DueOn());
        target.setCascoInstallment2Price(source.getCascoInstallment2Price());
        target.setCascoInstallment2Paid(source.isCascoInstallment2Paid());
        target.setCascoInstallment3DueOn(source.getCascoInstallment3DueOn());
        target.setCascoInstallment3Price(source.getCascoInstallment3Price());
        target.setCascoInstallment3Paid(source.isCascoInstallment3Paid());
        target.setCascoInstallment4DueOn(source.getCascoInstallment4DueOn());
        target.setCascoInstallment4Price(source.getCascoInstallment4Price());
        target.setCascoInstallment4Paid(source.isCascoInstallment4Paid());

        target.setTechnicalInspectionExpiresOn(source.getTechnicalInspectionExpiresOn());
        target.setTechnicalInspectionPrice(source.getTechnicalInspectionPrice());
        target.setVignetteExpiresOn(source.getVignetteExpiresOn());
        target.setVignettePrice(source.getVignettePrice());
        target.setSummerTiresActive(source.isSummerTiresActive());
        target.setSummerTiresBrand(normalize(source.getSummerTiresBrand()));
        target.setSummerTiresSize(normalize(source.getSummerTiresSize()));
        target.setSummerTiresDot(normalize(source.getSummerTiresDot()));
        target.setWinterTiresActive(source.isWinterTiresActive());
        target.setWinterTiresBrand(normalize(source.getWinterTiresBrand()));
        target.setWinterTiresSize(normalize(source.getWinterTiresSize()));
        target.setWinterTiresDot(normalize(source.getWinterTiresDot()));
        target.setAllSeasonTiresActive(source.isAllSeasonTiresActive());
        target.setAllSeasonTiresBrand(normalize(source.getAllSeasonTiresBrand()));
        target.setAllSeasonTiresSize(normalize(source.getAllSeasonTiresSize()));
        target.setAllSeasonTiresDot(normalize(source.getAllSeasonTiresDot()));
        target.setNotes(normalize(source.getNotes()));
        target.setActive(source.isActive());
    }

    @Transactional
    public FleetDashboardDto buildDashboard() {
        return buildDashboard(LocalDate.now());
    }

    @Transactional
    public FleetDashboardDto buildDashboard(LocalDate today) {
        List<FleetVehicle> vehicles = getActiveVehicles();
        List<FleetReminderItem> overdue = new ArrayList<>();
        List<FleetReminderItem> upcoming = new ArrayList<>();
        Set<String> reminderKeys = new HashSet<>();

        for (FleetVehicle vehicle : vehicles) {
            addSnapshotReminders(overdue, upcoming, reminderKeys, today, vehicle);
        }

        List<FleetEvent> openEvents = fleetEventRepository.findAll().stream()
                .filter(event -> event.getVehicle() != null && event.getVehicle().isActive())
                .filter(event -> event.getStatus() == FleetEventStatus.OPEN)
                .toList();

        for (FleetEvent event : openEvents) {
            addReminder(overdue, upcoming, reminderKeys, today, event.getVehicle(), reminderLabel(event), event.getDueOn());
        }

        overdue.sort(Comparator.comparing(FleetReminderItem::dueDate)
                .thenComparing(FleetReminderItem::registrationNumber));
        upcoming.sort(Comparator.comparing(FleetReminderItem::dueDate)
                .thenComparing(FleetReminderItem::registrationNumber));

        return new FleetDashboardDto(vehicles.size(), overdue, upcoming);
    }

    private List<FleetReminderItem> collectVehicleReminders(FleetVehicle vehicle, LocalDate today) {
        List<FleetReminderItem> overdue = new ArrayList<>();
        List<FleetReminderItem> upcoming = new ArrayList<>();
        Set<String> reminderKeys = new HashSet<>();

        addSnapshotReminders(overdue, upcoming, reminderKeys, today, vehicle);

        List<FleetEvent> openEvents = fleetEventRepository.findAll().stream()
                .filter(event -> event.getVehicle() != null && event.getVehicle().getId() != null)
                .filter(event -> vehicle.getId() != null && vehicle.getId().equals(event.getVehicle().getId()))
                .filter(event -> event.getStatus() == FleetEventStatus.OPEN)
                .toList();

        for (FleetEvent event : openEvents) {
            addReminder(overdue, upcoming, reminderKeys, today, vehicle, reminderLabel(event), event.getDueOn());
        }

        List<FleetReminderItem> all = new ArrayList<>(overdue);
        all.addAll(upcoming);
        return all;
    }

    private void addSnapshotReminders(List<FleetReminderItem> overdue,
                                      List<FleetReminderItem> upcoming,
                                      Set<String> reminderKeys,
                                      LocalDate today,
                                      FleetVehicle vehicle) {
        addReminder(overdue, upcoming, reminderKeys, today, vehicle, "Гражданска", vehicle.getLiabilityInsuranceExpiresOn());
        addReminder(overdue, upcoming, reminderKeys, today, vehicle, "Каско", vehicle.getCascoInsuranceExpiresOn());
        addReminder(overdue, upcoming, reminderKeys, today, vehicle, "ГТП", vehicle.getTechnicalInspectionExpiresOn());
        addReminder(overdue, upcoming, reminderKeys, today, vehicle, "Винетка", vehicle.getVignetteExpiresOn());
    }

    private void addReminder(List<FleetReminderItem> overdue,
                             List<FleetReminderItem> upcoming,
                             Set<String> reminderKeys,
                             LocalDate today,
                             FleetVehicle vehicle,
                             String deadlineType,
                             LocalDate dueDate) {
        if (dueDate == null) {
            return;
        }

        String reminderKey = vehicle.getId() + "|" + deadlineType + "|" + dueDate;
        if (!reminderKeys.add(reminderKey)) {
            return;
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
        FleetReminderItem item = new FleetReminderItem(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getMakeModel(),
                deadlineType,
                dueDate,
                daysUntilDue,
                daysUntilDue < 0
        );

        if (daysUntilDue < 0) {
            overdue.add(item);
        } else if (daysUntilDue <= 30) {
            upcoming.add(item);
        }
    }

    private String reminderLabel(FleetEvent event) {
        if (event.getType() == FleetEventType.OTHER) {
            return event.getTitle();
        }

        return switch (event.getType()) {
            case LIABILITY_INSURANCE -> "Гражданска";
            case CASCO_INSURANCE -> "Каско";
            case TECHNICAL_INSPECTION -> "ГТП";
            case VIGNETTE -> "Винетка";
            case SERVICE -> "Обслужване";
            case TIRE_CHANGE -> "Смяна гуми";
            case OTHER -> event.getTitle();
        };
    }

    private void normalizeEntity(FleetVehicle fleetVehicle) {
        fleetVehicle.setRegistrationNumber(normalize(fleetVehicle.getRegistrationNumber()));
        fleetVehicle.setMakeModel(normalize(fleetVehicle.getMakeModel()));
        fleetVehicle.setVin(normalize(fleetVehicle.getVin()));
        fleetVehicle.setVehicleUserName(normalize(fleetVehicle.getVehicleUserName()));
        fleetVehicle.setVehicleUserEmail(normalize(fleetVehicle.getVehicleUserEmail()));
        fleetVehicle.setFleetManagerName(normalize(fleetVehicle.getFleetManagerName()));
        fleetVehicle.setFleetManagerEmail(normalize(fleetVehicle.getFleetManagerEmail()));
        fleetVehicle.setLiabilityInsurer(normalize(fleetVehicle.getLiabilityInsurer()));
        fleetVehicle.setCascoInsurer(normalize(fleetVehicle.getCascoInsurer()));
        fleetVehicle.setSummerTiresBrand(normalize(fleetVehicle.getSummerTiresBrand()));
        fleetVehicle.setSummerTiresSize(normalize(fleetVehicle.getSummerTiresSize()));
        fleetVehicle.setSummerTiresDot(normalize(fleetVehicle.getSummerTiresDot()));
        fleetVehicle.setWinterTiresBrand(normalize(fleetVehicle.getWinterTiresBrand()));
        fleetVehicle.setWinterTiresSize(normalize(fleetVehicle.getWinterTiresSize()));
        fleetVehicle.setWinterTiresDot(normalize(fleetVehicle.getWinterTiresDot()));
        fleetVehicle.setAllSeasonTiresBrand(normalize(fleetVehicle.getAllSeasonTiresBrand()));
        fleetVehicle.setAllSeasonTiresSize(normalize(fleetVehicle.getAllSeasonTiresSize()));
        fleetVehicle.setAllSeasonTiresDot(normalize(fleetVehicle.getAllSeasonTiresDot()));
        fleetVehicle.setNotes(normalize(fleetVehicle.getNotes()));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
