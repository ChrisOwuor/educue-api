import http from "k6/http";
import { check } from "k6";

export const options = {
  scenarios: {
    simultaneous_confirmations: {
      executor: "shared-iterations",
      vus: 200,
      iterations: 1000,
      maxDuration: "60s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1000", "p(99)<2000"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const callbackToken = __ENV.MPESA_CALLBACK_TOKEN || "replace-this-callback-token";
const shortCode = __ENV.MPESA_SHORT_CODE || "600000";
const admissionNumber = __ENV.ADMISSION_NUMBER;

export default function () {
  if (!admissionNumber) throw new Error("ADMISSION_NUMBER is required");
  const unique = `${__VU}`.padStart(3, "0") + `${__ITER}`.padStart(7, "0");
  const payload = JSON.stringify({
    TransactionType: "Pay Bill",
    TransID: `K6${unique}`.slice(0, 20),
    TransTime: "20260718120000",
    TransAmount: "10.00",
    BusinessShortCode: shortCode,
    BillRefNumber: admissionNumber,
    InvoiceNumber: "",
    ThirdPartyTransID: "",
    MSISDN: "254700000000",
  });
  const response = http.post(
    `${baseUrl}/api/payments/mpesa/callback/${callbackToken}/confirmation`,
    payload,
    { headers: { "Content-Type": "application/json" } },
  );
  check(response, { "callback accepted": (r) => r.status === 200 && r.json("ResultCode") === 0 });
}
