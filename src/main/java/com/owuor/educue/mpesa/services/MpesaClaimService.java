package com.owuor.educue.mpesa.services;

import com.owuor.educue.mpesa.repositories.MpesaPaymentEventRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class MpesaClaimService {
    private final MpesaPaymentEventRepository repository;

    @Transactional
    public List<Long> claim(int limit) { return repository.claimBatch(limit); }

    @Transactional
    public void recoverStale() {
        repository.recoverStale(LocalDateTime.now().minusMinutes(5));
    }
}
