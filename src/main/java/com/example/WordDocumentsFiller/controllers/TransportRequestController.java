package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.TransportRequestData;
import com.example.WordDocumentsFiller.service.TransportRequestGeneratorService;
import com.example.WordDocumentsFiller.service.TransportRequestPdfService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;

@Controller
public class TransportRequestController {

    private final TransportRequestGeneratorService generatorService;
    private final TransportRequestPdfService pdfService;

    public TransportRequestController(TransportRequestGeneratorService generatorService,
                                      TransportRequestPdfService pdfService) {
        this.generatorService = generatorService;
        this.pdfService = pdfService;
    }

    @GetMapping("/form/TRUCK_ORDER")
    public String showForm(Model model) {
        TransportRequestData data = new TransportRequestData();
        data.setDate(LocalDate.now().toString());
        model.addAttribute("transportData", data);
        return "transport_request_form";
    }

    @PostMapping("/generate/TRUCK_ORDER")
    public ResponseEntity<InputStreamResource> generateDocx(@ModelAttribute TransportRequestData data) throws Exception {

        String docxTemplate = "truck_order.docx";

        String fileBase = safeFilePart(data.getTruck()) + "_" + safeFilePart(data.getDateOfLoading());
        String fileName = fileBase + ".docx";

        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        generatorService.generate(data, docxTemplate, tempFile.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }

    @PostMapping("/generate-pdf/TRUCK_ORDER")
    public ResponseEntity<InputStreamResource> generatePdf(@ModelAttribute TransportRequestData data) throws Exception {

        String docxTemplate = "truck_order.docx";

        String fileBase = safeFilePart(data.getTruck()) + "_" + safeFilePart(data.getDateOfLoading());
        String pdfFileName = fileBase + ".pdf";

        File tempPdf = new File(System.getProperty("java.io.tmpdir"), pdfFileName);

        pdfService.generatePdf(data, docxTemplate, tempPdf.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempPdf));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdfFileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    private String safeFilePart(String v) {
        if (v == null || v.isBlank()) return "NA";
        return v.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_\\-\\.]", "");
    }


}
