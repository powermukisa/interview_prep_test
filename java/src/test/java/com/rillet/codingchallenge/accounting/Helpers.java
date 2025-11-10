package com.rillet.codingchallenge.accounting;

import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;


public class Helpers {
    public static CurrencyUnit dollarsCurrency = Monetary.getCurrency("USD");

    public static MonetaryAmount dollars(Number n) {
        return Money.of(n, dollarsCurrency);
    }
}
