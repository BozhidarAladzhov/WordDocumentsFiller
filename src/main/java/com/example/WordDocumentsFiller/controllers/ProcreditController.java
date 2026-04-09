package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.service.ProcreditProcessingException;
import com.example.WordDocumentsFiller.service.ProcreditService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class ProcreditController {

    private final ProcreditService procreditService;

    public ProcreditController(ProcreditService procreditService) {
        this.procreditService = procreditService;
    }

    @GetMapping("/procredit")
    public String showPage() {
        return "procredit";
    }

    @PostMapping("/procredit/generate")
    public ResponseEntity<?> generate(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            ProcreditService.GeneratedWorkbook workbook = procreditService.generatePayments(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + workbook.fileName() + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .contentLength(workbook.content().length)
                    .body(new ByteArrayResource(workbook.content()));
        } catch (ProcreditProcessingException ex) {
            return ResponseEntity.badRequest()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body(ex.getMessage());
        }
    }
}
