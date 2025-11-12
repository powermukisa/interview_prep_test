package com.rillet.codingchallenge.accounting.domain;

public enum RoundingPlacement {
    FIRST,
    MIDDLE,
    LAST;

    public int calculateAdjustmentIndex() {
        return switch (this) {
            case FIRST -> 0;
            case MIDDLE -> 5;
            case LAST -> 11;
        };
    }
}
