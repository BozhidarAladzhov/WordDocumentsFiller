package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.OverdueDebtLine;
import com.example.WordDocumentsFiller.dto.OverdueDebtMailDraft;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OverdueDebtsService {

    public static final String DEFAULT_CC = "accounting@freeline.bg";

    private static final String HEADER_CUSTOMER = "Customer";
    private static final String HEADER_INVOICE_NO = "Invoice no.";
    private static final String HEADER_INVOICE_DATE = "Invoice date";
    private static final String HEADER_OUTSTANDING_AMOUNT = "Outstanding amount";
    private static final String HEADER_CURRENCY = "Currency";
    private static final String HEADER_PAYMENT_TARGET = "Payment target";
    private static final String HEADER_EMAIL = "email";

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final List<DateTimeFormatter> INPUT_DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy", Locale.US),
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US)
    );
    private static final Pattern EMAIL_SPLIT = Pattern.compile("[;,]");

    public List<OverdueDebtMailDraft> buildDrafts(MultipartFile file, LocalDate today) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ProcreditProcessingException("Избери входен Excel файл.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new ProcreditProcessingException("Файлът трябва да е .xlsx.");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            return buildDrafts(workbook, today == null ? LocalDate.now() : today);
        }
    }

    List<OverdueDebtMailDraft> buildDrafts(Workbook workbook, LocalDate today) {
        Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
        if (sheet == null) {
            throw new ProcreditProcessingException("Входният файл няма sheet-ове.");
        }

        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new ProcreditProcessingException("Входният файл няма header ред.");
        }

        Map<String, Integer> columns = mapColumns(headerRow);
        validateRequiredColumns(columns);

        Map<String, CustomerGroup> groups = new LinkedHashMap<>();
        for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isInputRowBlank(row, columns)) {
                continue;
            }

            String customer = requireValue(rowIndex, HEADER_CUSTOMER, readCellAsString(row.getCell(columns.get(HEADER_CUSTOMER))));
            String invoiceNo = requireValue(rowIndex, HEADER_INVOICE_NO, readCellAsString(row.getCell(columns.get(HEADER_INVOICE_NO))));
            BigDecimal amount = readRequiredAmount(rowIndex, row.getCell(columns.get(HEADER_OUTSTANDING_AMOUNT)));
            String currency = requireValue(rowIndex, HEADER_CURRENCY, readCellAsString(row.getCell(columns.get(HEADER_CURRENCY))));
            LocalDate paymentTarget = readRequiredDate(rowIndex, HEADER_PAYMENT_TARGET, row.getCell(columns.get(HEADER_PAYMENT_TARGET)));
            LocalDate invoiceDate = readOptionalDate(row.getCell(columns.get(HEADER_INVOICE_DATE)));
            String email = readCellAsString(row.getCell(columns.get(HEADER_EMAIL)));

            OverdueDebtLine line = new OverdueDebtLine(
                    rowIndex + 1,
                    customer,
                    invoiceNo,
                    invoiceDate,
                    amount,
                    currency,
                    paymentTarget,
                    email
            );

            CustomerGroup group = groups.computeIfAbsent(customer, CustomerGroup::new);
            group.lines.add(line);
            parseEmails(email).forEach(group.emails::add);
            if (!paymentTarget.isAfter(today)) {
                group.hasDueLine = true;
            }
        }

        return groups.values().stream()
                .filter(group -> group.hasDueLine)
                .sorted(Comparator.comparing(group -> group.customer.toLowerCase(Locale.ROOT)))
                .map(group -> toDraft(group, today))
                .toList();
    }

    private OverdueDebtMailDraft toDraft(CustomerGroup group, LocalDate today) {
        List<OverdueDebtLine> sortedLines = group.lines.stream()
                .sorted(Comparator
                        .comparing(OverdueDebtLine::paymentTarget)
                        .thenComparing(OverdueDebtLine::invoiceNo))
                .toList();

        Map<String, BigDecimal> totalsByCurrency = sortedLines.stream()
                .collect(Collectors.groupingBy(
                        OverdueDebtLine::currency,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, OverdueDebtLine::outstandingAmount, BigDecimal::add)
                ));

        return new OverdueDebtMailDraft(
                UUID.randomUUID().toString(),
                group.customer,
                String.join("; ", group.emails),
                DEFAULT_CC,
                "Справка неплатени фактури към " + today.format(DISPLAY_DATE) + " " + group.customer,
                buildBody(group.customer, sortedLines, totalsByCurrency, today),
                buildHtmlBody(group.customer, sortedLines, totalsByCurrency, today),
                sortedLines,
                totalsByCurrency
        );
    }

    private String buildBody(String customer, List<OverdueDebtLine> lines,
                             Map<String, BigDecimal> totalsByCurrency, LocalDate today) {
        List<String> out = new ArrayList<>();
        out.add("Здравейте,");
        out.add("");
        out.add("Приложено изпращаме справка с неплатените фактури към " + today.format(DISPLAY_DATE) + ".");
        out.add("Моля да прегледате дължимите суми към Фрилайн ООД и да ни информирате кога можем да очакваме плащане по просрочените фактури.");
        out.add("Клиент: " + customer);
        out.add("");
        for (OverdueDebtLine line : lines) {
            out.add("Invoice no. " + line.invoiceNo()
                    + " | Invoice date: " + formatDate(line.invoiceDate())
                    + " | Payment target: " + formatDate(line.paymentTarget())
                    + " | Amount: " + formatAmount(line.outstandingAmount()) + " " + line.currency());
        }
        out.add("");
        out.add("Общо:");
        totalsByCurrency.forEach((currency, total) ->
                out.add(formatAmount(total) + " " + currency));
        out.add("");
        out.add("Благодаря предварително!");
        return String.join("\n", out);
    }

    private String buildHtmlBody(String customer, List<OverdueDebtLine> lines,
                                 Map<String, BigDecimal> totalsByCurrency, LocalDate today) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Здравейте,</p>");
        html.append("<p>Приложено изпращаме справка с неплатените фактури към ")
                .append(today.format(DISPLAY_DATE))
                .append(".</p>");
        html.append("<p>Моля да прегледате дължимите суми към Фрилайн ООД и да ни информирате кога можем да очакваме плащане по просрочените фактури.</p>");
        html.append("<p><strong>Клиент: ").append(escapeHtml(customer)).append("</strong></p>");
        html.append("""
                <table style="border-collapse:collapse; margin:12px 0; font-family:'Verdana Pro', Verdana, Arial, sans-serif; font-size:10pt;">
                  <thead>
                    <tr>
                      <th style="border:1px solid #b8b8b8; padding:6px 8px; background:#f2f2f2; text-align:left;">Invoice no.</th>
                      <th style="border:1px solid #b8b8b8; padding:6px 8px; background:#f2f2f2; text-align:left;">Invoice date</th>
                      <th style="border:1px solid #b8b8b8; padding:6px 8px; background:#f2f2f2; text-align:left;">Payment target</th>
                      <th style="border:1px solid #b8b8b8; padding:6px 8px; background:#f2f2f2; text-align:right;">Outstanding amount</th>
                      <th style="border:1px solid #b8b8b8; padding:6px 8px; background:#f2f2f2; text-align:left;">Currency</th>
                    </tr>
                  </thead>
                  <tbody>
                """);
        for (OverdueDebtLine line : lines) {
            boolean highlightOverdue = isOverdueForHighlight(line, today);
            html.append("<tr>")
                    .append(tableCell(line.invoiceNo(), "left", highlightOverdue))
                    .append(tableCell(formatDate(line.invoiceDate()), "left", highlightOverdue))
                    .append(tableCell(formatDate(line.paymentTarget()), "left", highlightOverdue))
                    .append(tableCell(formatAmount(line.outstandingAmount()), "right", highlightOverdue))
                    .append(tableCell(line.currency(), "left", highlightOverdue))
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("<p><strong>Общо:<br>");
        totalsByCurrency.forEach((currency, total) ->
                html.append(escapeHtml(formatAmount(total))).append(" ").append(escapeHtml(currency)).append("<br>"));
        html.append("</strong></p>");
        html.append("<p>Благодаря предварително!</p>");
        return html.toString();
    }

    private boolean isOverdueForHighlight(OverdueDebtLine line, LocalDate today) {
        return line.paymentTarget() != null && line.paymentTarget().isBefore(today);
    }

    private String tableCell(String value, String align, boolean highlightOverdue) {
        String emphasis = highlightOverdue ? " color:#c00000; font-weight:700;" : "";
        return "<td style=\"border:1px solid #b8b8b8; padding:6px 8px; text-align:" + align + ";" + emphasis + "\">" +
                escapeHtml(value) + "</td>";
    }

    private Map<String, Integer> mapColumns(Row headerRow) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String value = readCellAsString(cell);
            if (!value.isBlank()) {
                columns.put(value.trim(), cell.getColumnIndex());
            }
        }
        return columns;
    }

    private void validateRequiredColumns(Map<String, Integer> columns) {
        List<String> required = List.of(
                HEADER_CUSTOMER,
                HEADER_INVOICE_NO,
                HEADER_INVOICE_DATE,
                HEADER_OUTSTANDING_AMOUNT,
                HEADER_CURRENCY,
                HEADER_PAYMENT_TARGET,
                HEADER_EMAIL
        );
        List<String> missing = required.stream()
                .filter(column -> !columns.containsKey(column))
                .toList();
        if (!missing.isEmpty()) {
            throw new ProcreditProcessingException("Липсват колони във входния файл: " + String.join(", ", missing));
        }
    }

    private boolean isInputRowBlank(Row row, Map<String, Integer> columns) {
        for (String header : List.of(HEADER_CUSTOMER, HEADER_INVOICE_NO, HEADER_OUTSTANDING_AMOUNT,
                HEADER_CURRENCY, HEADER_PAYMENT_TARGET, HEADER_EMAIL)) {
            if (!readCellAsString(row.getCell(columns.get(header))).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String requireValue(int zeroBasedRowIndex, String fieldName, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new ProcreditProcessingException("Липсва стойност в колона '" + fieldName +
                    "' на ред " + (zeroBasedRowIndex + 1) + ".");
        }
        return trimmed;
    }

    private BigDecimal readRequiredAmount(int zeroBasedRowIndex, Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new ProcreditProcessingException("Липсва стойност в колона '" + HEADER_OUTSTANDING_AMOUNT +
                    "' на ред " + (zeroBasedRowIndex + 1) + ".");
        }
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case FORMULA -> BigDecimal.valueOf(cell.getNumericCellValue());
                default -> parseAmount(readCellAsString(cell));
            };
        } catch (NumberFormatException ex) {
            throw new ProcreditProcessingException("Невалидна сума в колона '" + HEADER_OUTSTANDING_AMOUNT +
                    "' на ред " + (zeroBasedRowIndex + 1) + ".");
        }
    }

    private BigDecimal parseAmount(String value) {
        String normalized = value == null ? "" : value.trim().replace(" ", "").replace(",", ".");
        if (normalized.isBlank()) {
            throw new NumberFormatException("Blank amount");
        }
        return new BigDecimal(normalized);
    }

    private LocalDate readRequiredDate(int zeroBasedRowIndex, String fieldName, Cell cell) {
        LocalDate date = readOptionalDate(cell);
        if (date == null) {
            throw new ProcreditProcessingException("Липсва или е невалидна дата в колона '" + fieldName +
                    "' на ред " + (zeroBasedRowIndex + 1) + ".");
        }
        return date;
    }

    private LocalDate readOptionalDate(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
        }

        String value = readCellAsString(cell);
        if (value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : INPUT_DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next known export format.
            }
        }
        return null;
    }

    private List<String> parseEmails(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return EMAIL_SPLIT.splitAsStream(value)
                .map(String::trim)
                .filter(this::isExternalCorrespondenceEmail)
                .distinct()
                .toList();
    }

    private boolean isExternalCorrespondenceEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int atIndex = normalized.lastIndexOf('@');
        if (atIndex < 0 || atIndex == normalized.length() - 1) {
            return false;
        }
        String domain = normalized.substring(atIndex + 1);
        return !domain.equals("freeline.bg") && !domain.endsWith(".freeline.bg");
    }

    private String readCellAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return new DataFormatter(Locale.US).formatCellValue(cell).trim();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE);
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static final class CustomerGroup {
        private final String customer;
        private final List<OverdueDebtLine> lines = new ArrayList<>();
        private final LinkedHashSet<String> emails = new LinkedHashSet<>();
        private boolean hasDueLine;

        private CustomerGroup(String customer) {
            this.customer = customer;
        }
    }
}
