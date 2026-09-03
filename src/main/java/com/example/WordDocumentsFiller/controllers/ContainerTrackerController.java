package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.GraphMailDraft;
import com.example.WordDocumentsFiller.dto.GraphTokenSession;
import com.example.WordDocumentsFiller.dto.UnloadingPrefillDto;
import com.example.WordDocumentsFiller.entities.Container;
import com.example.WordDocumentsFiller.entities.Vehicle;
import com.example.WordDocumentsFiller.entities.enums.ContainerStatus;
import com.example.WordDocumentsFiller.entities.enums.PaidStatus;
import com.example.WordDocumentsFiller.entities.enums.TitlesStatus;
import com.example.WordDocumentsFiller.entities.enums.VehicleStatus;
import com.example.WordDocumentsFiller.service.ContainerService;
import com.example.WordDocumentsFiller.service.GraphMailProperties;
import com.example.WordDocumentsFiller.service.GraphMailService;
import com.example.WordDocumentsFiller.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/container-tracker")
public class ContainerTrackerController {

    private static final String DEFAULT_CC = "cars@freeline.bg";

    private final ContainerService containerService;
    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper;
    private final GraphMailService graphMailService;
    private final GraphMailProperties graphMailProperties;

    public ContainerTrackerController(ContainerService containerService,
                                      VehicleService vehicleService,
                                      ObjectMapper objectMapper,
                                      GraphMailService graphMailService,
                                      GraphMailProperties graphMailProperties) {
        this.containerService = containerService;
        this.vehicleService = vehicleService;
        this.objectMapper = objectMapper;
        this.graphMailService = graphMailService;
        this.graphMailProperties = graphMailProperties;
    }

