package com.owuor.educue.finance.claims;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.List;
@Service @RequiredArgsConstructor public class DebitClaimQueueService {private final DebitClaimBatchRepository repository;@Transactional public List<Long> claimNext(){return repository.claimNext();}}
