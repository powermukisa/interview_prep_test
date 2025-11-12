package com.rillet.codingchallenge.accounting.domain;

public enum RoundingPlacement {
    FIRST,
    LAST,
    MIDDLE;

    public int calculateAdjustmentIndex(int totalPeriods) {
        return switch (this) {
            case FIRST -> 0;
            case LAST -> totalPeriods - 1;
            case MIDDLE -> totalPeriods / 2 - 1;
        };
    }
}
