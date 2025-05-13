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

@Controller
public class WordController {


    @GetMapping("/")
    public String showForm() {
        return "form";
    }


    @PostMapping("/generate")
    public void generateWord(
            @RequestParam String name,
            @RequestParam String date,
            HttpServletResponse response) throws IOException{

        // Зареждаме шаблона
        InputStream templateStream = getClass().getResourceAsStream("/template.docx");
        XWPFDocument doc = new XWPFDocument(templateStream);

        // Замяна на плейсхолдъри
        for (XWPFParagraph p : doc.getParagraphs()){
            StringBuilder fullText = new StringBuilder();
            for (XWPFRun run: p.getRuns()){
                fullText.append(run.getText(0));
            }

            String replaced = fullText.toString()
                    .replace("${name}", name)
                    .replace("${date}", date);

            // Изчистване на старите runs
            int runCount = p.getRuns().size();
            for (int i = runCount - 1; i >= 0; i--) {
                p.removeRun(i);
            }

            // Добавяне на нов run с редактиран текст
            XWPFRun newRun = p.createRun();
            newRun.setText(replaced);
        }

        // Настройка за изтегляне
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=generated.docx");
        doc.write(response.getOutputStream());
        doc.close();

    }
}
