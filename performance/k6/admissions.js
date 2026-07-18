import http from "k6/http";
import { check, fail } from "k6";
import exec from "k6/execution";

const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/$/, "");
const APPLICATION_COUNT = Number.parseInt(__ENV.APPLICATION_COUNT || "100", 10);
const INTERVAL_SECONDS = Number.parseInt(__ENV.INTERVAL_SECONDS || "180", 10);
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

if (!Number.isInteger(APPLICATION_COUNT) || APPLICATION_COUNT < 1) {
  throw new Error("APPLICATION_COUNT must be a positive integer");
}
if (!Number.isInteger(INTERVAL_SECONDS) || INTERVAL_SECONDS < 1) {
  throw new Error("INTERVAL_SECONDS must be a positive integer");
}

export const options = {
  scenarios: {
    applications: {
      executor: "constant-arrival-rate",
      rate: 1,
      timeUnit: `${INTERVAL_SECONDS}s`,
      duration: `${APPLICATION_COUNT * INTERVAL_SECONDS}s`,
      preAllocatedVUs: 1,
      maxVUs: 5,
      gracefulStop: "30s",
    },
  },
  thresholds: {
    "http_req_failed{endpoint:submit-application}": ["rate<0.01"],
    "http_req_duration{endpoint:submit-application}": ["p(95)<2000"],
    checks: ["rate>0.99"],
    dropped_iterations: ["count==0"],
  },
};

/**
 * Resolve application targets once at the start. This follows the same public
 * discovery flow as the application page and avoids fragile database IDs in
 * the script. An intake must be open, before its deadline, and have courses.
 */
export function setup() {
  const response = http.get(`${BASE_URL}/api/intakes/open`, {
    tags: { endpoint: "open-intakes" },
  });

  if (!check(response, { "open intakes loaded": (res) => res.status === 200 })) {
    exec.test.abort(`Could not load open intakes: HTTP ${response.status} ${response.body}`);
  }

  let intakes;
  try {
    intakes = response.json();
  } catch (_) {
    exec.test.abort("The open-intakes endpoint did not return valid JSON");
  }

  const intakeCourses = (intakes || []).flatMap((intake) =>
    (intake.courses || []).map((course) => ({
      intakeCourseId: course.intakeCourseId,
      intake: intake.name,
      course: course.name,
    })),
  );

  if (intakeCourses.length === 0) {
    exec.test.abort(
      "No course is attached to an open intake. Create/open an intake and attach at least one course before running k6.",
    );
  }

  console.log(
    `Submitting ${APPLICATION_COUNT} applications, one every ${INTERVAL_SECONDS}s, across ${intakeCourses.length} intake course(s).`,
  );
  return { intakeCourses };
}

export default function (data) {
  const sequence = exec.scenario.iterationInTest + 1;
  const target = data.intakeCourses[(sequence - 1) % data.intakeCourses.length];
  const suffix = `${RUN_ID}-${String(sequence).padStart(3, "0")}`;
  const payload = JSON.stringify({
    intakeCourseId: target.intakeCourseId,
    fullName: `K6 Test Applicant ${sequence}`,
    email: `k6.applicant.${suffix}@example.test`,
    phone: `+2547${String(sequence).padStart(8, "0").slice(-8)}`,
    nationalId: `K6-${suffix}`,
    dateOfBirth: "2000-01-15",
    guardianName: `K6 Guardian ${sequence}`,
    guardianPhone: `+254710${String(sequence).padStart(6, "0").slice(-6)}`,
  });

  const response = http.post(`${BASE_URL}/api/applications`, payload, {
    headers: { "Content-Type": "application/json" },
    tags: { endpoint: "submit-application" },
  });

  const accepted = check(response, {
    "application accepted": (res) => res.status === 200 || res.status === 201,
    "application number returned": (res) => {
      try {
        return Boolean(res.json("applicationNumber"));
      } catch (_) {
        return false;
      }
    },
  });

  if (!accepted) {
    fail(
      `Application ${sequence} failed for ${target.intake} / ${target.course}: HTTP ${response.status} ${response.body}`,
    );
  }
}
