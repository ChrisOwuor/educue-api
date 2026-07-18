package com.owuor.educue.finance.service;

import com.owuor.educue.finance.repository.FeeLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Central authority for the document formats printed on student statements. */
@Service
@RequiredArgsConstructor
public class FinanceDocumentNumberService {
    private final FeeLedgerRepository ledgerRepository;

    @Value("${app.finance.receipt-prefix:MB02}")
    private String receiptPrefix;

    /** Receipt issued for every verified incoming payment, regardless of payer. */
    public String paymentReceipt() { return "RCT" + String.format("%08d", ledgerRepository.nextReceiptSequence()); }
    /** Student-statement document number for an ordinary payment posting. */
    public String paymentPosting() { return receiptPrefix + "-" + format(6); }
    public String invoice() { return "IN" + format(8); }
    public String debitNote() { return "DN" + format(9); }
    public String creditNote() { return "CN" + format(8); }

    private String format(int digits) {
        return String.format("%0" + digits + "d", ledgerRepository.nextDocumentSequence());
    }
}
