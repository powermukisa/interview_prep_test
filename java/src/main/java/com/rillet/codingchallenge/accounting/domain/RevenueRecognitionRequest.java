package com.rillet.codingchallenge.accounting.domain;

import com.rillet.codingchallenge.accounting.infra.dataclasses.RequestDto;

import javax.money.MonetaryAmount;
import java.util.Objects;

/**
 * Represents a request to allocate annual revenue across periods.
 *
 */
public record RevenueRecognitionRequest (
        MonetaryAmount amount,
        RoundingPlacement roundingPlacement
) {

    public RevenueRecognitionRequest {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(roundingPlacement, "Rounding placement cannot be null");

        if (amount.isNegative()) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
    }
}
