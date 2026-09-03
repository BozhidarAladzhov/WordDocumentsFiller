package com.example.WordDocumentsFiller.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphMailSignatureServiceTest {

    private final GraphMailSignatureService service = new GraphMailSignatureService();

    @Test
    void shouldUsePetyaSignatureForPetyaAccount() {
        String html = service.buildHtmlBody("Body", "petya@freeline.bg");

        assertTrue(html.contains("Petya Zayakova"));
        assertTrue(html.contains("Tel: +3592 442 01 88"));
        assertTrue(html.contains("GSM +359 895524295"));
        assertTrue(html.contains("mailto:petya@freeline.bg"));
        assertTrue(html.contains("cid:freeline-banner"));
        assertTrue(html.contains("cid:freeline-logo-small"));
        assertTrue(html.contains("cid:freeline-logo-wide"));
        assertFalse(html.contains("Bojidar Aladjov"));
    }

    @Test
    void shouldUseDefaultSignatureForOtherAccounts() {
        String html = service.buildHtmlBody("Body", "bojidar@freeline.bg");

        assertTrue(html.contains("Bojidar Aladjov"));
        assertTrue(html.contains("mailto:bojidar@freeline.bg"));
        assertFalse(html.contains("Petya Zayakova"));
    }
}
