package com.owuor.educue.finance.mpesa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface MpesaStkPushRepository extends JpaRepository<MpesaStkPushRequest, Long> {
    Optional<MpesaStkPushRequest> findByIdempotencyKey(UUID key);
    Optional<MpesaStkPushRequest> findByUuidAndStudentUserId(UUID uuid, Long userId);
    Optional<MpesaStkPushRequest> findByCheckoutRequestId(String checkoutRequestId);
}

