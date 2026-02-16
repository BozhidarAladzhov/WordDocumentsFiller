package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.UpData;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UpGeneratorService {

    public void generate(UpData data, String templatePathInResources, String outputPath) throws IOException {
        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(templatePathInResources);
        if (templateStream == null) {
            throw new IllegalArgumentException("Template not found in resources: " + templatePathInResources);
        }

        XWPFDocument document = new XWPFDocument(templateStream);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("{buyerName}", safe(data.getBuyerName()));
        replacements.put("{buyerAddress}", safe(data.getBuyerAddress()));
        replacements.put("{buyerTown}", safe(data.getBuyerTown()));
        replacements.put("{buyerCountry}", safe(data.getBuyerCountry()));
        replacements.put("{EORI}", safe(data.getEori()));
        replacements.put("{MBL}", safe(data.getMbl()));
        replacements.put("{container}", safe(data.getContainer()));
        replacements.put("{description}", safe(data.getDescription()));
        replacements.put("{descriptionBG}", safe(data.getDescriptionBg()));
        replacements.put("{vin}", safe(data.getVin()));
        replacements.put("{date}", safe(data.getDate()));

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceInParagraph(paragraph, replacements);
        }

        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceInParagraph(paragraph, replacements);
                    }
                }
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            document.write(outputStream);
        }
        document.close();
    }

    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            replacePlaceholderSplitRuns(paragraph, entry.getKey(), entry.getValue());
        }
    }

    private void replacePlaceholderSplitRuns(XWPFParagraph paragraph, String placeholder, String value) {
        while (true) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs == null || runs.isEmpty()) {
                return;
            }

            StringBuilder fullText = new StringBuilder();
            for (XWPFRun run : runs) {
                String text = run.getText(0);
                fullText.append(text == null ? "" : text);
            }

            String paragraphText = fullText.toString();
            if (!paragraphText.contains(placeholder)) {
                return;
            }

            int placeholderLength = placeholder.length();
            int matchedChars = 0;
            int startRun = -1;
            int startOffset = -1;
            int endRun = -1;
            int endOffset = -1;

            for (int runIdx = 0; runIdx < runs.size(); runIdx++) {
                XWPFRun run = runs.get(runIdx);
                String runText = run.getText(0);
                if (runText == null) {
                    runText = "";
                }

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
                if (matchedChars == placeholderLength) {
                    break;
                }
            }

            if (matchedChars != placeholderLength) {
                return;
            }

            if (startRun == endRun) {
                String txt = runs.get(startRun).getText(0);
                if (txt == null) {
                    txt = "";
                }

                String before = txt.substring(0, startOffset);
                String after = txt.substring(endOffset + 1);

                runs.get(startRun).setText(before, 0);

                XWPFRun valueRun = paragraph.insertNewRun(startRun + 1);
                copyStyle(runs.get(startRun), valueRun);
                valueRun.setText(value);

                XWPFRun afterRun = paragraph.insertNewRun(startRun + 2);
                copyStyle(runs.get(startRun), afterRun);
                afterRun.setText(after);
                continue;
            }

            String startText = runs.get(startRun).getText(0);
            if (startText == null) {
                startText = "";
            }
            String before = startText.substring(0, startOffset);
            runs.get(startRun).setText(before, 0);

            String endText = runs.get(endRun).getText(0);
            if (endText == null) {
                endText = "";
            }
            String after = endText.substring(endOffset + 1);

            for (int i = endRun; i >= startRun + 1; i--) {
                paragraph.removeRun(i);
            }

            XWPFRun valueRun = paragraph.insertNewRun(startRun + 1);
            copyStyle(runs.get(startRun), valueRun);
            valueRun.setText(value);

            XWPFRun afterRun = paragraph.insertNewRun(startRun + 2);
            copyStyle(runs.get(startRun), afterRun);
            afterRun.setText(after);
        }
    }

    private void copyStyle(XWPFRun from, XWPFRun to) {
        if (from == null || to == null) {
            return;
        }
        to.getCTR().setRPr(from.getCTR().getRPr());
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
