package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.UnpData;
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
public class UnpInstructionsGeneratorService {

    public void generate(UnpData data, String templatePathInResources, String outputPath) throws IOException {

        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(templatePathInResources);
        if (templateStream == null) {
            throw new IllegalArgumentException("Template not found in resources: " + templatePathInResources);
        }

        XWPFDocument document = new XWPFDocument(templateStream);

        // arrivalDate: input yyyy-MM-dd -> output dd.MM.yyyy (по-подходящо за BG)
        String formattedArrivalDate = formatDate(data.getArrivalDate(), "yyyy-MM-dd", "dd.MM.yyyy");

        Map<String, String> replacements = new HashMap<>();
        replacements.put("{TransportCompany}", safe(data.getTransportCompany()));
        replacements.put("{TransportCompanyVAT}", safe(data.getTransportCompanyVAT()));
        replacements.put("{TRUCK}", safe(data.getTruck()));
        replacements.put("{TRAILER}", safe(data.getTrailer()));
        replacements.put("{ArrivalDate}", safe(formattedArrivalDate));
        replacements.put("{ArrivalTime}", safe(data.getArrivalTime()));
        replacements.put("{09:00}", safe(data.getArrivalTime()));

        // Paragraphs
        for (XWPFParagraph p : document.getParagraphs()) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                replacePlaceholderSplitRuns(p, entry.getKey(), entry.getValue());
            }
        }

        // Tables (ако някога добавиш таблица в UNP docx)
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

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private String formatDate(String date, String inPattern, String outPattern) {
        if (date == null || date.isBlank()) return "";
        DateTimeFormatter input = DateTimeFormatter.ofPattern(inPattern);
        DateTimeFormatter output = DateTimeFormatter.ofPattern(outPattern);
        return LocalDate.parse(date, input).format(output);
    }

    // Същата идея като при OfferGeneratorService – работи и когато placeholder-а е split-нат на няколко runs. :contentReference[oaicite:4]{index=4}
    private void replacePlaceholderSplitRuns(XWPFParagraph paragraph, String placeholder, String value) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;

        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            fullText.append(text == null ? "" : text);
        }

        String paragraphText = fullText.toString();
        if (!paragraphText.contains(placeholder)) return;

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

        String before;
        String after;

        if (startRun == endRun) {
            String txt = runs.get(startRun).getText(0);
            if (txt == null) txt = "";
            before = txt.substring(0, startOffset);
            after = txt.substring(endOffset + 1);

            runs.get(startRun).setText(before, 0);

            XWPFRun valueRun = paragraph.insertNewRun(startRun + 1);
            valueRun.setText(value);
            valueRun.setFontFamily("Calibri");
            valueRun.setFontSize(12);
            valueRun.setBold(true);
            valueRun.setUnderline(UnderlinePatterns.SINGLE);

            XWPFRun afterRun = paragraph.insertNewRun(startRun + 2);
            afterRun.setText(after, 0);
        } else {
            String firstText = runs.get(startRun).getText(0);
            if (firstText == null) firstText = "";
            before = firstText.substring(0, startOffset);
            runs.get(startRun).setText(before, 0);

            String lastText = runs.get(endRun).getText(0);
            if (lastText == null) lastText = "";
            after = lastText.substring(endOffset + 1);

            for (int i = startRun + 1; i < endRun; i++) {
                runs.get(i).setText("", 0);
            }
            runs.get(endRun).setText("", 0);

            XWPFRun valueRun = paragraph.insertNewRun(startRun + 1);
            valueRun.setText(value);
            valueRun.setFontFamily("Calibri");
            valueRun.setFontSize(12);
            valueRun.setBold(true);
            valueRun.setUnderline(UnderlinePatterns.SINGLE);

            XWPFRun afterRun = paragraph.insertNewRun(startRun + 2);
            afterRun.setText(after, 0);
        }
    }


}
