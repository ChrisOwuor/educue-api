package com.owuor.educue.finance.mpesa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MpesaConfirmationRequest(
        @JsonProperty("TransactionType") String transactionType,
        @JsonProperty("TransID") String transactionId,
        @JsonProperty("TransTime") String transactionTime,
        @JsonProperty("TransAmount") String amount,
        @JsonProperty("BusinessShortCode") String businessShortCode,
        @JsonProperty("BillRefNumber") String accountReference,
        @JsonProperty("InvoiceNumber") String invoiceNumber,
        @JsonProperty("ThirdPartyTransID") String thirdPartyTransactionId,
        @JsonProperty("MSISDN") String msisdn
) {}