    @GetMapping("/containers")
    public String containers(Model model) {
        var containers = containerService.getActive();
        Map<Long, TitlesStatus> containerTitlesStatuses = containers.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Container::getId, c -> vehicleService.getContainerTitlesStatus(c.getId())));

        model.addAttribute("containers", containers);
        model.addAttribute("containerTitlesStatuses", containerTitlesStatuses);
        model.addAttribute("newContainer", new Container());
        return "container-tracker/containers";
    }

    @GetMapping("/archive")
    public String archive(Model model) {
        model.addAttribute("containers", containerService.getArchived());
        return "container-tracker/archive";
    }

    @GetMapping("/vehicles")
    public String vehicles(Model model) {
        var vehicles = vehicleService.getAllWithContainer();
        Map<Long, Long> containerVehicleCounts = vehicles.stream()
                .filter(v -> v.getContainer() != null && v.getContainer().getId() != null)
                .collect(Collectors.groupingBy(v -> v.getContainer().getId(), Collectors.counting()));

        model.addAttribute("vehicles", vehicles);
        model.addAttribute("containerVehicleCounts", containerVehicleCounts);
        return "container-tracker/vehicles";
    }

    @PostMapping("/containers")
    public String createContainer(@ModelAttribute("newContainer") Container newContainer) {
        containerService.create(newContainer);
        return "redirect:/container-tracker/containers";
    }

    @PostMapping("/containers/{id}/delete")
    public String deleteContainer(@PathVariable Long id) {
        Container container = containerService.getById(id);
        containerService.deleteContainer(id);
        return container.isArchived()
                ? "redirect:/container-tracker/archive"
                : "redirect:/container-tracker/containers";
    }

    @PostMapping("/containers/{id}/archive")
    public String archiveContainer(@PathVariable Long id) {
        containerService.archiveContainer(id);
        return "redirect:/container-tracker/containers";
    }

    @PostMapping("/containers/{id}/unarchive")
    public String unarchiveContainer(@PathVariable Long id) {
        containerService.unarchiveContainer(id);
        return "redirect:/container-tracker/archive";
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
        model.addAttribute("containerStatusOptions", ContainerStatus.values());

        return "container-tracker/container-details";
    }

    @PostMapping("/containers/{id}/update")
    public String updateContainer(@PathVariable Long id,
                                  @RequestParam(required = false) String bol,
                                  @RequestParam(required = false) String carrier,
                                  @RequestParam(required = false) String trackingLink,
                                  @RequestParam(required = false) String vesselName,
                                  @RequestParam(required = false) String pol,
                                  @RequestParam(required = false) String pod,
                                  @RequestParam(required = false) String seal,
                                  @RequestParam(required = false) String shippedOnBoard,
                                  @RequestParam(required = false) LocalDate eta,
                                  @RequestParam ContainerStatus status) {
        containerService.updateContainer(id, bol, carrier, trackingLink, vesselName, pol, pod, seal, shippedOnBoard, eta, status);
        return "redirect:/container-tracker/containers/" + id;
    }

    @PostMapping("/containers/{id}/titles")
    public String updateContainerTitles(@PathVariable Long id,
                                        @RequestParam TitlesStatus titles) {
        containerService.getById(id);
        vehicleService.updateTitlesForContainer(id, titles);
        return "redirect:/container-tracker/containers";
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
            String desc = safeText(v.getDescription());
            String vin = safeText(v.getVin());
            String car = desc + (vin.isBlank() ? "" : " (" + vin + ")");

            boolean canPickup = (v.getPaid() != null && "PAID".equals(v.getPaid().name()));
            boolean hasDocs = (v.getTitles() != null && "RECEIVED".equals(v.getTitles().name()));

            return new UnloadingPrefillDto.VehicleDto(car, canPickup, hasDocs, false);
        }).collect(Collectors.toList());

        var payload = new UnloadingPrefillDto(container.getContainerNo(), container.getUnloadingAt(), vehicleDtos);

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
                             @RequestParam(required = false) String mailStatus,
                             HttpSession session,
                             Model model) {
        EmailDraftViewData draft = buildEmailDraftViewData(containerId, vehicleId);

        model.addAttribute("container", draft.container());
        model.addAttribute("vehicle", draft.vehicle());
        model.addAttribute("graphConfigured", graphMailProperties.isConfigured());
        model.addAttribute("graphConnected", session.getAttribute(GraphMailController.SESSION_TOKEN_KEY) != null);
        model.addAttribute("mailStatus", mailStatus);
        model.addAttribute("mailTo", draft.to());
        model.addAttribute("mailCc", draft.cc());
        model.addAttribute("mailSubject", draft.subject());
        model.addAttribute("mailBody", draft.body());

        return "container-tracker/email-draft";
    }

    @PostMapping("/containers/{containerId}/vehicles/{vehicleId}/email-draft/send")
    public String sendEmailDraft(@PathVariable Long containerId,
                                 @PathVariable Long vehicleId,
                                 @RequestParam String to,
                                 @RequestParam(required = false) String cc,
                                 @RequestParam String subject,
                                 @RequestParam String body,
                                 HttpSession session) {
        String returnTo = "/container-tracker/containers/" + containerId + "/vehicles/" + vehicleId + "/email-draft";
        if (!graphMailProperties.isConfigured()) {
            return "redirect:" + returnTo + "?mailStatus=graphNotConfigured";
        }

        try {
            GraphTokenSession token = (GraphTokenSession) session.getAttribute(GraphMailController.SESSION_TOKEN_KEY);
            if (token == null) {
                session.setAttribute(GraphMailController.SESSION_PENDING_DRAFT_KEY,
                        new GraphMailDraft(to, safeText(cc), subject, body, returnTo));
                return "redirect:/microsoft/graph/connect?returnTo=" + urlEncode(returnTo);
            }

            GraphTokenSession validToken = graphMailService.ensureValidToken(token);
            session.setAttribute(GraphMailController.SESSION_TOKEN_KEY, validToken);
            graphMailService.sendMail(validToken.getAccessToken(), validToken.getAccountEmail(), to, safeText(cc), subject, body);
            return "redirect:" + returnTo + "?mailStatus=sent";
        } catch (Exception ex) {
            session.removeAttribute(GraphMailController.SESSION_TOKEN_KEY);
            session.setAttribute(GraphMailController.SESSION_PENDING_DRAFT_KEY,
                    new GraphMailDraft(to, safeText(cc), subject, body, returnTo));
            return "redirect:/microsoft/graph/connect?returnTo=" + urlEncode(returnTo);
        }
    }

    private EmailDraftViewData buildEmailDraftViewData(Long containerId, Long vehicleId) {
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
                "Фактура или договор с който е закупен автомобилът.",
                "Фактура за морски транспорт.",
                "ЕОРИ номер.",
                "Къде ще обмитявате автомобила ?",
                "",
                "Поздрави,",
                "Божидар"
        );

        return new EmailDraftViewData(container, vehicle, mailText, DEFAULT_CC, subjectText, draftText);
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record EmailDraftViewData(
            Container container,
            Vehicle vehicle,
            String to,
            String cc,
            String subject,
            String body
    ) {
    }
}
