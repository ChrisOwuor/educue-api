package com.owuor.educue.finance.claims;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public interface DebitClaimBatchRepository extends JpaRepository<DebitClaimBatch,Long>{
 Optional<DebitClaimBatch> findByUuid(UUID uuid);
 List<DebitClaimBatch> findAllByOrderByCreatedAtDesc();
 @Modifying @Query(value="""
  UPDATE debit_claim_batches SET status='PROCESSING' WHERE id=(SELECT id FROM debit_claim_batches WHERE status='QUEUED' ORDER BY confirmed_at,id FOR UPDATE SKIP LOCKED LIMIT 1) RETURNING id
 """,nativeQuery=true) List<Long> claimNext();
}
