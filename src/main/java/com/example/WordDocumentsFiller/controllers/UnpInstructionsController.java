package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.UnpData;
import com.example.WordDocumentsFiller.service.UnpInstructionsGeneratorService;
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
public class UnpInstructionsController {

    private final UnpInstructionsGeneratorService generatorService;

    public UnpInstructionsController(UnpInstructionsGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @GetMapping("/unp_instructions_form")
    public String legacyUnpLinkRedirect() {
        return "redirect:/form/UNP";
    }

    @GetMapping("/form/UNP")
    public String showUnpForm(Model model) {
        UnpData data = new UnpData();
        data.setArrivalTime("09:00"); // default да ти е готово
        model.addAttribute("unpData", data);
        return "unp_instructions_form";
    }

    @PostMapping("/generate/UNP")
    public ResponseEntity<InputStreamResource> generateUnp(@ModelAttribute UnpData data) throws IOException {


        String docxTemplate = "UNP_instructions.docx";

        String fileName = "UNP_" +
                safeFilePart(data.getTruck()) + "_" +
                safeFilePart(data.getTrailer()) +
                ".docx";

        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);

        generatorService.generate(data, docxTemplate, tempFile.getAbsolutePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(tempFile));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }

    private String safeFilePart(String v) {
        if (v == null || v.isBlank()) return "NA";
        return v.trim().replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_\\-\\.]", "");
    }


}
