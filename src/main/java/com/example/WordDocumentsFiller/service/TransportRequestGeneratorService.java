package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.TransportRequestData;
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
public class TransportRequestGeneratorService {

    public void generate(TransportRequestData data, String templatePathInResources, String outputPath) throws IOException {

        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(templatePathInResources);
        if (templateStream == null) {
            throw new IllegalArgumentException("Template not found in resources: " + templatePathInResources);
        }

        XWPFDocument document = new XWPFDocument(templateStream);

        String formattedDate = formatDate(data.getDate(), "yyyy-MM-dd", "dd.MM.yyyy");
        String formattedLoading = formatDate(data.getDateOfLoading(), "yyyy-MM-dd", "dd.MM.yyyy");
        String formattedDelivery = formatDate(data.getDateOfDelivery(), "yyyy-MM-dd", "dd.MM.yyyy");

        Map<String, String> replacements = new HashMap<>();
        replacements.put("{transportCompany}", safe(data.getTransportCompany()));
        replacements.put("{transportCompanyVAT}", safe(data.getTransportCompanyVAT()));
        replacements.put("{date}", safe(formattedDate));

        replacements.put("{truck}", safe(data.getTruck()));
        replacements.put("{loadingAddress}", safe(data.getLoadingAddress()));
        replacements.put("{dateOfLoading}", safe(formattedLoading));
        replacements.put("{dateOfDelivery}", safe(formattedDelivery));

        replacements.put("{vehiclesCount}", data.getVehiclesCount() == null ? "" : String.valueOf(data.getVehiclesCount()));
        replacements.put("{vehiclesList}", normalizeNewlines(safeKeepInner(data.getVehiclesList())));

        replacements.put("{price}", safe(data.getPrice()));

        // Paragraphs
        for (XWPFParagraph p : document.getParagraphs()) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                replacePlaceholderSplitRuns(p, entry.getKey(), entry.getValue());
            }
        }

        // Tables
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


    private String safeKeepInner(String v) {
        return v == null ? "" : v;
    }

    private String normalizeNewlines(String v) {
        if (v == null) return "";
        return v.replace("\r\n", "\n").replace("\r", "\n");
    }

    private String formatDate(String date, String inPattern, String outPattern) {
        if (date == null || date.isBlank()) return "";
        DateTimeFormatter input = DateTimeFormatter.ofPattern(inPattern);
        DateTimeFormatter output = DateTimeFormatter.ofPattern(outPattern);
        return LocalDate.parse(date, input).format(output);
    }

    /**
     * Replacement, който работи и когато плейсхолдърът е “счупен” на няколко runs.
     * + Поддържа multiline value (vehiclesList) чрез addBreak().
     */
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

        if (startRun == endRun) {
            String txt = runs.get(startRun).getText(0);
            if (txt == null) txt = "";

            String before = txt.substring(0, startOffset);
            String after = txt.substring(endOffset + 1);

            runs.get(startRun).setText(before, 0);

            XWPFRun valueRun = paragraph.insertNewRun(startRun + 1);
            copyStyle(runs.get(startRun), valueRun);
            writeMultiline(valueRun, value);

            XWPFRun afterRun = paragraph.insertNewRun(startRun + 2);
            copyStyle(runs.get(startRun), afterRun);
            afterRun.setText(after);

            // чистим останалите runs ако плейсхолдърът е бил само в този run
            // (няма допълнителни runs за чистене тук)
            return;
        }

        // Placeholder spanning multiple runs
        String startText = runs.get(startRun).getText(0);
        if (startText == null) startText = "";
        String before = startText.substring(0, startOffset);
        runs.get(startRun).setText(before, 0);

        String endText = runs.get(endRun).getText(0);
        if (endText == null) endText = "";
        String after = endText.substring(endOffset + 1);

        // remove runs between startRun+1 .. endRun inclusive
        for (int i = endRun; i >= startRun + 1; i--) {
            paragraph.removeRun(i);
        }

        XWPFRun valueRun = paragraph.insertNewRun(startRun + 1);
        copyStyle(runs.get(startRun), valueRun);
        writeMultiline(valueRun, value);

        XWPFRun afterRun = paragraph.insertNewRun(startRun + 2);
        copyStyle(runs.get(startRun), afterRun);
        afterRun.setText(after);
    }

    private void writeMultiline(XWPFRun run, String value) {
        if (value == null) value = "";
        String v = normalizeNewlines(value);

        if (!v.contains("\n")) {
            run.setText(v);
            return;
        }

        String[] lines = v.split("\n", -1);
        run.setText(lines.length > 0 ? lines[0] : "");

        for (int i = 1; i < lines.length; i++) {
            run.addBreak();
            run.setText(lines[i]);
        }
    }

    private void copyStyle(XWPFRun from, XWPFRun to) {
        if (from == null || to == null) return;
        to.getCTR().setRPr(from.getCTR().getRPr());
    }


}
