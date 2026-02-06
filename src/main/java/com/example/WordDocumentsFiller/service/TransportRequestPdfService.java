package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.TransportRequestData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class TransportRequestPdfService {

    private final TransportRequestGeneratorService generator;

    @Value("${libreoffice.sofficePath:}")
    private String sofficePath;

    @Value("${offer.output-dir:${java.io.tmpdir}}")
    private String outputDir;

    public TransportRequestPdfService(TransportRequestGeneratorService generator) {
        this.generator = generator;
    }

    public void generatePdf(TransportRequestData data, String templatePath, String outputPdfPath) throws Exception {
        if (sofficePath == null || sofficePath.isBlank()) {
            throw new IllegalStateException("Missing property libreoffice.sofficePath in application.properties");
        }

        Path targetPdf = Path.of(outputPdfPath);

        Path workDir = Path.of(outputDir);
        Files.createDirectories(workDir);

        String baseName = stripExtension(targetPdf.getFileName().toString());
        Path tempDocx = workDir.resolve(baseName + ".docx");

        generator.generate(data, templatePath, tempDocx.toAbsolutePath().toString());

        LibreOfficePdfConverter.convertDocxToPdf(sofficePath, tempDocx, workDir);

        Path producedPdf = workDir.resolve(baseName + ".pdf");
        Files.move(producedPdf, targetPdf, StandardCopyOption.REPLACE_EXISTING);

        try { Files.deleteIfExists(tempDocx); } catch (Exception ignored) {}
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

}
