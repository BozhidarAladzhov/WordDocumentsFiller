package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.UpData;
import com.example.WordDocumentsFiller.entities.UpClient;
import com.example.WordDocumentsFiller.service.ContainerService;
import com.example.WordDocumentsFiller.service.UpClientService;
import com.example.WordDocumentsFiller.service.UpGeneratorService;
import com.example.WordDocumentsFiller.service.UpPdfService;
import com.example.WordDocumentsFiller.service.VehicleService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
public class UpController {

    private final UpGeneratorService upGeneratorService;
    private final UpPdfService upPdfService;
    private final ContainerService containerService;
    private final VehicleService vehicleService;
    private final UpClientService upClientService;

    public UpController(UpGeneratorService upGeneratorService,
                        UpPdfService upPdfService,
                        ContainerService containerService,
                        VehicleService vehicleService,
                        UpClientService upClientService) {
        this.upGeneratorService = upGeneratorService;
        this.upPdfService = upPdfService;
        this.containerService = containerService;
        this.vehicleService = vehicleService;
        this.upClientService = upClientService;
    }

    @GetMapping("/up_form")
    public String legacyUpLinkRedirect() {
        return "redirect:/form/UP";
    }

    @GetMapping("/form/UP")
    public String showUpForm(@RequestParam(required = false) Long containerId,
                             @RequestParam(required = false) Long vehicleId,
                             Model model) {
        UpData data = new UpData();

        if (containerId != null) {
            var container = containerService.getById(containerId);
            data.setMbl(safe(container.getBol()));
            data.setContainer(safe(container.getContainerNo()));
        }

        if (containerId != null && vehicleId != null) {
            var vehicle = vehicleService.getVehicleInContainer(containerId, vehicleId);
            data.setDescription(safe(vehicle.getDescription()));
            data.setVin(safe(vehicle.getVin()));
        }

        data.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        model.addAttribute("upData", data);
        model.addAttribute("upClients", upClientService.getAll());
        return "up_form";
    }

    @PostMapping("/generate/UP")
    public ResponseEntity<InputStreamResource> generateUp(@ModelAttribute UpData data) throws IOException {
        applyClientSelection(data);

        String docxTemplate = "UP_BLANK.docx";
        String fileBase = safeFilePart(data.getOrder()) + "_" +
                safeFilePart(data.getDescription());
        String fileName = fileBase + ".docx";
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        upGeneratorService.generate(data, docxTemplate, tempFile.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }

    @PostMapping("/generate-pdf/UP")
    public ResponseEntity<InputStreamResource> generateUpPdf(@ModelAttribute UpData data) throws Exception {
        applyClientSelection(data);

        String docxTemplate = "UP_BLANK.docx";
        String fileBase = safeFilePart(data.getOrder()) + "_" +
                safeFilePart(data.getDescription());
        String fileName = fileBase + ".pdf";
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        upPdfService.generateUpPdf(data, docxTemplate, tempFile.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    private void applyClientSelection(UpData data) {
        UpClient client = upClientService.resolveAndMaybeCreate(
                data.getClientId(),
                data.getBuyerName(),
                data.getBuyerAddress(),
                data.getBuyerTown(),
                data.getBuyerCountry(),
                data.getEori()
        );

        if (client != null) {
            data.setBuyerName(client.getName());
            data.setBuyerAddress(client.getAddress());
            data.setBuyerTown(safe(client.getTown()));
            data.setBuyerCountry(safe(client.getCountry()));
            data.setEori(safe(client.getEori()));
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeFilePart(String v) {
        if (v == null || v.isBlank()) {
            return "NA";
        }
        return v.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^\\p{L}\\p{N}_\\-\\.]", "");
    }
}
