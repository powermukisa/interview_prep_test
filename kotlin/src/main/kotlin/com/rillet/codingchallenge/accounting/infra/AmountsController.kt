package com.rillet.codingchallenge.accounting.infra

import com.rillet.codingchallenge.accounting.application.SplitAmountsUseCase
import com.rillet.codingchallenge.accounting.infra.dataclasses.RequestDto
import com.rillet.codingchallenge.accounting.infra.dataclasses.ResponseDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/amounts")
class AmountsController(private val useCase: SplitAmountsUseCase) {
    @PostMapping
    fun createAmounts(@RequestBody request: RequestDto): ResponseEntity<ResponseDto> {
        val result = useCase.execute(request.amount)
        return ResponseEntity.ok(ResponseDto.from(result))
    }
}


