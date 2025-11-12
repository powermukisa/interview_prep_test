package com.rillet.codingchallenge.accounting.infra.dataclasses;

import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;
import com.rillet.codingchallenge.accounting.domain.RoundingPlacement;

import javax.money.MonetaryAmount;

public record RequestDto(MonetaryAmount amount, RoundingPlacement roundingPlacement) {

    public RevenueRecognitionRequest toDomain() {
        return new RevenueRecognitionRequest(amount, roundingPlacement);
    }
}
