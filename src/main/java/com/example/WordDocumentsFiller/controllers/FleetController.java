package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.entities.FleetEvent;
import com.example.WordDocumentsFiller.entities.FleetContact;
import com.example.WordDocumentsFiller.entities.FleetMaintenanceRecord;
import com.example.WordDocumentsFiller.entities.enums.FleetEventType;
import com.example.WordDocumentsFiller.entities.FleetVehicle;
import com.example.WordDocumentsFiller.entities.enums.FleetVehicleStatus;
import com.example.WordDocumentsFiller.entities.enums.InsurancePaymentPlan;
import com.example.WordDocumentsFiller.service.FleetEventService;
import com.example.WordDocumentsFiller.service.FleetContactService;
import com.example.WordDocumentsFiller.service.FleetMaintenanceRecordService;
import com.example.WordDocumentsFiller.service.FleetVehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fleet")
public class FleetController {

    private final FleetVehicleService fleetVehicleService;
    private final FleetContactService fleetContactService;
    private final FleetEventService fleetEventService;
    private final FleetMaintenanceRecordService fleetMaintenanceRecordService;

    public FleetController(FleetVehicleService fleetVehicleService,
                           FleetContactService fleetContactService,
                           FleetEventService fleetEventService,
                           FleetMaintenanceRecordService fleetMaintenanceRecordService) {
        this.fleetVehicleService = fleetVehicleService;
        this.fleetContactService = fleetContactService;
        this.fleetEventService = fleetEventService;
        this.fleetMaintenanceRecordService = fleetMaintenanceRecordService;
    }

    @GetMapping("/vehicles")
    public String vehicles(Model model) {
        model.addAttribute("dashboard", fleetVehicleService.buildDashboard());
        model.addAttribute("vehicleItems", fleetVehicleService.getVehicleListItems(java.time.LocalDate.now()));
        model.addAttribute("fleetContacts", fleetContactService.getAll());
        model.addAttribute("newFleetContact", new FleetContact());
        model.addAttribute("newVehicle", new FleetVehicle());
        model.addAttribute("statusOptions", FleetVehicleStatus.values());
        return "fleet/vehicles";
    }

    @PostMapping("/vehicles")
    public String createVehicle(@ModelAttribute("newVehicle") FleetVehicle newVehicle) {
        fleetVehicleService.create(newVehicle);
        return "redirect:/fleet/vehicles";
    }

    @PostMapping("/contacts")
    public String createContact(@ModelAttribute("newFleetContact") FleetContact newFleetContact) {
        fleetContactService.create(newFleetContact);
        return "redirect:/fleet/vehicles";
    }

    @PostMapping("/contacts/{id}/update")
    public String updateContact(@PathVariable Long id,
                                @ModelAttribute FleetContact fleetContact) {
        fleetContactService.update(id, fleetContact);
        return "redirect:/fleet/vehicles";
    }

    @PostMapping("/contacts/{id}/delete")
    public String deleteContact(@PathVariable Long id) {
        fleetContactService.delete(id);
        return "redirect:/fleet/vehicles";
    }

    @GetMapping("/vehicles/{id}")
    public String vehicleDetails(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", fleetVehicleService.getById(id));
        model.addAttribute("events", fleetEventService.getOpenByVehicleId(id));
        model.addAttribute("maintenanceRecords", fleetMaintenanceRecordService.getByVehicleId(id));
        model.addAttribute("newMaintenanceRecord", new FleetMaintenanceRecord());
        model.addAttribute("newEvent", new FleetEvent());
        model.addAttribute("statusOptions", FleetVehicleStatus.values());
        model.addAttribute("eventTypeOptions", FleetEventType.values());
        model.addAttribute("paymentPlanOptions", InsurancePaymentPlan.values());
        return "fleet/vehicle-details";
    }

    @PostMapping("/vehicles/{id}/update")
    public Object updateVehicle(@PathVariable Long id,
                                @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                @ModelAttribute("vehicle") FleetVehicle vehicle) {
        fleetVehicleService.updateDetails(id, vehicle);
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return ResponseEntity.noContent().build();
        }
        return "redirect:/fleet/vehicles/" + id;
    }

    @PostMapping("/vehicles/{id}/update-core")
    public String updateVehicleCore(@PathVariable Long id,
                                    @ModelAttribute("vehicle") FleetVehicle vehicle) {
        fleetVehicleService.updateCore(id, vehicle);
        return "redirect:/fleet/vehicles/" + id;
    }

    @PostMapping("/vehicles/{id}/events")
    public String createEvent(@PathVariable Long id,
                              @ModelAttribute("newEvent") FleetEvent newEvent) {
        fleetEventService.create(id, newEvent);
        return "redirect:/fleet/vehicles/" + id;
    }

    @PostMapping("/vehicles/{id}/events/{eventId}/complete")
    public String completeEvent(@PathVariable Long id,
                                @PathVariable Long eventId) {
        fleetEventService.complete(id, eventId);
        return "redirect:/fleet/vehicles/" + id;
    }

    @PostMapping("/vehicles/{id}/maintenance")
    public String createMaintenanceRecord(@PathVariable Long id,
                                          @ModelAttribute("newMaintenanceRecord") FleetMaintenanceRecord newMaintenanceRecord) {
        fleetMaintenanceRecordService.create(id, newMaintenanceRecord);
        return "redirect:/fleet/vehicles/" + id;
    }

    @PostMapping("/vehicles/{id}/maintenance/{recordId}/update")
    public String updateMaintenanceRecord(@PathVariable Long id,
                                          @PathVariable Long recordId,
                                          @ModelAttribute FleetMaintenanceRecord maintenanceRecord) {
        fleetMaintenanceRecordService.update(id, recordId, maintenanceRecord);
        return "redirect:/fleet/vehicles/" + id;
    }

    @PostMapping("/vehicles/{id}/maintenance/{recordId}/delete")
    public String deleteMaintenanceRecord(@PathVariable Long id,
                                          @PathVariable Long recordId) {
        fleetMaintenanceRecordService.delete(id, recordId);
        return "redirect:/fleet/vehicles/" + id;
    }
}
