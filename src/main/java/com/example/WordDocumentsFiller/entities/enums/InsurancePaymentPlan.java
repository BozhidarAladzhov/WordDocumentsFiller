package com.example.WordDocumentsFiller.entities.enums;

public enum InsurancePaymentPlan {
    FULL,
    TWO_INSTALLMENTS,
    FOUR_INSTALLMENTS;

    public String getDisplayName() {
        return switch (this) {
            case FULL -> "Еднократно";
            case TWO_INSTALLMENTS -> "На 2 вноски";
            case FOUR_INSTALLMENTS -> "На 4 вноски";
        };
    }
}
