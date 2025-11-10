package com.rillet.codingchallenge.accounting.infra.dataclasses

import javax.money.MonetaryAmount

data class RequestDto(val amount: MonetaryAmount)
