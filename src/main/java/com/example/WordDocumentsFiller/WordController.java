package com.example.WordDocumentsFiller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WordController {

    private Map<String, String> getPlaceholderMap(String email, String date, String fullName, String vehicle,
                                                  String domesticTransport, String pickUpAddress, String portOfLoading,
                                                  String oceanFreight, String portOfDelivery, String electricCar,
                                                  String inlandTransport, String validDate) {
        Map<String, String> values = new HashMap<>();
        values.put("${email}", email);
        values.put("${date}", date);
        values.put("${fullName}", fullName);
        values.put("${vehicle}", vehicle);
        values.put("${domesticTransport}", domesticTransport);
        values.put("${pickUpAddress}", pickUpAddress);
        values.put("${portOfLoading}", portOfLoading);
        values.put("${oceanFreight}", oceanFreight);
        values.put("${portOfDelivery}", portOfDelivery);
        values.put("${electricCar}", electricCar);
        values.put("${inlandTransport}", inlandTransport);
        values.put("${validDate}", validDate);
        return values;
    }

    @GetMapping("/")
    public String showForm() {
        return "form";
    }

    @PostMapping("/generate")
    public void generateWord(
            @RequestParam String email,
            @RequestParam String date,
            @RequestParam String fullName,
            @RequestParam String vehicle,
            @RequestParam String domesticTransport,
            @RequestParam String pickUpAddress,
            @RequestParam String portOfLoading,
            @RequestParam String oceanFreight,
            @RequestParam String portOfDelivery,
            @RequestParam String electricCar,
            @RequestParam String inlandTransport,
            @RequestParam String validDate,
            HttpServletResponse response) throws IOException {

        InputStream templateStream = getClass().getResourceAsStream("/BG_LCL_offer_US_NL_BG_electric_blank.docx");
        XWPFDocument doc = new XWPFDocument(templateStream);

        Map<String, String> values = getPlaceholderMap(email, date, fullName, vehicle, domesticTransport,
                pickUpAddress, portOfLoading, oceanFreight, portOfDelivery, electricCar, inlandTransport, validDate);

        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs == null || runs.isEmpty()) continue;

            StringBuilder fullText = new StringBuilder();
            Map<Integer, XWPFRun> runIndexMap = new HashMap<>();

            for (int i = 0; i < runs.size(); i++) {
                String text = runs.get(i).getText(0);
                if (text != null) {
                    runIndexMap.put(fullText.length(), runs.get(i));
                    fullText.append(text);
                }
            }

            String paragraphText = fullText.toString();

            for (Map.Entry<String, String> entry : values.entrySet()) {
                String placeholder = entry.getKey();
                String replacement = entry.getValue();

                int startIndex = paragraphText.indexOf(placeholder);
                if (startIndex >= 0) {
                    int endIndex = startIndex + placeholder.length();

                    // Определяне на run-овете, които трябва да изтрием
                    int runStart = -1, runEnd = -1;
                    for (int pos : runIndexMap.keySet()) {
                        if (pos <= startIndex) runStart = pos;
                        if (pos < endIndex) runEnd = pos;
                    }

                    int removeFrom = runs.indexOf(runIndexMap.get(runStart));
                    int removeTo = runs.indexOf(runIndexMap.get(runEnd));
                    for (int i = removeTo; i >= removeFrom; i--) {
                        paragraph.removeRun(i);
                    }

                    // Добавяне на текст преди плейсхолдъра (ако има)
                    if (startIndex > 0) {
                        XWPFRun before = paragraph.insertNewRun(removeFrom);
                        before.setText(paragraphText.substring(0, startIndex));
                        copyRunStyle(runIndexMap.get(runStart), before);
                    }

                    // Добавяне на заместващия текст със стил
                    XWPFRun replacedRun = paragraph.insertNewRun(removeFrom + 1);
                    replacedRun.setText(replacement);
                    copyRunStyle(runIndexMap.get(runStart), replacedRun);

                    // Добавяне на текст след плейсхолдъра (ако има)
                    if (endIndex < paragraphText.length()) {
                        XWPFRun after = paragraph.insertNewRun(removeFrom + 2);
                        after.setText(paragraphText.substring(endIndex));
                        copyRunStyle(runIndexMap.get(runStart), after);
                    }

                    break; // Прекъсваме, защото сме променили параграфа
                }
            }
        }

        // Изпращаме документа на клиента
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=generated.docx");
        doc.write(response.getOutputStream());
        doc.close();
    }

    // Метод за копиране на стилове от един run към друг
    private void copyRunStyle(XWPFRun sourceRun, XWPFRun targetRun) {
        targetRun.setBold(sourceRun.isBold());
        targetRun.setItalic(sourceRun.isItalic());
        targetRun.setUnderline(sourceRun.getUnderline());
        targetRun.setColor(sourceRun.getColor());
        targetRun.setFontFamily(sourceRun.getFontFamily());
        targetRun.setFontSize(sourceRun.getFontSize());
        targetRun.setTextPosition(sourceRun.getTextPosition());
        targetRun.setStrikeThrough(sourceRun.isStrikeThrough());

    }
}
