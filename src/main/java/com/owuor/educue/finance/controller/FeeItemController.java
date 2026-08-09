package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.ChangeFeeItemStatusRequest;
import com.owuor.educue.finance.dto.CreateFeeItemRequest;
import com.owuor.educue.finance.dto.FeeItemResponse;
import com.owuor.educue.finance.dto.UpdateFeeItemRequest;
import com.owuor.educue.finance.service.FeeItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fee-items")
@RequiredArgsConstructor
public class FeeItemController {

    private final FeeItemService feeItemService;

    @PostMapping
    public ResponseEntity<FeeItemResponse> create(
            @Valid @RequestBody CreateFeeItemRequest request
    ) {
        FeeItemResponse response = feeItemService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public Page<FeeItemResponse> search(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        return feeItemService.search(
                search,
                active,
                page,
                size,
                sort
        );
    }

    @GetMapping("/{uuid}")
    public FeeItemResponse getByUuid(
            @PathVariable UUID uuid
    ) {
        return feeItemService.getByUuid(uuid);
    }

    @PutMapping("/{uuid}")
    public FeeItemResponse update(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateFeeItemRequest request
    ) {
        return feeItemService.update(uuid, request);
    }

    @PatchMapping("/{uuid}/status")
    public FeeItemResponse changeStatus(
            @PathVariable UUID uuid,
            @Valid @RequestBody ChangeFeeItemStatusRequest request
    ) {
        return feeItemService.changeStatus(uuid, request);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID uuid
    ) {
        feeItemService.delete(uuid);
    }
}
