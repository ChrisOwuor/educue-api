package com.owuor.educue.finance.service;

import com.owuor.educue.finance.dto.ChangeFeeItemStatusRequest;
import com.owuor.educue.finance.dto.CreateFeeItemRequest;
import com.owuor.educue.finance.dto.FeeItemResponse;
import com.owuor.educue.finance.dto.UpdateFeeItemRequest;
import com.owuor.educue.finance.entity.FeeItem;
import com.owuor.educue.finance.repository.FeeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeItemService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "code",
            "name",
            "category",
            "active",
            "createdAt",
            "updatedAt"
    );

    private final FeeItemRepository feeItemRepository;

    @Transactional
    public FeeItemResponse create(CreateFeeItemRequest request) {
        String code = normalizeCode(request.code());

        if (feeItemRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A fee item with code '" + code + "' already exists"
            );
        }

        FeeItem feeItem = new FeeItem();
        feeItem.setCode(code);
        feeItem.setName(normalizeRequiredText(request.name()));
        feeItem.setCategory(normalizeNullableText(request.category()));
        feeItem.setActive(true);

        FeeItem saved = feeItemRepository.save(feeItem);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<FeeItemResponse> search(
            String search,
            Boolean active,
            int page,
            int size,
            String sort
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                parseSort(sort)
        );

        String normalizedSearch =
                search == null ? "" : search.trim();

        return feeItemRepository
                .search(normalizedSearch, active, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FeeItemResponse getByUuid(UUID uuid) {
        return toResponse(findByUuid(uuid));
    }

    @Transactional
    public FeeItemResponse update(
            UUID uuid,
            UpdateFeeItemRequest request
    ) {
        FeeItem feeItem = findByUuid(uuid);

        feeItem.setName(normalizeRequiredText(request.name()));
        feeItem.setCategory(normalizeNullableText(request.category()));

        return toResponse(feeItemRepository.save(feeItem));
    }

    @Transactional
    public FeeItemResponse changeStatus(
            UUID uuid,
            ChangeFeeItemStatusRequest request
    ) {
        FeeItem feeItem = findByUuid(uuid);
        feeItem.setActive(request.active());

        return toResponse(feeItemRepository.save(feeItem));
    }

    /**
     * Soft delete.
     *
     * We deactivate the fee item instead of deleting it because historical
     * period fee records or invoice lines may reference it.
     */
    @Transactional
    public void delete(UUID uuid) {
        FeeItem feeItem = findByUuid(uuid);

        if (!feeItem.isActive()) {
            return;
        }

        feeItem.setActive(false);
        feeItemRepository.save(feeItem);
    }

    private FeeItem findByUuid(UUID uuid) {
        return feeItemRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fee item was not found"
                ));
    }

    private String normalizeCode(String code) {
        return code
                .trim()
                .replaceAll("\\s+", "_")
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(
                    Sort.Order.asc("name"),
                    Sort.Order.asc("code")
            );
        }

        String[] parts = sort.split(",", 2);

        String field = parts[0].trim();

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            field = "name";
        }

        Sort.Direction direction =
                parts.length > 1
                && "desc".equalsIgnoreCase(parts[1].trim())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        return Sort.by(direction, field);
    }

    private FeeItemResponse toResponse(FeeItem feeItem) {
        return new FeeItemResponse(
                feeItem.getId(),
                feeItem.getUuid(),
                feeItem.getCode(),
                feeItem.getName(),
                feeItem.getCategory(),
                feeItem.isActive(),
                feeItem.getCreatedAt(),
                feeItem.getUpdatedAt()
        );
    }
}
