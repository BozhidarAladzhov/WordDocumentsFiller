package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.OfferData;
import com.example.WordDocumentsFiller.service.OfferGeneratorService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Controller
public class OfferController {

    private final OfferGeneratorService offerGeneratorService;

    public OfferController(OfferGeneratorService offerGeneratorService) {
        this.offerGeneratorService = offerGeneratorService;
    }


    @GetMapping("/offers")
    public String showOffers() {
        return "offers";
    }

    @GetMapping("/form/{destination}/{category}")
    public String showForm(@PathVariable String destination,
                           @PathVariable String category,
                           Model model) {
        model.addAttribute("offerData", new OfferData());
        model.addAttribute("market", destination);
        model.addAttribute("category", category);

        if ("LCL".equalsIgnoreCase(category)) {
            return "offer_template_" + destination;
        }

        return "offer_template_" + destination + "_" + category;
    }

    @PostMapping("/generate/{destination}/{category}")
    public ResponseEntity<InputStreamResource> generateOffer(@ModelAttribute OfferData data,
                                                             @PathVariable String destination,
                                                             @PathVariable String category) throws IOException {

        String docxTemplate;
        if ("LCL".equalsIgnoreCase(category)) {
            docxTemplate = "offer_template_" + destination + ".docx";
        } else {
            docxTemplate = "offer_template_" + destination + "_" + category + ".docx";
        }

        String fileName = ("offer_" +
                safeFilePart(data.getPortOfLoading(), "portOfLoading") + "_" +
                safeFilePart(data.getPortOfDelivery(), "portOfDelivery") + "_" +
                safeFilePart(data.getVehicle(), "vehicle") +
                ".docx");

        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        offerGeneratorService.generateOffer(data, docxTemplate, tempFile.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }

    private String safeFilePart(String v, String fallback) {
        if (v == null || v.isBlank()) return fallback;
        return v.trim().replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_\\-\\.]", "");
    }


}
