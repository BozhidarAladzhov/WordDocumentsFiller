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


    @GetMapping("/")
    public String showHome() {
        return "home";
    }

    @GetMapping("/form/{template}")
    public String showForm(@PathVariable String template, Model model) {
        model.addAttribute("offerData", new OfferData());
        model.addAttribute("template", template);

        return "offer_template_" + template;
    }

    @PostMapping("/generate/{template}")
    public ResponseEntity<InputStreamResource> generateOffer(@ModelAttribute OfferData data, @PathVariable String template) throws IOException {


        String docxTemplate = "offer_template_" + template + ".docx";

        File tempFile = File.createTempFile("offer_", ".docx");
        offerGeneratorService.generateOffer(data, docxTemplate, tempFile.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=offer.docx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }
}
