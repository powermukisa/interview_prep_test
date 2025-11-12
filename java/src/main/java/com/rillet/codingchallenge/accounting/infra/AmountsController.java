package com.rillet.codingchallenge.accounting.infra;

import com.rillet.codingchallenge.accounting.application.AllocateAmount;
import com.rillet.codingchallenge.accounting.domain.RevenueAllocation;
import com.rillet.codingchallenge.accounting.domain.RevenueRecognitionRequest;
import com.rillet.codingchallenge.accounting.infra.dataclasses.RequestDto;
import com.rillet.codingchallenge.accounting.infra.dataclasses.ResponseDto;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ResponseDto> createAmounts(
            @RequestBody RequestDto requestDto
    ) {
        try {
            // Step 1: Convert infrastructure DTO → Domain value object
            // This is the adapter pattern - translating between layers
            RevenueRecognitionRequest domainRequest = requestDto.toDomain();

            // Step 2: Call application service through port interface
            // We're calling AllocateAmount (interface), not AllocateAmountUseCase (implementation)
            // Spring injects the implementation, but we only depend on the abstraction
            RevenueAllocation allocation = allocateAmount.execute(domainRequest);

            // Step 3: Convert domain aggregate → Infrastructure DTO
            // Again, adapter pattern - translating layers
            ResponseDto responseDto = ResponseDto.fromDomain(allocation);

            // Step 4: Return HTTP 200 OK with JSON response
            return ResponseEntity.ok(responseDto);

        } catch (IllegalArgumentException e) {
            // Domain validation failed (e.g., negative amount)
            // Return 400 Bad Request (client error)
            // FUTURE: Could include error details in response body
            return ResponseEntity
                    .badRequest()
                    .build();

        } catch (Exception e) {
            // Unexpected error (should rarely happen)
            // Return 500 Internal Server Error
            // FUTURE: Log this error, alert monitoring
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}


