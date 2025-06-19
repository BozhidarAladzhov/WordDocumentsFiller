package com.example.WordDocumentsFiller;

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

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("offerData", new OfferData());
        return "generate";
    }

    @PostMapping("/generate")
    public ResponseEntity<InputStreamResource> generateOffer(@ModelAttribute OfferData data) throws IOException {

        String template = "offer_template.docx";

        File tempFile = File.createTempFile("offer_", ".docx");
        offerGeneratorService.generateOffer(data, template, tempFile.getAbsolutePath());

        // Връщане на документа като файл за сваляне
        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=offer.docx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }
}
