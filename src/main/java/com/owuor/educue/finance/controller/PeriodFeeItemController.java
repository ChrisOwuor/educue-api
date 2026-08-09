package com.owuor.educue.finance.controller;

import com.owuor.educue.finance.dto.ChangePeriodFeeAmountRequest;
import com.owuor.educue.finance.dto.CreatePeriodFeeItemRequest;
import com.owuor.educue.finance.dto.EffectivePeriodFeesResponse;
import com.owuor.educue.finance.dto.PeriodFeeItemResponse;
import com.owuor.educue.finance.dto.StopPeriodFeeItemRequest;
import com.owuor.educue.finance.dto.UpdatePeriodFeeItemRequest;
import com.owuor.educue.finance.service.PeriodFeeItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/period-fees")
@RequiredArgsConstructor
public class PeriodFeeItemController {

    private final PeriodFeeItemService periodFeeItemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodFeeItemResponse create(
            @Valid
            @RequestBody
            CreatePeriodFeeItemRequest request
    ) {
        return periodFeeItemService.create(request);
    }

    @GetMapping
    public Page<PeriodFeeItemResponse> search(
            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            UUID courseUuid,

            @RequestParam(required = false)
            UUID courseAcademicPeriodUuid,

            @RequestParam(required = false)
            UUID feeItemUuid,

            @RequestParam(required = false)
            Long effectiveAtIntakeId,

            @RequestParam(required = false)
            Boolean mandatory,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @RequestParam(defaultValue = "displayOrder,asc")
            String sort
    ) {
        return periodFeeItemService.search(
                search,
                courseUuid,
                courseAcademicPeriodUuid,
                feeItemUuid,
                effectiveAtIntakeId,
                mandatory,
                page,
                size,
                sort
        );
    }

    @GetMapping("/{id}")
    public PeriodFeeItemResponse getById(
            @PathVariable Long id
    ) {
        return periodFeeItemService.getById(id);
    }

    @PutMapping("/{id}")
    public PeriodFeeItemResponse update(
            @PathVariable Long id,

            @Valid
            @RequestBody
            UpdatePeriodFeeItemRequest request
    ) {
        return periodFeeItemService.update(id, request);
    }

    @PostMapping("/{id}/change-amount")
    public PeriodFeeItemResponse changeAmount(
            @PathVariable Long id,

            @Valid
            @RequestBody
            ChangePeriodFeeAmountRequest request
    ) {
        return periodFeeItemService.changeAmount(
                id,
                request
        );
    }

    @PostMapping("/{id}/stop")
    public PeriodFeeItemResponse stop(
            @PathVariable Long id,

            @Valid
            @RequestBody
            StopPeriodFeeItemRequest request
    ) {
        return periodFeeItemService.stop(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        periodFeeItemService.delete(id);
    }

    @GetMapping("/effective")
    public EffectivePeriodFeesResponse getEffectiveFees(
            @RequestParam
            UUID courseAcademicPeriodUuid,

            @RequestParam
            Long intakeId
    ) {
        return periodFeeItemService.getEffectiveFees(
                courseAcademicPeriodUuid,
                intakeId
        );
    }

    @GetMapping("/history")
    public List<PeriodFeeItemResponse> getHistory(
            @RequestParam
            UUID courseAcademicPeriodUuid,

            @RequestParam
            UUID feeItemUuid
    ) {
        return periodFeeItemService.getHistory(
                courseAcademicPeriodUuid,
                feeItemUuid
        );
    }
}
