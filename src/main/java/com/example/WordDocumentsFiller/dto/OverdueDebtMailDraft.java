package com.example.WordDocumentsFiller.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record OverdueDebtMailDraft(
        String id,
        String customer,
        String to,
        String cc,
        String subject,
        String body,
        String bodyHtml,
        List<OverdueDebtLine> lines,
        Map<String, BigDecimal> totalsByCurrency
) implements Serializable {
}
