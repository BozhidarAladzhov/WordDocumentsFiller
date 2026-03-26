package com.example.WordDocumentsFiller.dto;

import java.io.Serializable;

public record GraphMailDraft(
        String to,
        String cc,
        String subject,
        String body,
        String returnTo
) implements Serializable {
}
