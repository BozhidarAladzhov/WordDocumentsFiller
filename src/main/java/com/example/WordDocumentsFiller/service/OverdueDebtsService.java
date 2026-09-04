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
import java.time.temporal.ChronoUnit;
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
    public static final String LONG_OVERDUE_CC = "velin@freeline.bg";

    private static final String HEADER_CUSTOMER = "Customer";
    private static final String HEADER_INVOICE_NO = "Invoice no.";
    private static final String HEADER_INVOICE_DATE = "Invoice date";
    private static final String HEADER_OUTSTANDING_AMOUNT = "Outstanding amount";
    private static final String HEADER_CURRENCY = "Currency";
    private static final String HEADER_PAYMENT_TARGET = "Payment target";
    private static final String HEADER_EMAIL = "email";
    private static final String HEADER_CUSTOMER_EMAIL = "Customer email";
    private static final String HEADER_BP_COUNTRY = "BP country";
    private static final String HEADER_FREELINE_USER = "Freeline user";

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
            String email = readCellAsString(row.getCell(requiredColumn(columns, HEADER_CUSTOMER_EMAIL, HEADER_EMAIL)));
            String bpCountry = readOptionalColumn(row, columns, HEADER_BP_COUNTRY);
            String freelineUser = readOptionalColumn(row, columns, HEADER_FREELINE_USER);

            OverdueDebtLine line = new OverdueDebtLine(
                    rowIndex + 1,
                    customer,
                    invoiceNo,
                    invoiceDate,
                    amount,
                    currency,
                    paymentTarget,
                    email,
                    String.join("; ", parseEmails(freelineUser))
            );

            CustomerGroup group = groups.computeIfAbsent(customer, CustomerGroup::new);
            group.lines.add(line);
            parseCustomerEmails(email).forEach(group.emails::add);
            parseEmails(freelineUser).forEach(group.freelineUsers::add);
            if (!bpCountry.isBlank()) {
                group.countries.add(bpCountry.trim().toUpperCase(Locale.ROOT));
            }
            if (!paymentTarget.isAfter(today)) {
                group.hasDueLine = true;
            }
        }

        List<CustomerGroup> dueGroups = groups.values().stream()
                .filter(group -> group.hasDueLine)
                .sorted(Comparator.comparing(group -> group.customer.toLowerCase(Locale.ROOT)))
                .toList();
        List<OverdueDebtMailDraft> drafts = new ArrayList<>(dueGroups.stream()
                .map(group -> toDraft(group, today))
                .toList());
        buildLongOverdueDraft(dueGroups, today).ifPresent(drafts::add);
        return drafts;
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

        boolean english = group.isForeignCustomer();
        return new OverdueDebtMailDraft(
                UUID.randomUUID().toString(),
                group.customer,
                String.join("; ", group.emails),
                buildCc(group),
                subject(group.customer, english, today),
                buildBody(group.customer, sortedLines, totalsByCurrency, english, today),
                buildHtmlBody(group.customer, sortedLines, totalsByCurrency, english, today),
                sortedLines,
                totalsByCurrency
        );
    }

    private String buildBody(String customer, List<OverdueDebtLine> lines,
                             Map<String, BigDecimal> totalsByCurrency, boolean english, LocalDate today) {
        List<String> out = new ArrayList<>();
        out.add(english ? "Hello," : "Здравейте,");
        out.add("");
        if (english) {
            out.add("Please find below a statement of unpaid invoices as of " + today.format(DISPLAY_DATE) + ".");
            out.add("Please review the outstanding amounts to Freeline Ltd and let us know when we can expect payment for the overdue invoices.");
            out.add("Customer: " + customer);
        } else {
            out.add("Приложено изпращаме справка с неплатените фактури към " + today.format(DISPLAY_DATE) + ".");
            out.add("Моля да прегледате дължимите суми към Фрилайн ООД и да ни информирате кога можем да очакваме плащане по просрочените фактури.");
            out.add("Клиент: " + customer);
        }
        out.add("");
        for (OverdueDebtLine line : lines) {
            out.add("Invoice no. " + line.invoiceNo()
                    + " | Invoice date: " + formatDate(line.invoiceDate())
                    + " | Amount: " + formatAmount(line.outstandingAmount()) + " " + line.currency()
                    + " | Payment target: " + formatDate(line.paymentTarget()));
        }
        out.add("");
        out.add(english ? "Total:" : "Общо:");
        totalsByCurrency.forEach((currency, total) ->
                out.add(formatAmount(total) + " " + currency));
        out.add("");
        out.add(english ? "Thank you in advance!" : "Благодаря предварително!");
        return String.join("\n", out);
    }

    private String buildHtmlBody(String customer, List<OverdueDebtLine> lines,
                                 Map<String, BigDecimal> totalsByCurrency, boolean english, LocalDate today) {
        StringBuilder html = new StringBuilder();
        html.append("<p>").append(english ? "Hello," : "Здравейте,").append("</p>");
        if (english) {
            html.append("<p>Please find below a statement of unpaid invoices as of ")
                    .append(today.format(DISPLAY_DATE))
                    .append(".</p>");
            html.append("<p>Please review the outstanding amounts to Freeline Ltd and let us know when we can expect payment for the overdue invoices.</p>");
            html.append("<p><strong>Customer: ").append(escapeHtml(customer)).append("</strong></p>");
        } else {
            html.append("<p>Приложено изпращаме справка с неплатените фактури към ")
                    .append(today.format(DISPLAY_DATE))
                    .append(".</p>");
            html.append("<p>Моля да прегледате дължимите суми към Фрилайн ООД и да ни информирате кога можем да очакваме плащане по просрочените фактури.</p>");
            html.append("<p><strong>Клиент: ").append(escapeHtml(customer)).append("</strong></p>");
        }
        html.append("""
                <table cellpadding="0" cellspacing="0" style="border-collapse:collapse; margin:8px 0; table-layout:auto; width:auto; font-family:'Verdana Pro', Verdana, Arial, sans-serif; font-size:8pt; line-height:1; mso-line-height-rule:exactly;">
                  <thead>
                    <tr>
                      <th style="border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; background:#f2f2f2; text-align:left; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px;">Invoice no.</th>
                      <th style="border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; background:#f2f2f2; text-align:left; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px;">Invoice date</th>
                      <th style="border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; background:#f2f2f2; text-align:right; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px;">Outstanding amount</th>
                      <th style="border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; background:#f2f2f2; text-align:left; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px;">Currency</th>
                      <th style="border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; background:#f2f2f2; text-align:left; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px;">Payment target</th>
                    </tr>
                  </thead>
                  <tbody>
                """);
        for (OverdueDebtLine line : lines) {
            boolean highlightOverdue = isOverdueForHighlight(line, today);
            html.append("<tr>")
                    .append(tableCell(line.invoiceNo(), "left", highlightOverdue))
                    .append(tableCell(formatDate(line.invoiceDate()), "left", highlightOverdue))
                    .append(tableCell(formatAmount(line.outstandingAmount()), "right", highlightOverdue))
                    .append(tableCell(line.currency(), "left", highlightOverdue))
                    .append(tableCell(formatDate(line.paymentTarget()), "left", highlightOverdue))
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        html.append("<p><strong>").append(english ? "Total:" : "Общо:").append("<br>");
        totalsByCurrency.forEach((currency, total) ->
                html.append(escapeHtml(formatAmount(total))).append(" ").append(escapeHtml(currency)).append("<br>"));
        html.append("</strong></p>");
        html.append("<p>").append(english ? "Thank you in advance!" : "Благодаря предварително!").append("</p>");
        return html.toString();
    }

    private String subject(String customer, boolean english, LocalDate today) {
        if (english) {
            return "Unpaid invoices statement as of " + today.format(DISPLAY_DATE) + " " + customer;
        }
        return "Справка неплатени фактури към " + today.format(DISPLAY_DATE) + " " + customer;
    }

    private String buildCc(CustomerGroup group) {
        LinkedHashSet<String> cc = new LinkedHashSet<>();
        cc.add(DEFAULT_CC);
        cc.addAll(group.freelineUsers);
        return String.join("; ", cc);
    }

    private java.util.Optional<OverdueDebtMailDraft> buildLongOverdueDraft(List<CustomerGroup> groups, LocalDate today) {
        List<OverdueDebtLine> longOverdueLines = groups.stream()
                .flatMap(group -> group.lines.stream())
                .filter(line -> isLongOverdue(line.paymentTarget(), today))
                .sorted(Comparator
                        .comparing(OverdueDebtLine::customer, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(OverdueDebtLine::paymentTarget)
                        .thenComparing(OverdueDebtLine::invoiceNo))
                .toList();
        if (longOverdueLines.isEmpty()) {
            return java.util.Optional.empty();
        }

        Map<String, BigDecimal> totalsByCurrency = totalsByCurrency(longOverdueLines);
        return java.util.Optional.of(new OverdueDebtMailDraft(
                UUID.randomUUID().toString(),
                "Overdue > 60 days",
                LONG_OVERDUE_CC,
                "",
                "Просрочени задължения към Фрилайн повече от 60 дни.",
                buildLongOverdueBody(longOverdueLines, totalsByCurrency, today),
                buildLongOverdueHtmlBody(longOverdueLines, totalsByCurrency, today),
                longOverdueLines,
                totalsByCurrency
        ));
    }

    private Map<String, BigDecimal> totalsByCurrency(List<OverdueDebtLine> lines) {
        return lines.stream()
                .collect(Collectors.groupingBy(
                        OverdueDebtLine::currency,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, OverdueDebtLine::outstandingAmount, BigDecimal::add)
                ));
    }

    private String buildLongOverdueBody(List<OverdueDebtLine> lines,
                                        Map<String, BigDecimal> totalsByCurrency, LocalDate today) {
        List<String> out = new ArrayList<>();
        for (OverdueDebtLine line : lines) {
            out.add("Customer: " + line.customer()
                    + " | Invoice no. " + line.invoiceNo()
                    + " | Invoice date: " + formatDate(line.invoiceDate())
                    + " | Amount: " + formatAmount(line.outstandingAmount()) + " " + line.currency()
                    + " | Payment target: " + formatDate(line.paymentTarget())
                    + " | Freeline user: " + line.freelineUser());
        }
        out.add("");
        out.add("Total:");
        totalsByCurrency.forEach((currency, total) -> out.add(formatAmount(total) + " " + currency));
        return String.join("\n", out);
    }

    private String buildLongOverdueHtmlBody(List<OverdueDebtLine> lines,
                                            Map<String, BigDecimal> totalsByCurrency, LocalDate today) {
        StringBuilder html = new StringBuilder();
        appendInvoiceTable(html, lines, today, true, true);
        html.append("<p><strong>Total:<br>");
        totalsByCurrency.forEach((currency, total) ->
                html.append(escapeHtml(formatAmount(total))).append(" ").append(escapeHtml(currency)).append("<br>"));
        html.append("</strong></p>");
        return html.toString();
    }

    private void appendInvoiceTable(StringBuilder html, List<OverdueDebtLine> lines, LocalDate today,
                                    boolean includeCustomer, boolean includeFreelineUser) {
        html.append("""
                <table cellpadding="0" cellspacing="0" style="border-collapse:collapse; margin:8px 0; table-layout:auto; width:auto; font-family:'Verdana Pro', Verdana, Arial, sans-serif; font-size:8pt; line-height:1; mso-line-height-rule:exactly;">
                  <thead>
                    <tr>
                """);
        if (includeCustomer) {
            html.append(headerCell("Customer", "left"));
        }
        html.append(headerCell("Invoice no.", "left"));
        html.append(headerCell("Invoice date", "left"));
        html.append(headerCell("Outstanding amount", "right"));
        html.append(headerCell("Currency", "left"));
        html.append(headerCell("Payment target", "left"));
        if (includeFreelineUser) {
            html.append(headerCell("Freeline user", "left"));
        }
        html.append("""
                    </tr>
                  </thead>
                  <tbody>
                """);
        for (OverdueDebtLine line : lines) {
            boolean highlightOverdue = isOverdueForHighlight(line, today);
            html.append("<tr>");
            if (includeCustomer) {
                html.append(tableCell(line.customer(), "left", highlightOverdue));
            }
            html.append(tableCell(line.invoiceNo(), "left", highlightOverdue))
                    .append(tableCell(formatDate(line.invoiceDate()), "left", highlightOverdue))
                    .append(tableCell(formatAmount(line.outstandingAmount()), "right", highlightOverdue))
                    .append(tableCell(line.currency(), "left", highlightOverdue))
                    .append(tableCell(formatDate(line.paymentTarget()), "left", highlightOverdue));
            if (includeFreelineUser) {
                html.append(tableCell(line.freelineUser(), "left", highlightOverdue));
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private String headerCell(String value, String align) {
        return "<th style=\"border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; background:#f2f2f2; text-align:" + align + "; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px;\">" +
                escapeHtml(value) + "</th>";
    }

    private boolean isLongOverdue(LocalDate paymentTarget, LocalDate today) {
        return paymentTarget != null && ChronoUnit.DAYS.between(paymentTarget, today) > 60;
    }

    private boolean isOverdueForHighlight(OverdueDebtLine line, LocalDate today) {
        return line.paymentTarget() != null && line.paymentTarget().isBefore(today);
    }

    private String tableCell(String value, String align, boolean highlightOverdue) {
        String emphasis = highlightOverdue ? " color:#c00000; font-weight:700;" : "";
        return "<td style=\"border:1px solid #b8b8b8; padding:1px 3px; mso-padding-alt:1px 3px 1px 3px; text-align:" + align + "; white-space:nowrap; font-size:8pt; line-height:10pt; mso-line-height-rule:exactly; height:12px;" + emphasis + "\">" +
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
        List<String> missing = new ArrayList<>();
        for (String column : List.of(HEADER_CUSTOMER, HEADER_INVOICE_NO, HEADER_INVOICE_DATE,
                HEADER_OUTSTANDING_AMOUNT, HEADER_CURRENCY, HEADER_PAYMENT_TARGET)) {
            if (!columns.containsKey(column)) {
                missing.add(column);
            }
        }
        if (!columns.containsKey(HEADER_CUSTOMER_EMAIL) && !columns.containsKey(HEADER_EMAIL)) {
            missing.add(HEADER_CUSTOMER_EMAIL);
        }
        if (!missing.isEmpty()) {
            throw new ProcreditProcessingException("Липсват колони във входния файл: " + String.join(", ", missing));
        }
    }

    private boolean isInputRowBlank(Row row, Map<String, Integer> columns) {
        for (String header : List.of(HEADER_CUSTOMER, HEADER_INVOICE_NO, HEADER_OUTSTANDING_AMOUNT,
                HEADER_CURRENCY, HEADER_PAYMENT_TARGET)) {
            if (!readCellAsString(row.getCell(columns.get(header))).isBlank()) {
                return false;
            }
        }
        Integer emailColumn = optionalColumn(columns, HEADER_CUSTOMER_EMAIL, HEADER_EMAIL);
        return emailColumn == null || readCellAsString(row.getCell(emailColumn)).isBlank();
    }

    private Integer requiredColumn(Map<String, Integer> columns, String primary, String fallback) {
        Integer column = optionalColumn(columns, primary, fallback);
        if (column == null) {
            throw new ProcreditProcessingException("Липсва колона във входния файл: " + primary);
        }
        return column;
    }

    private Integer optionalColumn(Map<String, Integer> columns, String primary, String fallback) {
        Integer column = columns.get(primary);
        return column != null ? column : columns.get(fallback);
    }

    private String readOptionalColumn(Row row, Map<String, Integer> columns, String header) {
        Integer column = columns.get(header);
        return column == null ? "" : readCellAsString(row.getCell(column));
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
                .filter(email -> !email.isBlank())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<String> parseCustomerEmails(String value) {
        return parseEmails(value).stream()
                .filter(email -> !isFreelineEmail(email))
                .toList();
    }

    private boolean isFreelineEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int atIndex = normalized.lastIndexOf('@');
        if (atIndex < 0 || atIndex == normalized.length() - 1) {
            return false;
        }
        String domain = normalized.substring(atIndex + 1);
        return domain.equals("freeline.bg") || domain.endsWith(".freeline.bg");
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
        private final LinkedHashSet<String> freelineUsers = new LinkedHashSet<>();
        private final LinkedHashSet<String> countries = new LinkedHashSet<>();
        private boolean hasDueLine;

        private CustomerGroup(String customer) {
            this.customer = customer;
        }

        private boolean isForeignCustomer() {
            return countries.stream()
                    .map(country -> country.trim().toUpperCase(Locale.ROOT))
                    .anyMatch(country -> !country.isBlank() && !"BG".equals(country));
        }
    }
}
