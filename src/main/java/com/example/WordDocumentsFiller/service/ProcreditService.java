package com.example.WordDocumentsFiller.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ProcreditService {

    private static final String TEMPLATE_RESOURCE = "prcb_payments_template.xls";
    private static final String INPUT_SHEET_NAME = "Sheet";
    private static final String PAYMENTS_SHEET_NAME = "payments";

    private static final String HEADER_CARRIER = "Carrier";
    private static final String HEADER_IBAN = "IBAN";
    private static final String HEADER_ADDRESS = "Address";
    private static final String HEADER_COUNTRY = "Country";
    private static final String HEADER_CURRENCY = "Currency";
    private static final String HEADER_OUTSTANDING_AMOUNT = "Outstanding amount";
    private static final String HEADER_INVOICE_NUMBER = "Invoice no#";
    private static final String HEADER_INVOICE_NUMBER_ALT = "Invoice no.";

    private static final String COUNTRY_BG = "BG";
    private static final String CURRENCY_EUR = "EUR";
    private static final String COUNTRY_CODE_BG = "100";
    private static final String PAYER_IBAN_VALUE = "BG81PRCB92301023854015";

    private static final int FIRST_PAYMENT_ROW_INDEX = 8;
    private static final int TEMPLATE_VERSION_ROW_INDEX = 9;
    private static final int TEMPLATE_TOTAL_ROW_INDEX = 10;
    private static final int TEMPLATE_INSTRUCTIONS_START_ROW_INDEX = 11;
    private static final int TEMPLATE_INSTRUCTIONS_END_ROW_INDEX = 14;
    private static final int TEMPLATE_ARCHIVE_START_ROW_INDEX = 17;
    private static final int TEMPLATE_ARCHIVE_END_ROW_INDEX = 18;
    private static final int TEMPLATE_COLUMN_COUNT = 12;

    private static final int COL_SEQUENCE = 0;
    private static final int COL_NAME = 1;
    private static final int COL_IBAN = 2;
    private static final int COL_ADDRESS = 3;
    private static final int COL_COUNTRY = 4;
    private static final int COL_BANK_NAME = 5;
    private static final int COL_BIC = 6;
    private static final int COL_AMOUNT_EUR = 7;
    private static final int COL_REASON = 8;
    private static final int COL_PAYMENT_SYSTEM = 9;
    private static final int COL_PAYER_IBAN = 10;
    private static final int COL_DECLARATION = 11;

    public GeneratedWorkbook generatePayments(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ProcreditProcessingException("Избери входен Excel файл.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new ProcreditProcessingException("Файлът трябва да е .xlsx.");
        }

        List<PaymentRow> paymentRows;
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            paymentRows = extractPayments(workbook);
        }

        if (paymentRows.isEmpty()) {
            throw new ProcreditProcessingException("Няма редове с Country = BG и Currency = EUR.");
        }

        try (InputStream templateStream = new ClassPathResource(TEMPLATE_RESOURCE).getInputStream();
             Workbook templateWorkbook = WorkbookFactory.create(templateStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            fillTemplate(templateWorkbook, paymentRows);
            templateWorkbook.write(outputStream);

            String outputFileName = "procredit_payments_" +
                    LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".xls";

            return new GeneratedWorkbook(outputFileName, outputStream.toByteArray());
        }
    }

    private List<PaymentRow> extractPayments(Workbook workbook) {
        Sheet sheet = workbook.getSheet(INPUT_SHEET_NAME);
        if (sheet == null) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ProcreditProcessingException("Входният файл няма sheet-ове.");
            }
            sheet = workbook.getSheetAt(0);
        }

        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new ProcreditProcessingException("Входният файл няма header ред.");
        }

        Map<String, Integer> columns = mapColumns(headerRow);
        validateRequiredColumns(columns);

        Map<String, AggregatedCarrier> grouped = new LinkedHashMap<>();

        for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isInputRowBlank(row, columns)) {
                continue;
            }

            String country = readCellAsString(row.getCell(columns.get(HEADER_COUNTRY)));
            String currency = readCellAsString(row.getCell(columns.get(HEADER_CURRENCY)));
            if (!COUNTRY_BG.equalsIgnoreCase(country.trim()) || !CURRENCY_EUR.equalsIgnoreCase(currency.trim())) {
                continue;
            }

            String carrier = requireValue(rowIndex, HEADER_CARRIER, readCellAsString(row.getCell(columns.get(HEADER_CARRIER))));
            String iban = requireValue(rowIndex, HEADER_IBAN, readCellAsString(row.getCell(columns.get(HEADER_IBAN))));
            String address = requireValue(rowIndex, HEADER_ADDRESS, readCellAsString(row.getCell(columns.get(HEADER_ADDRESS))));
            Integer invoiceColumnIndex = findColumn(columns, HEADER_INVOICE_NUMBER, HEADER_INVOICE_NUMBER_ALT);
            String invoiceNo = requireValue(rowIndex, "Invoice no.", readCellAsString(row.getCell(invoiceColumnIndex)));
            BigDecimal outstandingAmount = readRequiredAmount(rowIndex, HEADER_OUTSTANDING_AMOUNT,
                    row.getCell(columns.get(HEADER_OUTSTANDING_AMOUNT)));

            grouped.computeIfAbsent(carrier, ignored -> new AggregatedCarrier(carrier))
                    .add(rowIndex + 1, iban, address, invoiceNo, outstandingAmount);
        }

        return grouped.values().stream()
                .map(AggregatedCarrier::toPaymentRow)
                .toList();
    }

    private void fillTemplate(Workbook workbook, List<PaymentRow> paymentRows) {
        Sheet sheet = workbook.getSheet(PAYMENTS_SHEET_NAME);
        if (sheet == null) {
            throw new ProcreditProcessingException("Template файлът няма sheet 'payments'.");
        }

        RowTemplate dataTemplate = captureRowTemplate(sheet.getRow(FIRST_PAYMENT_ROW_INDEX), TEMPLATE_COLUMN_COUNT);
        RowTemplate versionTemplate = captureRowTemplate(sheet.getRow(TEMPLATE_VERSION_ROW_INDEX), TEMPLATE_COLUMN_COUNT);
        RowTemplate totalTemplate = captureRowTemplate(sheet.getRow(TEMPLATE_TOTAL_ROW_INDEX), TEMPLATE_COLUMN_COUNT);
        List<RowTemplate> instructionTemplates = captureRowTemplates(
                sheet, TEMPLATE_INSTRUCTIONS_START_ROW_INDEX, TEMPLATE_INSTRUCTIONS_END_ROW_INDEX, TEMPLATE_COLUMN_COUNT);
        List<RowTemplate> archiveTemplates = captureRowTemplates(
                sheet, TEMPLATE_ARCHIVE_START_ROW_INDEX, TEMPLATE_ARCHIVE_END_ROW_INDEX, TEMPLATE_COLUMN_COUNT);

        if (dataTemplate == null || versionTemplate == null || totalTemplate == null) {
            throw new ProcreditProcessingException("Template файлът няма очакваната таблица за плащания.");
        }

        String defaultPaymentSystem = readCellAsString(sheet.getRow(FIRST_PAYMENT_ROW_INDEX).getCell(COL_PAYMENT_SYSTEM));
        int originalLastRow = sheet.getLastRowNum();

        for (int i = 0; i < paymentRows.size(); i++) {
            Row row = recreateRow(sheet, FIRST_PAYMENT_ROW_INDEX + i);
            applyRowTemplate(row, dataTemplate);

            PaymentRow paymentRow = paymentRows.get(i);
            int excelRowNumber = FIRST_PAYMENT_ROW_INDEX + i + 1;
            writeString(row, COL_SEQUENCE, Integer.toString(i + 1));
            writeString(row, COL_NAME, paymentRow.carrier());
            writeString(row, COL_IBAN, paymentRow.iban());
            writeString(row, COL_ADDRESS, paymentRow.address());
            writeString(row, COL_COUNTRY, COUNTRY_CODE_BG);
            writeFormula(row, COL_BANK_NAME, bankNameFormula(excelRowNumber));
            writeFormula(row, COL_BIC, bankBicFormula(excelRowNumber));
            writeAmount(row, COL_AMOUNT_EUR, paymentRow.totalAmount());
            writeString(row, COL_REASON, paymentRow.reason());
            writeString(row, COL_PAYMENT_SYSTEM, defaultPaymentSystem.isBlank() ? "STANDARD" : defaultPaymentSystem);
            writeString(row, COL_PAYER_IBAN, PAYER_IBAN_VALUE);
            clearCell(row, COL_DECLARATION);
        }

        int versionRowIndex = FIRST_PAYMENT_ROW_INDEX + paymentRows.size();
        int totalRowIndex = versionRowIndex + 1;
        int cursor = totalRowIndex + 1;

        Row versionRow = recreateRow(sheet, versionRowIndex);
        applyRowTemplate(versionRow, versionTemplate);

        Row totalRow = recreateRow(sheet, totalRowIndex);
        applyRowTemplate(totalRow, totalTemplate);

        for (RowTemplate template : instructionTemplates) {
            Row row = recreateRow(sheet, cursor);
            applyRowTemplate(row, template);
            cursor++;
        }

        for (int i = 0; i < 2; i++) {
            clearRow(recreateRow(sheet, cursor), TEMPLATE_COLUMN_COUNT);
            cursor++;
        }

        int archiveStartRowIndex = cursor;
        for (RowTemplate template : archiveTemplates) {
            Row row = recreateRow(sheet, cursor);
            applyRowTemplate(row, template);
            cursor++;
        }

        for (int rowIndex = cursor; rowIndex <= originalLastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                clearRow(row, TEMPLATE_COLUMN_COUNT);
            }
        }

        updateNamedRanges(workbook, paymentRows.size(), archiveStartRowIndex);
        workbook.setForceFormulaRecalculation(true);
        sheet.setForceFormulaRecalculation(true);
    }

    private Map<String, Integer> mapColumns(Row headerRow) {
        Map<String, Integer> columns = new HashMap<>();
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
                HEADER_CARRIER,
                HEADER_IBAN,
                HEADER_ADDRESS,
                HEADER_COUNTRY,
                HEADER_CURRENCY,
                HEADER_OUTSTANDING_AMOUNT
        );
        List<String> missing = new ArrayList<>(required.stream()
                .filter(column -> !columns.containsKey(column))
                .toList());
        if (findColumn(columns, HEADER_INVOICE_NUMBER, HEADER_INVOICE_NUMBER_ALT) == null) {
            missing.add("Invoice no. / Invoice no#");
        }
        if (!missing.isEmpty()) {
            throw new ProcreditProcessingException("Липсват колони във входния файл: " + String.join(", ", missing));
        }
    }

    private boolean isInputRowBlank(Row row, Map<String, Integer> columns) {
        for (String header : List.of(HEADER_CARRIER, HEADER_IBAN, HEADER_ADDRESS, HEADER_COUNTRY,
                HEADER_CURRENCY, HEADER_OUTSTANDING_AMOUNT)) {
            Cell cell = row.getCell(columns.get(header));
            if (!readCellAsString(cell).isBlank()) {
                return false;
            }
        }
        Integer invoiceColumnIndex = findColumn(columns, HEADER_INVOICE_NUMBER, HEADER_INVOICE_NUMBER_ALT);
        return invoiceColumnIndex == null || readCellAsString(row.getCell(invoiceColumnIndex)).isBlank();
    }

    private Integer findColumn(Map<String, Integer> columns, String... candidates) {
        for (String candidate : candidates) {
            Integer index = columns.get(candidate);
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private String requireValue(int zeroBasedRowIndex, String fieldName, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new ProcreditProcessingException("Липсва стойност в колона '" + fieldName +
                    "' на ред " + (zeroBasedRowIndex + 1) + ".");
        }
        return trimmed;
    }

    private BigDecimal readRequiredAmount(int zeroBasedRowIndex, String fieldName, Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new ProcreditProcessingException("Липсва стойност в колона '" + fieldName +
                    "' на ред " + (zeroBasedRowIndex + 1) + ".");
        }

        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> parseAmount(cell.getStringCellValue());
                case FORMULA -> BigDecimal.valueOf(cell.getNumericCellValue());
                default -> parseAmount(readCellAsString(cell));
            };
        } catch (NumberFormatException ex) {
            throw new ProcreditProcessingException("Невалидна сума в колона '" + fieldName +
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

    private String readCellAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        DataFormatter formatter = new DataFormatter(Locale.US);
        return formatter.formatCellValue(cell).trim();
    }

    private List<RowTemplate> captureRowTemplates(Sheet sheet, int startRowIndex, int endRowIndex, int columnCount) {
        List<RowTemplate> templates = new ArrayList<>();
        for (int rowIndex = startRowIndex; rowIndex <= endRowIndex; rowIndex++) {
            RowTemplate template = captureRowTemplate(sheet.getRow(rowIndex), columnCount);
            if (template != null) {
                templates.add(template);
            }
        }
        return templates;
    }

    private RowTemplate captureRowTemplate(Row sourceRow, int columnCount) {
        if (sourceRow == null) {
            return null;
        }
        CellTemplate[] cells = new CellTemplate[columnCount];
        for (int col = 0; col < columnCount; col++) {
            cells[col] = CellTemplate.from(sourceRow.getCell(col));
        }
        return new RowTemplate(sourceRow.getHeight(), cells);
    }

    private void applyRowTemplate(Row row, RowTemplate template) {
        row.setHeight(template.height());
        for (int col = 0; col < template.cells().length; col++) {
            Cell cell = row.getCell(col);
            if (cell == null) {
                cell = row.createCell(col);
            }
            template.cells()[col].apply(cell);
        }
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        return row;
    }

    private Row recreateRow(Sheet sheet, int rowIndex) {
        Row existing = sheet.getRow(rowIndex);
        if (existing != null) {
            sheet.removeRow(existing);
        }
        return sheet.createRow(rowIndex);
    }

    private void writeString(Row row, int columnIndex, String value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(value == null ? "" : value);
    }

    private void writeFormula(Row row, int columnIndex, String formula) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellFormula(formula);
    }

    private void writeAmount(Row row, int columnIndex, BigDecimal amount) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(amount.setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    private void clearCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setBlank();
    }

    private void clearRow(Row row, int columnCount) {
        for (int col = 0; col < columnCount; col++) {
            Cell cell = row.getCell(col);
            if (cell == null) {
                cell = row.createCell(col);
            }
            CellStyle style = cell.getCellStyle();
            cell.setBlank();
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private void updateNamedRanges(Workbook workbook, int paymentRowCount, int archiveStartRowIndex) {
        int firstExcelRow = FIRST_PAYMENT_ROW_INDEX + 1;
        int lastExcelRow = FIRST_PAYMENT_ROW_INDEX + paymentRowCount;

        updateName(workbook, "Benef_Name", range("B", firstExcelRow, "B", lastExcelRow));
        updateName(workbook, "Benef_Iban", range("C", firstExcelRow, "C", lastExcelRow));
        updateName(workbook, "Benef_Address", range("D", firstExcelRow, "D", lastExcelRow));
        updateName(workbook, "Benef_Country", range("E", firstExcelRow, "E", lastExcelRow));
        updateName(workbook, "Benef_Bank", range("F", firstExcelRow, "F", lastExcelRow));
        updateName(workbook, "Benef_BIC", range("G", firstExcelRow, "G", lastExcelRow));
        updateName(workbook, "Payment_Amount", range("H", firstExcelRow, "H", lastExcelRow));
        updateName(workbook, "Payment_Reason1", range("I", firstExcelRow, "I", lastExcelRow));
        updateName(workbook, "Pay_Sys", range("J", firstExcelRow, "J", lastExcelRow));
        updateName(workbook, "Payer_IBAN", range("K", firstExcelRow, "K", lastExcelRow));
        updateName(workbook, "decl", range("L", firstExcelRow, "L", lastExcelRow));
        updateName(workbook, "rNumbers", range("A", firstExcelRow, "A", lastExcelRow));
        updateName(workbook, "p_data", range("A", firstExcelRow, "L", lastExcelRow));
        updateName(workbook, "arh_data", range("A", archiveStartRowIndex + 1, "L", archiveStartRowIndex + 2));
    }

    private void updateName(Workbook workbook, String name, String formula) {
        Name workbookName = findName(workbook, name);
        if (workbookName != null) {
            workbookName.setRefersToFormula(PAYMENTS_SHEET_NAME + "!" + formula);
        }
    }

    private Name findName(Workbook workbook, String targetName) {
        for (Name name : workbook.getAllNames()) {
            if (targetName.equals(name.getNameName())) {
                return name;
            }
        }
        return null;
    }

    private String range(String startCol, int startRow, String endCol, int endRow) {
        return "$" + startCol + "$" + startRow + ":$" + endCol + "$" + endRow;
    }

    private String bankNameFormula(int excelRowNumber) {
        return "IF(ISBLANK(C" + excelRowNumber + "),\"\",IF(ISNA(VLOOKUP(MID(C" + excelRowNumber
                + ",5,4),sysBanks,2,FALSE)),\"невалидна банка\",VLOOKUP(MID(C" + excelRowNumber
                + ",5,4),sysBanks,2,FALSE)))";
    }

    private String bankBicFormula(int excelRowNumber) {
        return "IF(ISBLANK(C" + excelRowNumber + "),\"\",IF(ISNA(VLOOKUP(MID(C" + excelRowNumber
                + ",5,4),sysBanks,3,FALSE)),\"\",VLOOKUP(MID(C" + excelRowNumber + ",5,4),sysBanks,3,FALSE)))";
    }

    public record GeneratedWorkbook(String fileName, byte[] content) {
    }

    private static final class AggregatedCarrier {
        private final String carrier;
        private String iban;
        private String address;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private final List<String> invoices = new ArrayList<>();

        private AggregatedCarrier(String carrier) {
            this.carrier = carrier;
        }

        private void add(int excelRowNumber, String iban, String address, String invoiceNo, BigDecimal amount) {
            validateSameValue("IBAN", excelRowNumber, this.iban, iban, carrier);
            validateSameValue("Address", excelRowNumber, this.address, address, carrier);

            if (this.iban == null) {
                this.iban = iban;
            }
            if (this.address == null) {
                this.address = address;
            }

            totalAmount = totalAmount.add(amount);
            invoices.add(invoiceNo);
        }

        private PaymentRow toPaymentRow() {
            return new PaymentRow(
                    carrier,
                    iban,
                    address,
                    totalAmount,
                    String.join("; ", invoices)
            );
        }

        private void validateSameValue(String fieldName, int excelRowNumber, String currentValue,
                                       String newValue, String carrier) {
            if (currentValue != null && !currentValue.equals(newValue)) {
                throw new ProcreditProcessingException(
                        "Carrier '" + carrier + "' има различни стойности за " + fieldName +
                                ". Провери ред " + excelRowNumber + "."
                );
            }
        }
    }

    private record PaymentRow(String carrier, String iban, String address, BigDecimal totalAmount, String reason) {
    }

    private record RowTemplate(short height, CellTemplate[] cells) {
    }

    private static final class CellTemplate {
        private final CellType cellType;
        private final CellStyle style;
        private final String stringValue;
        private final Double numericValue;
        private final Boolean booleanValue;
        private final String formula;

        private CellTemplate(CellType cellType, CellStyle style, String stringValue,
                             Double numericValue, Boolean booleanValue, String formula) {
            this.cellType = cellType;
            this.style = style;
            this.stringValue = stringValue;
            this.numericValue = numericValue;
            this.booleanValue = booleanValue;
            this.formula = formula;
        }

        private static CellTemplate from(Cell cell) {
            if (cell == null) {
                return new CellTemplate(CellType.BLANK, null, null, null, null, null);
            }
            return switch (cell.getCellType()) {
                case STRING -> new CellTemplate(CellType.STRING, cell.getCellStyle(),
                        cell.getStringCellValue(), null, null, null);
                case NUMERIC -> new CellTemplate(CellType.NUMERIC, cell.getCellStyle(),
                        null, cell.getNumericCellValue(), null, null);
                case BOOLEAN -> new CellTemplate(CellType.BOOLEAN, cell.getCellStyle(),
                        null, null, cell.getBooleanCellValue(), null);
                case FORMULA -> new CellTemplate(CellType.FORMULA, cell.getCellStyle(),
                        null, null, null, cell.getCellFormula());
                default -> new CellTemplate(CellType.BLANK, cell.getCellStyle(), null, null, null, null);
            };
        }

        private void apply(Cell cell) {
            if (style != null) {
                cell.setCellStyle(style);
            }
            switch (cellType) {
                case STRING -> cell.setCellValue(stringValue == null ? "" : stringValue);
                case NUMERIC -> cell.setCellValue(numericValue == null ? 0d : numericValue);
                case BOOLEAN -> cell.setCellValue(booleanValue != null && booleanValue);
                case FORMULA -> cell.setCellFormula(formula);
                default -> cell.setBlank();
            }
        }
    }
}
