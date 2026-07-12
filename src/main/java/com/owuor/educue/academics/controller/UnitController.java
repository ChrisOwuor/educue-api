package com.owuor.educue.academics.controller;

import com.owuor.educue.academics.dto.*;
import com.owuor.educue.academics.service.UnitService;
import com.owuor.educue.common.dto.ApiPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public ResponseEntity<ApiPageResponse<UnitDto>> getAll(
            UnitFilterRequest request
    ) {
        return ResponseEntity.ok(
                unitService.getAll(request)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitDto> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                unitService.getById(id)
        );
    }

    @PostMapping
    public ResponseEntity<UnitDto> create(
            @Valid @RequestBody CreateUnitRequest request
    ) {
        return ResponseEntity.ok(
                unitService.create(request)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnitDto> update(
            @PathVariable Long id,
            @RequestBody UpdateUnitRequest request
    ) {
        return ResponseEntity.ok(
                unitService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        unitService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
