package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.OfferData;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OfferGeneratorService {

    public void generateOffer(OfferData data, String templatePath, String outputPath) throws IOException {

        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(templatePath);
        XWPFDocument document = new XWPFDocument(templateStream);

        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = LocalDate.parse(data.getDate(), inputFormatter).format(outputFormatter);
        String formattedValidate = LocalDate.parse(data.getValidate(), inputFormatter).format(outputFormatter);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("${date}", formattedDate);
        replacements.put("${email}", data.getEmail());
        replacements.put("${fullName}", data.getFullName());
        replacements.put("${vehicle}", data.getVehicle());
        replacements.put("${domesticTransport}", data.getDomesticTransport());
        replacements.put("${hazardousCargo}", data.getHazardousCargo());
        replacements.put("${terminalHandling}", data.getTerminalHandling());
        replacements.put("${pickUpAddress}", data.getPickUpAddress());
        replacements.put("${portOfLoading}", data.getPortOfLoading());
        replacements.put("${oceanFreight}", data.getOceanFreight());
        replacements.put("${portOfDelivery}", data.getPortOfDelivery());
        replacements.put("${inlandTransport}", data.getInlandTransport());
        replacements.put("${validate}", formattedValidate);

        for (XWPFParagraph p : document.getParagraphs()) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                replacePlaceholderSplitRuns(p, entry.getKey(), entry.getValue());
            }
        }

        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (Map.Entry<String, String> entry : replacements.entrySet()) {
                            replacePlaceholderSplitRuns(p, entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            document.write(outputStream);
        }
        document.close();

    }
    private void replacePlaceholderSplitRuns(XWPFParagraph paragraph, String placeholder, String value) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.size() == 0) return;

        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            fullText.append(text == null ? "" : text);
        }
        String paragraphText = fullText.toString();

        int idx = paragraphText.indexOf(placeholder);
        if (idx < 0) return;

        // Опитваме се да намерим run-овете, които съдържат плейсхолдъра (дори split-нат)
        int placeholderLength = placeholder.length();
        int matchedChars = 0;
        int startRun = -1, startOffset = -1, endRun = -1, endOffset = -1;

        for (int runIdx = 0; runIdx < runs.size(); runIdx++) {
            XWPFRun run = runs.get(runIdx);
            String runText = run.getText(0);
            if (runText == null) runText = "";

            for (int charIdx = 0; charIdx < runText.length(); charIdx++) {
                if (runText.charAt(charIdx) == placeholder.charAt(matchedChars)) {
                    if (matchedChars == 0) {
                        startRun = runIdx;
                        startOffset = charIdx;
                    }
                    matchedChars++;
                    if (matchedChars == placeholderLength) {
                        endRun = runIdx;
                        endOffset = charIdx;
                        break;
                    }
                } else {
                    matchedChars = 0;
                    startRun = -1;
                    startOffset = -1;
                }
            }
            if (matchedChars == placeholderLength) break;
        }
        if (matchedChars != placeholderLength) return;

        // Изтриваме текста на всички run-ове, които съдържат плейсхолдъра
        // и вмъкваме стойността с формат
        String before = "";
        String after = "";

        if (startRun == endRun) {
            String txt = runs.get(startRun).getText(0);
            before = txt.substring(0, startOffset);
            after = txt.substring(endOffset + 1);
            runs.get(startRun).setText(before, 0);

            XWPFRun valueRun = paragraph.insertNewRun(startRun + 1);
            valueRun.setText(value != null ? value : "");
            valueRun.setBold(true);
            valueRun.setUnderline(UnderlinePatterns.SINGLE);

            XWPFRun afterRun = paragraph.insertNewRun(startRun + 2);
            afterRun.setText(after, 0);
        } else {
            // първи run – текста преди плейсхолдъра
            String firstText = runs.get(startRun).getText(0);
            before = firstText.substring(0, startOffset);
            runs.get(startRun).setText(before, 0);

            // последен run – текста след плейсхолдъра
            String lastText = runs.get(endRun).getText(0);
            after = lastText.substring(endOffset + 1);

            // Изчистваме run-овете между startRun+1 и endRun-1 (ако има)
            for (int i = startRun + 1; i < endRun; i++) {
                runs.get(i).setText("", 0);
            }
            // Изчистваме плейсхолдъра от последния run
            runs.get(endRun).setText("", 0);

            // Добавяме bold стойност след първия run
            XWPFRun valueRun = paragraph.insertNewRun(startRun + 1);
            valueRun.setText(value != null ? value : "");
            valueRun.setBold(true);
            valueRun.setUnderline(UnderlinePatterns.SINGLE);

            // Добавяме остатъка след плейсхолдъра в нов run
            XWPFRun afterRun = paragraph.insertNewRun(startRun + 2);
            afterRun.setText(after, 0);
        }
    }



}
