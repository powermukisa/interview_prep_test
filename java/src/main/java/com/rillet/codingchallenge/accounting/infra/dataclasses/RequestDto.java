package com.rillet.codingchallenge.accounting.infra.dataclasses;

import javax.money.MonetaryAmount;

public record RequestDto(MonetaryAmount amount) {
}
