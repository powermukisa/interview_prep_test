package com.rillet.codingchallenge.accounting

import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.MonetaryAmount
import org.javamoney.moneta.Money

val dollarsCurrency: CurrencyUnit = Monetary.getCurrency("USD")

fun dollars(n: Number): MonetaryAmount = Money.of(n, dollarsCurrency)
