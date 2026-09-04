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
            header.createCell(6).setCellValue("Customer email");
            header.createCell(7).setCellValue("BP country");
            header.createCell(8).setCellValue("Freeline user");

            addRow(sheet, 1, "Alpha Ltd", "1001", LocalDate.of(2026, 8, 1), 100.25, "EUR",
                    LocalDate.of(2026, 8, 20), "ONE@example.com; two@example.com; one@example.com; ", "BG",
                    "slavomira@freeline.bg");
            addRow(sheet, 2, "Alpha Ltd", "1002", LocalDate.of(2026, 8, 25), 50.00, "EUR",
                    LocalDate.of(2026, 9, 5), "one@example.com", "BG", "simo@freeline.bg");
            addRow(sheet, 3, "Alpha Ltd", "1003", LocalDate.of(2026, 8, 26), 10.00, "EUR",
                    LocalDate.of(2026, 8, 28), "two@example.com", "BG", "simo@freeline.bg");
            addRow(sheet, 4, "Beta Ltd", "2001", LocalDate.of(2026, 8, 25), 20.00, "EUR",
                    LocalDate.of(2026, 9, 5), "beta@example.com", "BG", "polya@freeline.bg");
            addRow(sheet, 5, "Gamma Ltd", "3001", LocalDate.of(2026, 8, 10), 30.00, "EUR",
                    LocalDate.of(2026, 8, 15), "cars@freeline.bg; accounting@freeline.bg", "BG", "slavomira@freeline.bg");
            addRow(sheet, 6, "Zeta SA", "4001", LocalDate.of(2026, 8, 10), 70.00, "EUR",
                    LocalDate.of(2026, 6, 28), "zeta@example.com; zeta@example.com", "GR",
                    "documentatition@freeline.bg");

            List<OverdueDebtMailDraft> drafts = service.buildDrafts(workbook, LocalDate.of(2026, 8, 28));

            assertEquals(4, drafts.size());

            OverdueDebtMailDraft draft = drafts.get(0);
            assertEquals("Alpha Ltd", draft.customer());
            assertEquals("one@example.com; two@example.com", draft.to());
            assertEquals("accounting@freeline.bg; slavomira@freeline.bg; simo@freeline.bg", draft.cc());
            assertEquals("Справка неплатени фактури към 28.08.2026 Alpha Ltd", draft.subject());
            assertEquals(3, draft.lines().size());
            assertEquals("160.25", draft.totalsByCurrency().get("EUR").toPlainString());
            assertTrue(draft.body().contains("Invoice no. 1001 | Invoice date: 01.08.2026 | Amount: 100.25 EUR | Payment target: 20.08.2026"));
            assertTrue(draft.body().contains("Invoice no. 1002 | Invoice date: 25.08.2026 | Amount: 50.00 EUR | Payment target: 05.09.2026"));
            assertTrue(draft.bodyHtml().contains("Приложено изпращаме справка с неплатените фактури към 28.08.2026."));
            assertTrue(draft.bodyHtml().contains("<strong>Клиент: Alpha Ltd</strong>"));
            assertTrue(draft.bodyHtml().contains("Моля да прегледате дължимите суми към Фрилайн ООД"));
            assertTrue(draft.bodyHtml().contains("Благодаря предварително"));
            assertTrue(draft.bodyHtml().contains("<table"));
            assertTrue(draft.bodyHtml().indexOf("Currency</th>") < draft.bodyHtml().indexOf("Payment target</th>"));
            assertTrue(draft.bodyHtml().contains("font-size:8pt"));
            assertTrue(draft.bodyHtml().contains("mso-padding-alt:1px 3px 1px 3px"));
            assertTrue(draft.bodyHtml().contains("<td style=\"border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; text-align:left; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px; color:#c00000; font-weight:700;\">1001</td>"));
            assertTrue(draft.bodyHtml().contains("<td style=\"border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; text-align:right; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px; color:#c00000; font-weight:700;\">100.25</td>"));
            assertTrue(draft.bodyHtml().contains("<td style=\"border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; text-align:left; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px;\">1003</td>"));
            assertTrue(draft.bodyHtml().contains("<strong>Общо:<br>160.25 EUR<br></strong>"));

            OverdueDebtMailDraft reviewDraft = drafts.get(1);
            assertEquals("Gamma Ltd", reviewDraft.customer());
            assertEquals("", reviewDraft.to());
            assertEquals("accounting@freeline.bg; slavomira@freeline.bg", reviewDraft.cc());

            OverdueDebtMailDraft englishDraft = drafts.get(2);
            assertEquals("Zeta SA", englishDraft.customer());
            assertEquals("zeta@example.com", englishDraft.to());
            assertEquals("accounting@freeline.bg; documentatition@freeline.bg", englishDraft.cc());
            assertEquals("Unpaid invoices statement as of 28.08.2026 Zeta SA", englishDraft.subject());
            assertTrue(englishDraft.bodyHtml().contains("Please find below a statement of unpaid invoices as of 28.08.2026."));
            assertTrue(englishDraft.bodyHtml().contains("<strong>Customer: Zeta SA</strong>"));
            assertTrue(englishDraft.bodyHtml().contains("<strong>Total:<br>70.00 EUR<br></strong>"));
            assertTrue(englishDraft.bodyHtml().contains("Thank you in advance!"));

            OverdueDebtMailDraft velinDraft = drafts.get(3);
            assertEquals("Overdue > 60 days", velinDraft.customer());
            assertEquals("velin@freeline.bg", velinDraft.to());
            assertEquals("", velinDraft.cc());
            assertEquals("Просрочени задължения към Фрилайн повече от 60 дни.", velinDraft.subject());
            assertEquals(1, velinDraft.lines().size());
            assertTrue(velinDraft.bodyHtml().startsWith("<table"));
            assertTrue(velinDraft.bodyHtml().indexOf("Customer</th>") < velinDraft.bodyHtml().indexOf("Invoice no.</th>"));
            assertTrue(velinDraft.bodyHtml().indexOf("Payment target</th>") < velinDraft.bodyHtml().indexOf("Freeline user</th>"));
            assertTrue(velinDraft.bodyHtml().contains(">Zeta SA</td>"));
            assertTrue(velinDraft.bodyHtml().contains(">documentatition@freeline.bg</td>"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static void addRow(Sheet sheet, int rowIndex, String customer, String invoiceNo,
                               LocalDate invoiceDate, double amount, String currency,
                               LocalDate paymentTarget, String customerEmail, String bpCountry,
                               String freelineUser) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(customer);
        row.createCell(1).setCellValue(invoiceNo);
        row.createCell(2).setCellValue(invoiceDate);
        row.createCell(3).setCellValue(amount);
        row.createCell(4).setCellValue(currency);
        row.createCell(5).setCellValue(paymentTarget);
        row.createCell(6).setCellValue(customerEmail);
        row.createCell(7).setCellValue(bpCountry);
        row.createCell(8).setCellValue(freelineUser);
    }
}
