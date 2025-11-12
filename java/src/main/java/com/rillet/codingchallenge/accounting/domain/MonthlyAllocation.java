package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.Objects;

/**
 * Represents a request to allocate annual revenue across periods.
 *
 */
public record MonthlyAllocation(
        Month month,
        MonetaryAmount amount
) {

    public MonthlyAllocation {
        Objects.requireNonNull(month, "Month cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        if (amount.isNegative()) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
    }

}
