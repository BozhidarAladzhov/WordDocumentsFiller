package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.OverdueDebtMailDraft;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverdueDebtsServiceTest {

    @Test
    void shouldBuildDraftsForCustomersWithDuePaymentTargetAndIncludeAllCustomerRows() {
        OverdueDebtsService service = new OverdueDebtsService();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Customer");
            header.createCell(1).setCellValue("Invoice no.");
            header.createCell(2).setCellValue("Invoice date");
            header.createCell(3).setCellValue("Outstanding amount");
            header.createCell(4).setCellValue("Currency");
            header.createCell(5).setCellValue("Payment target");
            header.createCell(6).setCellValue("email");

            addRow(sheet, 1, "Alpha Ltd", "1001", LocalDate.of(2026, 8, 1), 100.25, "EUR",
                    LocalDate.of(2026, 8, 20), "one@example.com; two@example.com");
            addRow(sheet, 2, "Alpha Ltd", "1002", LocalDate.of(2026, 8, 25), 50.00, "EUR",
                    LocalDate.of(2026, 9, 5), "one@example.com");
            addRow(sheet, 3, "Alpha Ltd", "1003", LocalDate.of(2026, 8, 26), 10.00, "EUR",
                    LocalDate.of(2026, 8, 28), "one@example.com");
            addRow(sheet, 4, "Beta Ltd", "2001", LocalDate.of(2026, 8, 25), 20.00, "EUR",
                    LocalDate.of(2026, 9, 5), "beta@example.com");
            addRow(sheet, 5, "Gamma Ltd", "3001", LocalDate.of(2026, 8, 10), 30.00, "EUR",
                    LocalDate.of(2026, 8, 15), "cars@freeline.bg");

            List<OverdueDebtMailDraft> drafts = service.buildDrafts(workbook, LocalDate.of(2026, 8, 28));

            assertEquals(2, drafts.size());
            OverdueDebtMailDraft draft = drafts.get(0);
            assertEquals("Alpha Ltd", draft.customer());
            assertEquals("one@example.com; two@example.com", draft.to());
            assertEquals(OverdueDebtsService.DEFAULT_CC, draft.cc());
            assertEquals(3, draft.lines().size());
            assertEquals("160.25", draft.totalsByCurrency().get("EUR").toPlainString());
            assertTrue(draft.body().contains("Invoice no. 1001 | Invoice date: 01.08.2026 | Payment target: 20.08.2026 | Amount: 100.25 EUR"));
            assertTrue(draft.body().contains("Invoice no. 1002 | Invoice date: 25.08.2026 | Payment target: 05.09.2026 | Amount: 50.00 EUR"));
            assertTrue(draft.bodyHtml().contains("Приложено изпращаме справка с неплатените фактури към 28.08.2026."));
            assertTrue(draft.bodyHtml().contains("<strong>Клиент: Alpha Ltd</strong>"));
            assertTrue(draft.bodyHtml().contains("Моля да прегледате дължимите суми към Фрилайн ООД"));
            assertTrue(draft.bodyHtml().contains("Благодаря предварително"));
            assertTrue(draft.bodyHtml().contains("<table"));
            assertTrue(draft.bodyHtml().contains("<td style=\"border:1px solid #b8b8b8; padding:6px 8px; text-align:left; color:#c00000; font-weight:700;\">1001</td>"));
            assertTrue(draft.bodyHtml().contains("<td style=\"border:1px solid #b8b8b8; padding:6px 8px; text-align:right; color:#c00000; font-weight:700;\">100.25</td>"));
            assertTrue(draft.bodyHtml().contains("<td style=\"border:1px solid #b8b8b8; padding:6px 8px; text-align:left;\">1003</td>"));
            assertTrue(draft.bodyHtml().contains("<strong>Общо:<br>160.25 EUR<br></strong>"));
            assertTrue(draft.bodyHtml().contains("160.25 EUR"));

            OverdueDebtMailDraft reviewDraft = drafts.get(1);
            assertEquals("Gamma Ltd", reviewDraft.customer());
            assertEquals("", reviewDraft.to());
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static void addRow(Sheet sheet, int rowIndex, String customer, String invoiceNo,
                               LocalDate invoiceDate, double amount, String currency,
                               LocalDate paymentTarget, String email) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(customer);
        row.createCell(1).setCellValue(invoiceNo);
        row.createCell(2).setCellValue(invoiceDate);
        row.createCell(3).setCellValue(amount);
        row.createCell(4).setCellValue(currency);
        row.createCell(5).setCellValue(paymentTarget);
        row.createCell(6).setCellValue(email);
    }
}
