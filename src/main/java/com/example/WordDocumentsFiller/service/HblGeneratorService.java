package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.HblData;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HblGeneratorService {

    private static final String WORD_NS =
            "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'; ";

    public void generate(HblData data, String templatePathInResources, String outputPath) throws IOException {
        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(templatePathInResources);
        if (templateStream == null) {
            throw new IllegalArgumentException("Template not found in resources: " + templatePathInResources);
        }

        XWPFDocument document = new XWPFDocument(templateStream);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("{sellerName}", safe(data.getSellerName()));
        replacements.put("{sellerAddress}", safe(data.getSellerAddress()));
        replacements.put("{sellerTown}", safe(data.getSellerTown()));
        replacements.put("{sellerCountry}", safe(data.getSellerCountry()));
        replacements.put("{buyerName}", safe(data.getBuyerName()));
        replacements.put("{buyerAddress}", safe(data.getBuyerAddress()));
        replacements.put("{buyerTown}", safe(data.getBuyerTown()));
        replacements.put("{buyerCountry}", safe(data.getBuyerCountry()));
        replacements.put("{description}", safe(data.getDescription()));
        replacements.put("{vin}", safe(data.getVin()));
        replacements.put("{totalKgs}", safe(data.getTotalKgs()));
        replacements.put("{dateOnBoard}", safe(data.getDateOnBoard()));
        replacements.put("{dateOfIssue}", safe(data.getDateOfIssue()));

        replacements.put("{carrier}", safe(data.getCarrier()));
        replacements.put("{container}", safe(data.getContainer()));
        replacements.put("{kgs}", safe(data.getKgs()));
        replacements.put("{order}", safe(data.getOrder()));
        replacements.put("{pod}", safe(data.getPod()));
        replacements.put("{pol}", safe(data.getPol()));
        replacements.put("{seal}", safe(data.getSeal()));
        replacements.put("{term}", safe(data.getTerm()));

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceInParagraph(paragraph, replacements);
        }

        for (XWPFTable table : document.getTables()) {
            replaceInsideTable(table, replacements);
        }

        replaceInsideTextBoxes(document, replacements);

        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            document.write(outputStream);
        }
        document.close();
    }

    private void replaceInsideTable(XWPFTable table, Map<String, String> replacements) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    replaceInParagraph(paragraph, replacements);
                }
                for (XWPFTable nestedTable : cell.getTables()) {
                    replaceInsideTable(nestedTable, replacements);
                }
            }
        }
    }

    private void replaceInsideTextBoxes(XWPFDocument document, Map<String, String> replacements) {
        XmlObject[] paragraphs = document.getDocument().selectPath(WORD_NS + ".//w:txbxContent//w:p");
        for (XmlObject paragraphNode : paragraphs) {
            if (paragraphNode instanceof CTP ctp) {
                XWPFParagraph paragraph = new XWPFParagraph(ctp, document);
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    replacePlaceholderSplitRuns(paragraph, entry.getKey(), entry.getValue());
                }
            }
        }

        // Fallback for placeholders split in unusual XML structures inside text boxes.
        // We rebuild text from all w:t nodes in a paragraph and write back when changed.
        XmlObject[] textboxParagraphs = document.getDocument().selectPath(WORD_NS + ".//w:txbxContent//w:p");
        for (XmlObject paragraphNode : textboxParagraphs) {
            XmlObject[] tNodes = paragraphNode.selectPath(WORD_NS + ".//w:t");
            if (tNodes == null || tNodes.length == 0) {
                continue;
            }

            StringBuilder fullText = new StringBuilder();
            for (XmlObject tNode : tNodes) {
                try (XmlCursor cursor = tNode.newCursor()) {
                    String t = cursor.getTextValue();
                    fullText.append(t == null ? "" : t);
                }
            }

            String original = fullText.toString();
            if (original.isEmpty()) {
                continue;
            }

            String replaced = original;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                replaced = replaced.replace(entry.getKey(), entry.getValue());
            }

            if (!original.equals(replaced)) {
                try (XmlCursor first = tNodes[0].newCursor()) {
                    first.setTextValue(replaced);
                }
                for (int i = 1; i < tNodes.length; i++) {
                    try (XmlCursor rest = tNodes[i].newCursor()) {
                        rest.setTextValue("");
                    }
                }
            }
        }

        XmlObject[] textNodes = document.getDocument().selectPath(WORD_NS + ".//w:txbxContent//w:t");
        for (XmlObject textNode : textNodes) {
            try (XmlCursor cursor = textNode.newCursor()) {
                String original = cursor.getTextValue();
                if (original == null || original.isEmpty()) {
                    continue;
                }

                String replaced = original;
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    replaced = replaced.replace(entry.getKey(), entry.getValue());
                }

                if (!original.equals(replaced)) {
                    cursor.setTextValue(replaced);
                }
            }
        }
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
