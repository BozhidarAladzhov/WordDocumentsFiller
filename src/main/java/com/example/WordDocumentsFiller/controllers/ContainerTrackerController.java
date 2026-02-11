package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.UnloadingPrefillDto;
import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.entities.Vehicle;
import com.example.WordDocumentsFiller.entities.enums.ContainerStatus;
import com.example.WordDocumentsFiller.entities.enums.PaidStatus;
import com.example.WordDocumentsFiller.entities.enums.TitlesStatus;
import com.example.WordDocumentsFiller.entities.enums.VehicleStatus;
import com.example.WordDocumentsFiller.service.ContainerService;
import com.example.WordDocumentsFiller.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/container-tracker")
public class ContainerTrackerController {

    private final ContainerService containerService;
    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper;


    public ContainerTrackerController(ContainerService containerService, VehicleService vehicleService, ObjectMapper objectMapper) {
        this.containerService = containerService;
        this.vehicleService = vehicleService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/containers")
    public String containers(Model model) {
        model.addAttribute("containers", containerService.getAll());
        model.addAttribute("newContainer", new Container());
        return "container-tracker/containers";
    }

    @PostMapping("/containers")
    public String createContainer(@ModelAttribute("newContainer") Container newContainer) {
        containerService.create(newContainer);
        return "redirect:/container-tracker/containers";
    }

    @PostMapping("/containers/{id}/delete")
    public String deleteContainer(@PathVariable Long id) {
        containerService.deleteContainer(id);
        return "redirect:/container-tracker/containers";
    }


    @GetMapping("/containers/{id}")
    public String containerDetails(@PathVariable Long id, Model model) {
        Container container = containerService.getById(id);

        model.addAttribute("container", container);
        model.addAttribute("vehicles", vehicleService.getByContainerId(id));
        model.addAttribute("newVehicle", new Vehicle());

        model.addAttribute("paidOptions", PaidStatus.values());
        model.addAttribute("titlesOptions", TitlesStatus.values());
        model.addAttribute("vehicleStatusOptions", VehicleStatus.values());

        // container status select
        model.addAttribute("containerStatusOptions", ContainerStatus.values());

        return "container-tracker/container-details";
    }

    @PostMapping("/containers/{id}/update")
    public String updateContainer(@PathVariable Long id,
                                  @RequestParam(required = false) String bol,
                                  @RequestParam(required = false) String carrier,
                                  @RequestParam(required = false) LocalDate eta,
                                  @RequestParam ContainerStatus status) {

        containerService.updateContainer(id, bol, carrier, eta, status);
        return "redirect:/container-tracker/containers/" + id;
    }



    @PostMapping("/containers/{id}/vehicles")
    public String addVehicle(@PathVariable Long id, @ModelAttribute("newVehicle") Vehicle newVehicle) {
        Container container = containerService.getById(id);
        vehicleService.addToContainer(container, newVehicle);
        return "redirect:/container-tracker/containers/" + id;
    }

    @PostMapping("/containers/{containerId}/vehicles/{vehicleId}/update")
    public String updateVehicle(@PathVariable Long containerId,
                                @PathVariable Long vehicleId,
                                @RequestParam PaidStatus paid,
                                @RequestParam TitlesStatus titles,
                                @RequestParam VehicleStatus status,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String notes) {

        vehicleService.updateVehicle(containerId, vehicleId, paid, titles, status, phone, email, notes);
        return "redirect:/container-tracker/containers/" + containerId;
    }

    @PostMapping("/containers/{containerId}/vehicles/{vehicleId}/delete")
    public String deleteVehicle(@PathVariable Long containerId,
                                @PathVariable Long vehicleId) {
        vehicleService.deleteVehicle(containerId, vehicleId);
        return "redirect:/container-tracker/containers/" + containerId;
    }


    @GetMapping("/containers/{containerId}/unloading")
    public String containerUnloading(@PathVariable Long containerId, Model model) {

        var container = containerService.getById(containerId);
        var vehicles = vehicleService.getByContainerId(containerId);

        var vehicleDtos = vehicles.stream().map(v -> {
            String desc = (v.getDescription() == null) ? "" : v.getDescription().trim();
            String vin = (v.getVin() == null) ? "" : v.getVin().trim();
            String car = desc + (vin.isBlank() ? "" : " (" + vin + ")");

            boolean canPickup = (v.getPaid() != null && "PAID".equals(v.getPaid().name()));
            boolean hasDocs = (v.getTitles() != null && "RECEIVED".equals(v.getTitles().name()));

            return new UnloadingPrefillDto.VehicleDto(car, canPickup, hasDocs, false);
        }).collect(Collectors.toList());

        var payload = new UnloadingPrefillDto(container.getContainerNo(), vehicleDtos);

        try {
            String json = objectMapper.writeValueAsString(payload);
            model.addAttribute("prefillJson", json);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot build prefill JSON", e);
        }

        return "unloading";
    }

    @GetMapping("/containers/{containerId}/vehicles/{vehicleId}/email-draft")
    public String emailDraft(@PathVariable Long containerId,
                             @PathVariable Long vehicleId,
                             Model model) {
        Container container = containerService.getById(containerId);
        Vehicle vehicle = vehicleService.getVehicleInContainer(containerId, vehicleId);

        String containerNo = safeText(container.getContainerNo());
        String description = safeText(vehicle.getDescription());
        String vin = safeText(vehicle.getVin());
        String vehicleLine = (description.isBlank() ? "" : description + "  ") + "VIN: " + vin;
        String subjectText = (description.isBlank() ? "" : description + " ") + "VIN: " + vin;
        String mailText = safeText(vehicle.getEmail());

        String draftText = String.join("\n",
                "Здравейте,",
                "",
                "Пишем ви във връзка с пристигането на контейнер: " + containerNo,
                "Номинирани сме като получатели и следва да обработим контейнера.",
                "От Autobidmaster ни предоставиха вашите данни, като собственик на автомобил:",
                vehicleLine,
                "",
                "Моля да ни изпратите следните документи и информация.",
                "",
                "Фактура или договор с който е закупен автомобил.",
                "Фактура за морски транспорт.",
                "ЕОРИ номер.",
                "Къде ще обмитявате автомобила ?",
                "",
                "Поздрави,",
                "Божидар"
        );

        model.addAttribute("container", container);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("draftText", draftText);
        model.addAttribute("subjectText", subjectText);
        model.addAttribute("mailText", mailText);

        return "container-tracker/email-draft";
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }




}
