package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.entities.Vehicle;
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
        return "container-tracker/container-details";
    }

    @PostMapping("/containers/{id}/vehicles")
    public String addVehicle(@PathVariable Long id, @ModelAttribute("newVehicle") Vehicle newVehicle) {
        Container container = containerService.getById(id);
        vehicleService.addToContainer(container, newVehicle);
        return "redirect:/container-tracker/containers/" + id;
    }

}
