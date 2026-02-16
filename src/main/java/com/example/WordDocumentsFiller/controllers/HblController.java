package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.HblData;
import com.example.WordDocumentsFiller.entities.Party;
import com.example.WordDocumentsFiller.entities.enums.PartyType;
import com.example.WordDocumentsFiller.service.ContainerService;
import com.example.WordDocumentsFiller.service.HblGeneratorService;
import com.example.WordDocumentsFiller.service.HblPdfService;
import com.example.WordDocumentsFiller.service.PartyService;
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
public class HblController {

    private final HblGeneratorService hblGeneratorService;
    private final HblPdfService hblPdfService;
    private final ContainerService containerService;
    private final VehicleService vehicleService;
    private final PartyService partyService;

    public HblController(HblGeneratorService hblGeneratorService,
                         HblPdfService hblPdfService,
                         ContainerService containerService,
                         VehicleService vehicleService,
                         PartyService partyService) {
        this.hblGeneratorService = hblGeneratorService;
        this.hblPdfService = hblPdfService;
        this.containerService = containerService;
        this.vehicleService = vehicleService;
        this.partyService = partyService;
    }

    @GetMapping("/hbl_form")
    public String legacyHblLinkRedirect() {
        return "redirect:/form/HBL";
    }

    @GetMapping("/form/HBL")
    public String showHblForm(@RequestParam(required = false) Long containerId,
                              @RequestParam(required = false) Long vehicleId,
                              Model model) {
        HblData data = new HblData();

        if (containerId != null) {
            var container = containerService.getById(containerId);
            data.setContainer(safe(container.getContainerNo()));
            data.setCarrier(safe(container.getVesselName()).isBlank()
                    ? safe(container.getCarrier())
                    : safe(container.getVesselName()));
            data.setPol(safe(container.getPol()));
            data.setPod(safe(container.getPod()));
            data.setSeal(safe(container.getSeal()));
            data.setDateOnBoard(safe(container.getShippedOnBoard()));
        }

        if (containerId != null && vehicleId != null) {
            var vehicle = vehicleService.getVehicleInContainer(containerId, vehicleId);
            data.setDescription(safe(vehicle.getDescription()));
            data.setVin(safe(vehicle.getVin()));
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        if (data.getDateOnBoard() == null || data.getDateOnBoard().isBlank()) {
            data.setDateOnBoard(today);
        }
        data.setDateOfIssue(today);
        data.setTerm("PREPAID");

        model.addAttribute("hblData", data);
        addPartyOptions(model);
        return "hbl_form";
    }

    @PostMapping("/generate/HBL")
    public ResponseEntity<InputStreamResource> generateHbl(@ModelAttribute HblData data) throws IOException {
        applyPartySelection(data);

        String docxTemplate = "HBL_blank.docx";
        String fileBase = safeFilePart(data.getOrder()) + "_" +
                safeFilePart(data.getBuyerName()) + "_" +
                safeFilePart(data.getDescription());
        String fileName = fileBase + ".docx";
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        hblGeneratorService.generate(data, docxTemplate, tempFile.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }

    @PostMapping("/generate-pdf/HBL")
    public ResponseEntity<InputStreamResource> generateHblPdf(@ModelAttribute HblData data) throws Exception {
        applyPartySelection(data);

        String docxTemplate = "HBL_blank.docx";
        String fileBase = safeFilePart(data.getOrder()) + "_" +
                safeFilePart(data.getBuyerName()) + "_" +
                safeFilePart(data.getDescription());
        String fileName = fileBase + ".pdf";
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        hblPdfService.generateHblPdf(data, docxTemplate, tempFile.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    private void addPartyOptions(Model model) {
        model.addAttribute("sellerParties", partyService.getByType(PartyType.SELLER));
        model.addAttribute("buyerParties", partyService.getByType(PartyType.BUYER));
    }

    private void applyPartySelection(HblData data) {
        Party seller = partyService.resolveAndMaybeCreate(
                PartyType.SELLER,
                data.getSellerId(),
                data.getSellerName(),
                data.getSellerAddress(),
                data.getSellerTown(),
                data.getSellerCountry()
        );
        if (seller != null) {
            data.setSellerName(seller.getName());
            data.setSellerAddress(seller.getAddress());
            data.setSellerTown(safe(seller.getTown()));
            data.setSellerCountry(safe(seller.getCountry()));
        }

        Party buyer = partyService.resolveAndMaybeCreate(
                PartyType.BUYER,
                data.getBuyerId(),
                data.getBuyerName(),
                data.getBuyerAddress(),
                data.getBuyerTown(),
                data.getBuyerCountry()
        );
        if (buyer != null) {
            data.setBuyerName(buyer.getName());
            data.setBuyerAddress(buyer.getAddress());
            data.setBuyerTown(safe(buyer.getTown()));
            data.setBuyerCountry(safe(buyer.getCountry()));
        }
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
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
