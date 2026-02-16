package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.HblData;
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

class HblGeneratorServiceTest {

    @Test
    void shouldReplaceAllHblPlaceholdersIncludingTextboxes() throws Exception {
        HblGeneratorService service = new HblGeneratorService();

        HblData data = new HblData();
        data.setCarrier("MSC");
        data.setContainer("MSCU1234567");
        data.setKgs("2500");
        data.setTotalKgs("2500");
        data.setOrder("ORD-2026-01");
        data.setPod("Varna");
        data.setPol("Rotterdam");
        data.setSeal("SEAL-99");
        data.setTerm("FOB");
        data.setDescription("Toyota Corolla");
        data.setVin("JTDBR32E620123456");
        data.setSellerName("Seller Co");
        data.setSellerAddress("Seller street 1");
        data.setSellerTown("Sofia");
        data.setSellerCountry("Bulgaria");
        data.setBuyerName("Buyer Co");
        data.setBuyerAddress("Buyer street 2");
        data.setBuyerTown("Varna");
        data.setBuyerCountry("Bulgaria");
        data.setDateOnBoard("16.02.2026");
        data.setDateOfIssue("16.02.2026");

        Path output = Files.createTempFile("hbl-test-", ".docx");

        try {
            service.generate(data, "HBL_blank.docx", output.toString());

            String documentXml = readZipEntry(output, "word/document.xml");

            assertTrue(Files.exists(output));
            assertTrue(Files.size(output) > 0);
            assertTrue(documentXml.contains("w:document"));

            assertFalse(documentXml.contains("{sellerName}"));
            assertFalse(documentXml.contains("{sellerAddress}"));
            assertFalse(documentXml.contains("{sellerTown}"));
            assertFalse(documentXml.contains("{sellerCountry}"));
            assertFalse(documentXml.contains("{buyerName}"));
            assertFalse(documentXml.contains("{buyerAddress}"));
            assertFalse(documentXml.contains("{buyerTown}"));
            assertFalse(documentXml.contains("{buyerCountry}"));
            assertFalse(documentXml.contains("{vin}"));
            assertFalse(documentXml.contains("{totalKgs}"));
            assertFalse(documentXml.contains("{dateOnBoard}"));
            assertFalse(documentXml.contains("{dateOfIssue}"));
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
