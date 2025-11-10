package com.rillet.codingchallenge.accounting.infra;

import com.rillet.codingchallenge.accounting.application.AllocateAmount;
import com.rillet.codingchallenge.accounting.infra.dataclasses.RequestDto;
import com.rillet.codingchallenge.accounting.infra.dataclasses.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/amounts")
public class AmountsController {
    private final AllocateAmount allocateAmount;

    public AmountsController(AllocateAmount allocateAmount) {
        this.allocateAmount = allocateAmount;
    }

    @PostMapping
    public ResponseEntity<ResponseDto> createAmounts(@RequestBody RequestDto request) {
        var result = allocateAmount.execute(request.amount());
        return ResponseEntity.ok(ResponseDto.from(result));
    }
}


