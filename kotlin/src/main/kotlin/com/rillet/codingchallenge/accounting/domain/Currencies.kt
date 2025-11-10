package com.rillet.codingchallenge.accounting.domain

import javax.money.Monetary
import javax.money.MonetaryAmount

/**
 * Rounds a monetary amount using the default rounding defined for its currency.
 */
fun MonetaryAmount.rounded(): MonetaryAmount = this.with(Monetary.getDefaultRounding())
