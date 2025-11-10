package com.rillet.codingchallenge.accounting.domain;

import javax.money.Monetary;
import javax.money.MonetaryAmount;

public class CurrenciesHelper {

    /**
     * Rounds a monetary amount using the default rounding defined for its currency.
     */
    public static MonetaryAmount rounded(MonetaryAmount amount) {
        return amount.with(Monetary.getDefaultRounding());
    }
}
