package com.rillet.codingchallenge.accounting.application

import com.rillet.codingchallenge.accounting.dollars
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isNotEqualTo

class SplitAmountsUseCaseTest {

    private val useCase = SplitAmountsUseCase()

    @Test
    fun `returns amounts calculated over a year`() {
        val result = useCase.execute(dollars(1200))

        expectThat(result).isNotEqualTo(null)
    }
}