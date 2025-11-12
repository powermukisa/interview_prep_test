package com.rillet.codingchallenge.accounting.domain;

import javax.money.MonetaryAmount;
import java.time.Month;
import java.util.Objects;

public record MonthlyAllocation(
    Month month,
    MonetaryAmount amount
) {
//todo add validations

//    Objects.
}
