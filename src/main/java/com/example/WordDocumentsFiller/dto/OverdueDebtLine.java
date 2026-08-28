package com.example.WordDocumentsFiller.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OverdueDebtLine(
        int rowNumber,
        String customer,
        String invoiceNo,
        LocalDate invoiceDate,
        BigDecimal outstandingAmount,
        String currency,
        LocalDate paymentTarget,
        String email
) implements Serializable {
}
