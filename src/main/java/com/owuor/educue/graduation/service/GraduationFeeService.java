package com.owuor.educue.graduation.service;

import com.owuor.educue.academics.enums.QualificationType;
import com.owuor.educue.admissions.entity.Intake;
import com.owuor.educue.admissions.repository.IntakeRepository;
import com.owuor.educue.finance.entity.FeeItem;
import com.owuor.educue.finance.entity.GraduationFeeItem;
import com.owuor.educue.finance.repository.FeeItemRepository;
import com.owuor.educue.finance.repository.GraduationFeeItemRepository;
import com.owuor.educue.graduation.dto.GraduationFeeStructureResponse;
import com.owuor.educue.graduation.dto.SaveGraduationFeeStructureRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GraduationFeeService {
    private final GraduationFeeItemRepository repository;
    private final FeeItemRepository feeItemRepository;
    private final IntakeRepository intakeRepository;

    @Transactional(readOnly = true)
    public GraduationFeeStructureResponse getStructure(QualificationType type, Long intakeId) {
        Intake intake = intake(intakeId);
        return response(type, intake, repository.findEffective(type, intake.getSequenceNumber()));
    }

    @Transactional(readOnly = true)
    public List<GraduationFeeItem> effectiveRules(QualificationType type, Intake intake) {
        return repository.findEffective(type, intake.getSequenceNumber());
    }

    @Transactional
    public GraduationFeeStructureResponse saveStructure(SaveGraduationFeeStructureRequest request) {
        Intake selected = intake(request.intakeId());
        long sequence = selected.getSequenceNumber();
        LinkedHashMap<UUID, SaveGraduationFeeStructureRequest.Item> desired = new LinkedHashMap<>();
        Map<UUID, Integer> displayOrders = new HashMap<>();
        int requestedOrder = 10;
        for (var line : request.items()) {
            if (desired.put(line.feeItemUuid(), line) != null) throw new IllegalArgumentException("A fee item can only appear once");
            displayOrders.put(line.feeItemUuid(), requestedOrder);
            requestedOrder += 10;
        }
        Map<UUID, FeeItem> feeItems = feeItemRepository.findAllByUuidIn(desired.keySet()).stream()
                .collect(Collectors.toMap(FeeItem::getUuid, Function.identity()));
        if (feeItems.size() != desired.size()) throw new IllegalArgumentException("One or more fee items were not found");
        feeItems.values().forEach(item -> { if (!item.isActive()) throw new IllegalArgumentException("Inactive fee items cannot be configured"); });

        List<GraduationFeeItem> effective = repository.findEffectiveForUpdate(request.qualificationType(), sequence);
        for (GraduationFeeItem current : effective) {
            var wanted = desired.remove(current.getFeeItem().getUuid());
            if (wanted == null) {
                closeOrDelete(current, selected, sequence);
                continue;
            }
            BigDecimal amount = normalize(wanted.amount());
            current.setDisplayOrder(displayOrders.get(wanted.feeItemUuid()));
            if (normalize(current.getAmount()).compareTo(amount) == 0) continue;
            if (current.getEffectiveFromIntake().getSequenceNumber() == sequence) {
                current.setAmount(amount);
            } else {
                Intake oldEnd = current.getEffectiveToIntake();
                current.setEffectiveToIntake(selected);
                repository.save(newRule(request.qualificationType(), current.getFeeItem(), amount, selected, oldEnd,
                        displayOrders.get(wanted.feeItemUuid())));
            }
        }
        for (var wanted : desired.values()) {
            FeeItem feeItem = feeItems.get(wanted.feeItemUuid());
            Intake end = repository.findNext(request.qualificationType(), feeItem.getUuid(), sequence)
                    .map(GraduationFeeItem::getEffectiveFromIntake).orElse(null);
            repository.save(newRule(request.qualificationType(), feeItem, normalize(wanted.amount()), selected, end,
                    displayOrders.get(wanted.feeItemUuid())));
        }
        repository.flush();
        return response(request.qualificationType(), selected, repository.findEffective(request.qualificationType(), sequence));
    }

    private void closeOrDelete(GraduationFeeItem current, Intake selected, long sequence) {
        if (current.getEffectiveFromIntake().getSequenceNumber() == sequence) repository.delete(current);
        else current.setEffectiveToIntake(selected);
    }

    private GraduationFeeItem newRule(QualificationType type, FeeItem feeItem, BigDecimal amount,
                                      Intake from, Intake to, int order) {
        GraduationFeeItem rule = new GraduationFeeItem();
        rule.setQualificationType(type); rule.setFeeItem(feeItem); rule.setAmount(amount);
        rule.setEffectiveFromIntake(from); rule.setEffectiveToIntake(to); rule.setDisplayOrder(order);
        return rule;
    }

    private GraduationFeeStructureResponse response(QualificationType type, Intake intake, List<GraduationFeeItem> rules) {
        List<GraduationFeeStructureResponse.Item> items = rules.stream().map(rule ->
                new GraduationFeeStructureResponse.Item(rule.getId(), rule.getFeeItem().getUuid(), rule.getFeeItem().getCode(),
                        rule.getFeeItem().getName(), rule.getFeeItem().getCategory(), normalize(rule.getAmount()),
                        !rule.getEffectiveFromIntake().getId().equals(intake.getId()), rule.getEffectiveFromIntake().getId(),
                        rule.getEffectiveFromIntake().getName())).toList();
        BigDecimal total = items.stream().map(GraduationFeeStructureResponse.Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GraduationFeeStructureResponse(type, intake.getId(), intake.getName(), intake.getSequenceNumber(), total, items);
    }

    private Intake intake(Long id) { return intakeRepository.findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Intake not found")); }
    private BigDecimal normalize(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
