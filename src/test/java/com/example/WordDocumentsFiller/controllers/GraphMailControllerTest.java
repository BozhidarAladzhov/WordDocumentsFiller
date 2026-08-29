package com.example.WordDocumentsFiller.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphMailControllerTest {

    @Test
    void shouldCarryReturnToAndSendPendingInState() {
        String state = GraphMailController.buildState("/overdue-debts", false);

        GraphMailController.ParsedState parsed = GraphMailController.parseState(state);

        assertNotNull(parsed);
        assertEquals("/overdue-debts", parsed.returnTo());
        assertFalse(parsed.sendPending());
    }

    @Test
    void shouldRejectInvalidState() {
        assertNull(GraphMailController.parseState("not-a-valid-state"));
    }

    @Test
    void shouldSanitizeStateReturnTo() {
        String state = GraphMailController.buildState("https://example.com/elsewhere", true);

        GraphMailController.ParsedState parsed = GraphMailController.parseState(state);

        assertNotNull(parsed);
        assertEquals("/container-tracker/containers", parsed.returnTo());
        assertTrue(parsed.sendPending());
    }
}
