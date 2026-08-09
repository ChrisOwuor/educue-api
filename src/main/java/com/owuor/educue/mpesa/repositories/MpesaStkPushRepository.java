package com.owuor.educue.mpesa.repositories;

import com.owuor.educue.mpesa.entities.MpesaStkPushRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface MpesaStkPushRepository extends JpaRepository<MpesaStkPushRequest, Long> {
    Optional<MpesaStkPushRequest> findByIdempotencyKey(UUID key);
    Optional<MpesaStkPushRequest> findByUuidAndStudentUserId(UUID uuid, Long userId);
    Optional<MpesaStkPushRequest> findByCheckoutRequestId(String checkoutRequestId);
}


