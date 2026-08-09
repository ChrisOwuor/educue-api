package com.owuor.educue.finance;

import com.owuor.educue.finance.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class FinanceSearchIntegrationTest {
    @Autowired PaymentService payments;

    @Test
    void paymentPageAcceptsOpenDateRange() {
        assertThatCode(() -> payments.searchPayments(null, null, null, null, null,
                null, null, 0, 50, "paidAt,desc")).doesNotThrowAnyException();
    }
}
