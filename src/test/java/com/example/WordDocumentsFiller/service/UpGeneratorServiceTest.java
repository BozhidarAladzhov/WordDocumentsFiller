package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.UpData;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpGeneratorServiceTest {

    @Test
    void shouldReplaceAllUpPlaceholders() throws Exception {
        UpGeneratorService service = new UpGeneratorService();

        UpData data = new UpData();
        data.setBuyerName("Клиент ООД");
        data.setBuyerAddress("ул. Пример 1");
        data.setBuyerTown("София");
        data.setBuyerCountry("България");
        data.setEori("BG123456789");
        data.setMbl("MBL-2026-001");
        data.setContainer("MSCU1234567");
        data.setDescription("Toyota Corolla");
        data.setDescriptionBg("Тойота Корола");
        data.setVin("JTDBR32E620123456");
        data.setDate("16.02.2026");

        Path output = Files.createTempFile("up-test-", ".docx");
        try {
            service.generate(data, "UP_BLANK.docx", output.toString());
            String documentXml = readZipEntry(output, "word/document.xml");

            assertTrue(Files.exists(output));
            assertTrue(Files.size(output) > 0);
            assertFalse(documentXml.contains("{buyerName}"));
            assertFalse(documentXml.contains("{buyerAddress}"));
            assertFalse(documentXml.contains("{buyerTown}"));
            assertFalse(documentXml.contains("{buyerCountry}"));
            assertFalse(documentXml.contains("{EORI}"));
            assertFalse(documentXml.contains("{MBL}"));
            assertFalse(documentXml.contains("{container}"));
            assertFalse(documentXml.contains("{description}"));
            assertFalse(documentXml.contains("{descriptionBG}"));
            assertFalse(documentXml.contains("{vin}"));
            assertFalse(documentXml.contains("{date}"));
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private String readZipEntry(Path zipPath, String entryName) throws Exception {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                throw new IllegalStateException("Missing entry in docx: " + entryName);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        }
    }
}
