package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.entities.Vehicle;
import com.example.WordDocumentsFiller.entities.enums.PaidStatus;
import com.example.WordDocumentsFiller.entities.enums.TitlesStatus;
import com.example.WordDocumentsFiller.entities.enums.VehicleStatus;
import com.example.WordDocumentsFiller.service.ContainerService;
import com.example.WordDocumentsFiller.service.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/container-tracker")
public class ContainerTrackerController {

    private final ContainerService containerService;
    private final VehicleService vehicleService;

    public ContainerTrackerController(ContainerService containerService, VehicleService vehicleService) {
        this.containerService = containerService;
        this.vehicleService = vehicleService;
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

    @GetMapping("/containers/{id}")
    public String containerDetails(@PathVariable Long id, Model model) {
        Container container = containerService.getById(id);

        model.addAttribute("container", container);
        model.addAttribute("vehicles", vehicleService.getByContainerId(id));
        model.addAttribute("newVehicle", new Vehicle());

        model.addAttribute("paidOptions", PaidStatus.values());
        model.addAttribute("titlesOptions", TitlesStatus.values());
        model.addAttribute("vehicleStatusOptions", VehicleStatus.values());

        return "container-tracker/container-details";
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



}
