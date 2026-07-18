# EduCue University Management System

EduCue is a modular university management platform that connects institutional configuration, academic planning, admissions, student enrollment, unit registration, teaching assignments, examination results, student finance, reporting, and operational dashboards.

The current academic model is built around `CourseAcademicPeriod`. It does not depend on a fixed semester-only curriculum. A course defines its own ordered progression using semesters, terms, or modules, and the same progression is used consistently by admissions, enrollment, unit placement, fees, registration, results, and promotion.

This document describes the implemented backend, its business rules, and the start-to-finish workflow for each major operation. It intentionally uses numbered workflow descriptions rather than sequence diagrams.

## Contents

- [System scope](#system-scope)
- [Technology and architecture](#technology-and-architecture)
- [Domain model](#domain-model)
- [Users, roles, and permissions](#users-roles-and-permissions)
- [End-to-end institutional lifecycle](#end-to-end-institutional-lifecycle)
- [Workflow reference](#workflow-reference)
- [Finance and accounting model](#finance-and-accounting-model)
- [Documents and reports](#documents-and-reports)
- [Security model](#security-model)
- [Transactions, integrity, and concurrency](#transactions-integrity-and-concurrency)
- [API organization](#api-organization)
- [Configuration and local setup](#configuration-and-local-setup)
- [Database migrations and seed data](#database-migrations-and-seed-data)
- [Testing, monitoring, and performance](#testing-monitoring-and-performance)
- [Operational rules and production checklist](#operational-rules-and-production-checklist)

## System scope

EduCue currently provides the following functional areas:

| Area | Implemented capability |
|---|---|
| Institution | Institution profile, logo-backed reports, departments, and academic years |
| Identity and access | Users, roles, permissions, JWT authentication, account activation state, and password hashing |
| Academics | Courses, qualification types, study modes, academic periods, course progression, units, and unit placements |
| Teaching | Lecturer-to-unit-placement assignments effective for selected academic years |
| Admissions | Intakes, courses offered by an intake, public applications, document uploads, review, and approval |
| Students | Student profiles, course enrollment, current academic period, progression, and class lists |
| Registration | Current-period unit discovery, financial eligibility checks, and unit registration |
| Results | CA and examination marks, grading, result states, HOD approval, and student result access |
| Finance | Fee structures, automatic tuition invoices, payments, sponsor/HELB/bursary credits, debit notes, reversals, and calculated balances |
| Documents | Fee structures, fee statements, course/unit structures, class lists, examination lists, submission checklists, and QR-verifiable exam cards |
| Analytics | Administrator and finance dashboards, time-series summaries, and course-based reporting |

## Technology and architecture

### Backend

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA and Hibernate
- Spring Security
- PostgreSQL
- Flyway database migrations
- JJWT for signed authentication tokens
- OpenPDF for A4 institutional PDF generation
- ZXing for QR codes
- Spring Actuator and Prometheus metrics
- JUnit, Spring Boot Test, Mockito, and Spring Security Test

The backend follows a package-by-domain structure:

```text
com.owuor.educue
├── academics       courses, periods, units, placements, trainers
├── admissions      intakes, applications, approval
├── auth            login and JWT processing
├── common          security, exceptions, storage, PDF support
├── dashboard       administrator and finance analytics
├── finance         fee structures, payments, ledger
├── institution     profile, departments, academic years
├── results         marks, grading, approval, release
├── roles           roles and permissions
├── students        enrollment, registration, promotion, exam cards
└── users           staff and user-account administration
```

Controllers define the HTTP boundary, DTOs validate and shape requests, services apply business rules and transactions, repositories own persistence queries, and entities define the relational domain model. API entities are not intended to be submitted directly by clients.

### Frontend

The companion frontend is a React and TypeScript application. It consumes the `/api` endpoints with credentials enabled, renders role-specific navigation and workflows, uses Recharts for dashboards, and downloads server-generated PDFs as binary responses.

### Storage and reporting

Uploaded application files and the institution logo are served through the configured storage implementation. Professional reports use institution profile data and `storage/logo.png`. Generated reports use a neutral black, white, and gray A4 layout suitable for official printing.

## Domain model

### Academic structure

The central academic relationship is:

`Course` → ordered `CourseAcademicPeriod` → `CourseUnitPlacement` → `Unit`

- `AcademicPeriod` is a reusable definition such as `Y1S1`, `Y2T3`, or a module.
- `CourseAcademicPeriod` places an academic period at an ordered position inside one course.
- `CourseUnitPlacement` assigns a unit to a specific course academic period and records whether it is core or elective.
- `CourseAcademicPeriod.nextPeriod` supports course progression without hard-coding semester logic.

Consequently, certificate courses can use terms while degree courses use semesters without creating separate enrollment, finance, or result implementations.

### Admission and student structure

`Intake` belongs to an `AcademicYear`. `IntakeCourse` states which courses are available in that intake. An `Application` selects exactly one `IntakeCourse`, preventing an invalid intake/course combination.

Once approved, an application produces:

- a portal `User` with the `STUDENT` role;
- a permanent `Student` record;
- an active `Enrollment` linked to the selected intake course;
- the first `CourseAcademicPeriod` as the enrollment's current position;
- an initial tuition ledger invoice.

### Teaching and assessment structure

`LecturerUnitAssignment` links a lecturer to a `CourseUnitPlacement`. Its effective-from and optional effective-to academic years determine when the assignment applies. This means the assignment already identifies the course, period, and unit; those values are not duplicated on the assignment.

`StudentUnitRegistration` links an enrollment to a course unit placement and records attempt type, status, and registration time. `StudentResult` has a one-to-one relationship with the registration, so a retake can have a separate registration and result without overwriting the original attempt.

### Finance structure

`FeeStructure` belongs to an `IntakeCourse` and a `CourseAcademicPeriod`. Its line items calculate the total charge for that intake, course, and progression stage.

`Payment` represents money received and owns the official receipt number. `FeeLedger` represents the accounting effect on a student account. The current balance is never stored in a second cache table; it is calculated from the authoritative ledger:

```text
balance = SUM(debit - credit)
```

A positive balance is money owed, zero is cleared, and a negative balance is a student credit.

## Users, roles, and permissions

The seed migrations establish operational roles such as:

- `ADMIN` — system and institutional administration;
- `REGISTRAR` — admissions, enrollment, and academic records workflows;
- `HOD` — departmental oversight, class lists, and result approval;
- `TRAINER` — assigned teaching units, class/exam lists, and marks entry;
- `FINANCE` — fee structures, payments, ledgers, and finance reporting;
- `STUDENT` — own profile, units, results, finance records, and exam card.

Authorization uses both roles and fine-grained authorities. Examples include `manage_users`, `manage_courses`, `manage_intakes`, `assign_trainer`, `view_student`, `record_results`, `approve_results`, and `promote_students`.

The database is the authority for a user's active state, role, and permissions. The JWT filter reloads the user for every authenticated request, so deactivating an account takes effect immediately even if its token has not expired.

## End-to-end institutional lifecycle

The normal operational order is:

1. Configure the institution profile, departments, academic years, roles, and staff users.
2. Define reusable academic periods such as semester or term stages.
3. Create a course and arrange its ordered course academic periods.
4. Create units and place them in the appropriate course academic periods.
5. Create an intake for an academic year and add the courses offered in it.
6. Create fee structures for each intake course and course academic period.
7. Open the intake and receive public applications and supporting documents.
8. Review and approve an application.
9. EduCue atomically creates the user, student, enrollment, initial invoice, and approval record.
10. Finance records payments from the student, sponsor, HELB, or bursary provider.
11. The student registers units belonging to the enrollment's current course academic period, subject to the fee policy.
12. The HOD assigns lecturers to course unit placements for an academic year.
13. Lecturers view registered students, download examination documents, and enter marks.
14. Authorized academic officers approve results.
15. Eligible students are promoted to the next course academic period and are automatically invoiced for it.
16. When no next period exists, the enrollment has reached the end of the configured programme progression.

## Workflow reference

### 1. Institution setup

1. An administrator records the institution name, contact information, address, website, and other profile information.
2. The institution logo is placed at `storage/logo.png`.
3. Departments are created and later referenced by users, courses, reports, and dashboards.
4. Academic years are created with start/end boundaries and an active state.
5. The active academic year is loaded by the application and is used by intake and lecturer-assignment workflows.

Institution details become the common source for report headers rather than being duplicated in every PDF service.

### 2. User, role, and permission administration

1. An administrator creates or selects a role.
2. Authorities are attached to the role.
3. A staff user is created with one role and, where applicable, one department.
4. The password is BCrypt-hashed before storage.
5. The account can be activated or deactivated without deleting its audit history.
6. On login, the role and authorities become Spring Security granted authorities.

Student accounts are not normally created through staff-user administration. They are created by the admission approval transaction.

### 3. Authentication

1. The user submits an email and password to `/api/auth/login`.
2. EduCue locates the user and rejects unknown, inactive, or invalid credentials with a generic authentication error.
3. BCrypt verifies the submitted password against the stored hash.
4. A signed JWT is created with user ID, email, role, issued time, and expiry.
5. The token is returned in an HttpOnly `access_token` cookie.
6. On later requests, the JWT filter validates the signature and expiry.
7. The filter reloads the user and current authorities from PostgreSQL.
8. Controller role/authority rules decide whether the action is permitted.
9. Logout expires the authentication cookie.

### 4. Academic period and course setup

1. Academic periods are created with a code, name, position information, and period type.
2. A course is created with its department, qualification type, study mode, duration, and ordered period configuration.
3. Course creation persists the course and its `CourseAcademicPeriod` records in one transaction.
4. The single-course response returns its academic-period progression so the frontend can manage the course without guessing its structure.
5. The ordered chain determines initial admission placement and future promotion.

### 5. Unit creation and course unit placement

1. A unit is created once with its institutional code, name, and credit hours.
2. From a course, an administrator selects a course academic period.
3. One or more units are placed into that period.
4. Each placement records the unit classification, such as core or elective.
5. Duplicate or invalid placements are rejected by service and persistence rules.
6. Course structures can be downloaded for all periods or one selected period.
7. Authorized staff can generate period class lists using the course and active enrollment data.

### 6. Intake creation

1. The registrar creates an intake linked to an academic year and defines its application dates and status.
2. Courses are added through `IntakeCourse` records.
3. Only courses explicitly offered by the intake are presented to applicants.
4. Open-intake data is exposed publicly for the application form.
5. Closing an intake prevents it from appearing as an open application option.

### 7. Public application submission

1. The applicant selects an open intake and one of its offered courses.
2. Personal and contact details are validated through the application request DTO.
3. EduCue creates a unique application number.
4. The application is stored against the selected `IntakeCourse` with a pending status.
5. Supporting documents can be uploaded and associated with the application and document type.
6. Registrar users can filter and review submitted applications.

Public access is limited to the submission and document-upload endpoints needed by an applicant. Administrative review endpoints require authentication and the relevant authority.

### 8. Application approval and first enrollment

Approval is a single database transaction:

1. Load the application and verify that its status is `PENDING`.
2. Reject repeated processing and reject an application that already produced a student.
3. Resolve the selected intake, course, and first ordered course academic period.
4. Verify that the applicant email is not already used by another account.
5. Resolve the `STUDENT` role.
6. Generate a unique admission number.
7. Create a student portal user with a BCrypt-hashed temporary password and `mustChangePassword` enabled.
8. Create the permanent student record linked one-to-one to the application.
9. Create the enrollment for the selected intake course at the first course academic period.
10. Locate the fee structure for that intake course and first period.
11. Post the initial tuition invoice to the student's ledger.
12. Mark the application approved and record the approver and review timestamps.
13. Commit all records together. Any failure rolls back the entire approval.
14. After commit, publish a student-admitted event to the asynchronous admission executor.
15. The current asynchronous handlers log admission-letter generation and welcome-email delivery. They provide the integration boundary for a later serverless worker.

Because external work runs only after commit, a rolled-back admission cannot accidentally send a welcome message or generate an official letter.

### 9. Fee structure creation

1. Finance selects an intake course and one of that course's academic periods.
2. Finance adds named fee items and amounts.
3. The service calculates and stores the total.
4. The combination is used by first admission and later promotion billing.
5. Staff and students can download the applicable professional fee-structure PDF.
6. A structure that has already been used for billing should be treated as accounting reference data rather than casually edited.

### 10. Automatic tuition charging

There is no ordinary frontend action to manually issue the standard tuition charge.

Initial charging occurs during admission approval. Subsequent charging occurs during promotion:

1. EduCue resolves the applicable fee structure.
2. It checks whether that student was already billed for the same structure.
3. It calculates the fee structure total.
4. It posts a debit ledger entry using an `IN...` invoice document number.
5. The description identifies the academic period, for example `Fees Invoice For Y1S2`.
6. The posting date is the business date on which the admission or promotion transaction occurs.

The duplicate-billing guard makes retrying a workflow safe from creating a second invoice for the same structure.

### 11. Recording a payment or institutional credit

All money received—student payment, sponsor payment, HELB loan, or bursary allocation—is a `Payment`. A ledger credit is never created without its payment record.

1. Finance selects the student and payer type.
2. Finance records the amount, method, external bank/M-PESA/reference value, actual payment date/time, and optional academic period.
3. Non-student payer types require the payer's name.
4. EduCue rejects a duplicate external transaction reference.
5. EduCue rejects a future payment date.
6. A unique receipt number such as `RCT00000001` is issued.
7. The payment is stored as verified and records the finance user.
8. In the same transaction, EduCue posts the matching credit to the student's ledger.
9. An ordinary student receipt gets an `MB...` ledger document number; institutional credits use a `CN...` number.
10. The ledger description includes `Receipt Ref No.RCT...`, providing direct traceability to the payment record.

`receiptNumber` identifies the money-received record. `documentNumber` identifies the accounting posting. They are deliberately different identifiers.

#### M-PESA C2B processing

Safaricom confirmations enter through a durable inbox rather than posting directly to the student ledger:

1. Safaricom calls `/api/payments/mpesa/callback/{token}/confirmation`.
2. EduCue validates required fields, amount, transaction time, short code, and the high-entropy callback token.
3. PostgreSQL atomically inserts the transaction using `TransID` as the idempotency key and immediately acknowledges accepted callbacks.
4. Duplicate deliveries are acknowledged but do not create another inbox row, receipt, payment, or ledger credit.
5. A scheduled worker atomically claims batches with `FOR UPDATE SKIP LOCKED`, allowing multiple application instances to process the queue safely.
6. `BillRefNumber` must contain the student's admission number.
7. A matched event creates an M-PESA payment, `RCT...` receipt, and linked ledger credit in one transaction.
8. An unmatched admission number moves to `REVIEW`; it is never silently credited to another account.
9. Transient failures retry with exponential backoff. Exhausted retries move to review.
10. Finance users inspect `/api/finance/mpesa/events` and may retry a corrected/recoverable event.
11. A worker crash is recovered by returning stale processing claims to the retry queue.

The inbox does not retain payer names or the full phone number. It stores only a peppered SHA-256 phone hash and the final four digits for controlled reconciliation. Application logs identify internal event IDs and do not log callback bodies.

#### M-PESA STK Push

STK Push is available from the student's current fee-structure page and uses the same settlement inbox as PayBill:

1. The authenticated student selects a whole-shilling amount up to the current calculated ledger balance and enters a Kenyan M-PESA number.
2. The frontend sends a unique idempotency key, preventing a repeated browser request from creating multiple logical initiations.
3. EduCue validates student ownership, phone format, amount, and outstanding balance before contacting Daraja.
4. EduCue obtains and briefly caches a Daraja OAuth token, constructs the Lipa na M-PESA password, and sends `CustomerPayBillOnline` to Safaricom.
5. The UI reports `PENDING` and polls the student's own request status; an accepted push is not treated as payment.
6. Safaricom sends the result to the token-protected STK callback URL.
7. Cancellation, timeout, or another non-zero result marks the request failed and does not affect finance.
8. A successful callback must include a receipt, transaction time, and exactly the initiated amount.
9. The receipt enters the idempotent M-PESA inbox and is processed into one payment, one `RCT...` receipt, and one ledger credit.
10. Only the phone hash and final four digits are stored; EduCue never requests or receives the customer's M-PESA PIN.

Required production variables are `MPESA_BASE_URL`, `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_SHORT_CODE`, `MPESA_STK_PASSKEY`, `MPESA_STK_CALLBACK_TOKEN`, `MPESA_CALLBACK_TOKEN`, and `MPESA_PHONE_HASH_PEPPER`. The public base URL must be HTTPS and reachable by Safaricom.

### 12. Manual debit notes

Manual debit is reserved for exceptional charges such as resits, retakes, penalties, damages, graduation fees, or another approved charge.

1. Finance selects the student, reason, amount, posting date, description details, and optional external approval reference.
2. Future posting dates are rejected.
3. EduCue creates a `DN...` document number.
4. The amount is posted as a debit and immediately affects the calculated balance.
5. The finance user and immutable creation time remain available for audit.

Standard tuition must continue to use the automatic admission/promotion invoice workflow rather than a manual debit.

### 13. Reversing a ledger entry

Posted accounting records are not deleted or overwritten.

1. Finance selects the incorrect ledger entry and provides a reason and reversal posting date.
2. EduCue rejects an entry already reversed.
3. The reversal date cannot precede the original posting date and cannot be in the future.
4. EduCue creates a new entry with the exact opposite debit/credit values.
5. The new entry receives a `CN...` document number and references the original document in its description.
6. The original entry is marked `REVERSED` but remains visible.
7. If the original entry came from a payment, the associated payment is also marked `REVERSED` in the same transaction.
8. The ledger balance changes only through the new opposite entry, preserving a complete audit trail.

### 14. Ledger statements and balances

1. Ledger rows are read in `posting date`, `recorded timestamp`, then `ID` order.
2. The running balance is calculated row by row as debit minus credit.
3. No cached student-finance balance table is maintained.
4. The statement presents posting date, document number, description, debit, credit, and balance.
5. Backdated entries appear at their correct business date while `createdAt` retains evidence of when they were actually entered.

This design prevents the ledger and a separate balance table from drifting apart.

### 15. Lecturer assignment

1. An authorized user opens the allocation view, which joins course unit placements with any matching lecturer assignment.
2. An unassigned placement returns a null lecturer and can be assigned.
3. The user selects the lecturer, effective-from academic year, optional effective-to academic year, start year, and enabled state.
4. The effective academic-year range is validated against the assignment.
5. The assignment records who performed it, when it was performed, and its optimistic-lock version.
6. A lecturer's “My Teaching” view returns only units assigned to that authenticated lecturer for the applicable year.

### 16. Student unit registration

1. EduCue resolves the authenticated student's active enrollment.
2. Available units are loaded from placements in the enrollment's current course academic period.
3. The service validates that submitted placement IDs belong to that current course period.
4. The finance service calculates charges and credits directly from the ledger.
5. The configured policy requires the student to satisfy the minimum payment threshold before registration.
6. Existing active registrations are not duplicated.
7. Each accepted unit creates an active `StudentUnitRegistration` with its attempt type and timestamp.
8. Registered units become the source for lecturer class lists, result entry, exam documents, and the exam card.

### 17. Teaching lists and examination documents

For an assigned course unit placement, an authorized trainer, HOD, or administrator can:

1. Load active registered students.
2. Download a students list.
3. Download an examination marks-entry sheet containing CA and exam columns for physical entry.
4. Download an examination submission checklist where students sign after submitting an examination paper.

These are examination-control documents, not ordinary classroom-attendance registers.

### 18. Marks entry and grading

1. The lecturer opens an assigned unit's marks-entry page.
2. EduCue loads active registrations and any existing result for each registration.
3. The lecturer enters CA marks, examination marks, and remarks.
4. Bean validation constrains marks to valid numeric ranges.
5. The entity derives total marks from CA plus examination marks; clients cannot set the total independently.
6. `GradingService` calculates the grade and pass state.
7. A new result begins in `DRAFT`; subsequent workflow states include submission and approval.
8. Optimistic locking prevents silent overwriting when two users edit the same result concurrently.

### 19. Result approval and student access

1. A lecturer records and submits results according to the result workflow.
2. An authorized HOD or academic officer selects a result batch.
3. Batch status processing records the approving user and approval time.
4. Approved/released result rules determine what students may view.
5. Promotion considers only approved, passed core-unit results.
6. Withheld or incomplete results do not satisfy promotion eligibility.

### 20. Promotion and subsequent charging

1. The promotion table loads active enrollments and registrations for each current course academic period.
2. A candidate must have registered units.
3. Every core unit must have a result that is both passed and approved.
4. Ineligible students are skipped and retain their current period.
5. EduCue follows `currentCourseAcademicPeriod.nextPeriod`.
6. If a next period exists, the enrollment moves to it.
7. EduCue resolves the fee structure for the same intake course and the next period.
8. The next-period tuition invoice is posted in the same transaction.
9. If no next period exists, the student has reached the configured end of the programme and is not advanced further.

### 21. Exam card issuance and verification

1. The authenticated student requests the exam-card PDF.
2. EduCue loads the current enrollment period and active registered units.
3. Generation fails if there are no active examination units.
4. EduCue reuses or creates one exam-card record for the student and period.
5. The A4 PDF states that the student is authorized to sit the listed examinations.
6. It includes institution details, admission number, course, period, units, issue time, and a QR code.
7. The QR code points to the public verification endpoint using a random verification UUID.
8. Verification reloads current active registrations rather than trusting data embedded in the PDF.
9. A card is reported valid only while it matches the student's current period and has active registered units.

### 22. Dashboards

The finance dashboard aggregates collections, invoices, balances, payment trends, and course-level figures. The administrator dashboard summarizes students, applications, courses, operational activity, and time-series trends. Course graphs are based on database results and therefore accommodate newly created courses without frontend constants.

Dashboard queries are read-only and must not be used as the accounting source of truth; finance balances always come from ledger sums.

## Finance and accounting model

### Document conventions

| Record | Example | Meaning |
|---|---|---|
| Payment receipt | `RCT00000001` | Official receipt attached to every verified incoming payment |
| Payment ledger posting | `MB02-100001` | Student payment line on the account statement |
| Fee invoice | `IN00100002` | Tuition invoice generated from a fee structure |
| Debit note | `DN000100003` | Exceptional/manual student charge |
| Credit note | `CN00100004` | Institutional credit or reversal posting |

The `MB02` prefix is configurable through `FINANCE_RECEIPT_PREFIX`. Historical document numbers are retained rather than rewritten.

### Dates

Finance distinguishes three concepts:

| Field | Purpose |
|---|---|
| `paidAt` | Business date/time when funds were actually received |
| `postingDate` | Accounting date used on the student statement |
| `createdAt` | Immutable system timestamp showing when EduCue recorded the event |

The frontend submits institutional local time without converting a selected value to browser UTC. The backend rejects future business dates. Ordering is deterministic even when several transactions share a posting date.

### Accounting invariants

- Charges are debits.
- Payments and allocations are credits.
- Every incoming credit has one payment record and one linked ledger credit.
- Standard tuition invoices originate from a fee structure.
- An invoice for the same student and fee structure cannot be duplicated.
- A gateway or external payment reference is unique.
- A posted entry is corrected using a reversal, never deletion.
- Reversing a payment ledger line also reverses the linked payment status.
- Student balance is always derived from ledger totals.

## Documents and reports

The reporting layer provides or supports:

- individual and combined fee-structure PDFs;
- student fee statements;
- course unit structures for one or all academic periods;
- course-period class lists based on active enrollments;
- trainer students lists;
- examination marks-entry sheets;
- examination submission checklists;
- QR-verifiable student examination cards;
- finance ledger exports.

Reports use institution-profile information, the stored logo where present, A4 page sizing, restrained grayscale styling, table headers, and page footers. Access to each report follows the same role or ownership checks as its underlying data.

## Security model

### Implemented controls

- BCrypt password hashing; raw passwords are not stored.
- Signed, expiring JWTs.
- JWT stored in an HttpOnly cookie, reducing access from browser JavaScript.
- Stateless Spring Security request processing.
- Database user reload on every request for immediate deactivation and permission changes.
- Role and authority checks using `@PreAuthorize` on sensitive operations.
- DTO validation for required fields, numeric limits, and identifiers.
- Generic invalid-login responses that do not reveal whether an email exists.
- Configured CORS allowlist rather than unrestricted origins.
- Unique constraints for admission numbers, application numbers, payment references, receipts, and ledger documents.
- Transactional admission, payment/credit, reversal, and promotion operations.
- Optimistic locking on contested records such as results and lecturer assignments.
- Non-destructive finance reversals and immutable audit timestamps.
- Random UUIDs for externally referenced records and exam-card verification.
- Public endpoints limited to login, open-intake/application functions, payment callback, health/metrics, and exam-card verification.
- File-size limits for multipart requests.
- Central exception handling for consistent API errors.

### Production hardening requirements

Before public production deployment:

- set the authentication cookie `Secure` flag to `true` and serve only through HTTPS;
- keep `HttpOnly` and use an appropriate production `SameSite` policy;
- reassess CSRF protection if cookie authentication remains the browser transport;
- use a long, random JWT signing secret from a secrets manager, never source control;
- narrow CORS to the exact production frontend origin;
- remove or disable development quick-login controls and seeded common passwords;
- rotate all seed credentials;
- protect Prometheus if it is not isolated by the deployment network;
- add explicit fine-grained authorization to any configuration CRUD endpoint currently protected only by global authentication;
- add rate limiting to login, public application, upload, and public verification endpoints;
- validate upload content type, scan uploaded files, and prevent executable content from being served inline;
- move production documents to controlled object storage with private access or signed URLs;
- add structured security/audit events for login, approval, finance, results, and permission changes;
- configure backups, point-in-time recovery, retention, and restore testing.

## Transactions, integrity, and concurrency

Service operations use transactions at business boundaries. Important atomic operations include:

- application approval, student creation, enrollment, and first billing;
- payment creation and corresponding ledger credit;
- ledger reversal and linked payment reversal;
- student promotion and next-period billing;
- unit registration batches;
- marks persistence and grading.

If a required record is missing or a business validation fails, the transaction rolls back. External admission tasks use an `AFTER_COMMIT` event listener and therefore run only after durable database success.

Database constraints complement service validation. IDs used outside internal joins commonly have UUID alternatives, and chronological finance queries include stable tie-breakers. Flyway owns schema evolution; Hibernate is configured to validate the schema rather than silently redesign it.

## API organization

All application APIs use the `/api` prefix.

| Prefix | Responsibility |
|---|---|
| `/api/auth` | login, current user, logout |
| `/api/users`, `/api/roles` | user and access administration |
| `/api/institution-profile` | institution configuration |
| `/api/departments`, `/api/academic-years` | institutional reference data |
| `/api/courses`, `/api/academic-periods`, `/api/units` | academic configuration |
| `/api/course-unit-placements`, `/api/courses/{uuid}/unit-placements` | course structures and class lists |
| `/api/trainer-assignments` | lecturer allocation and “My Teaching” |
| `/api/intakes`, `/api/applications` | admissions |
| `/api/enrollments`, `/api/academic/promotions` | student records and progression |
| `/api/student` | authenticated student's own profile, units, registration, finance, and documents |
| `/api/unit-registrations` | staff views, trainer lists, and examination exports |
| `/api/results`, `/api/trainer/results` | result entry, review, and student results |
| `/api/finance/fee-structures` | fee structure management and downloads |
| `/api/finance/payments` | incoming payment recording and search |
| `/api/finance/ledger` | debit notes, reversals, statements, balances, and export |
| `/api/dashboards` | admin and finance analytics |
| `/api/public/exam-cards` | public QR verification |

Consult controller classes for the exact request fields and endpoint-level authorities. The DTO layer is the contract source for validation and response structure.

## Configuration and local setup

### Prerequisites

- JDK 21
- PostgreSQL 17 or another version supported by the configured driver/Flyway version
- Maven wrapper included in this repository
- Node.js and npm for the companion frontend

### Database

Create an empty PostgreSQL database and user, then grant that user ownership or migration privileges for the schema.

### Application configuration

Use [application-example.yml](src/main/resources/application-example.yml) as the non-secret template. Configure at least:

- database URL, username, and password;
- JWT signing secret and expiry;
- allowed frontend origins;
- bootstrap administrator credentials;
- local or external storage location;
- public base URL used by QR verification;
- optional finance document prefix.

Never commit real database passwords, JWT secrets, cloud credentials, or production bootstrap passwords.

### Run the backend

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

Flyway validates and applies pending migrations before the application becomes available. The default backend port is `8080`.

### Run tests

```powershell
.\mvnw.cmd test
```

### Frontend integration

Configure the React application to use the backend base URL, send requests with credentials, and run it on an origin listed in `app.cors.allowed-origins`. During local development this is commonly `http://localhost:5173`.

## Database migrations and seed data

Migrations live in `src/main/resources/db/migration` and must remain immutable after being applied to a shared environment. Add a new, incremented migration for every schema or controlled-data change.

The migration history covers:

- identity, roles, and permissions;
- institution and academic reference data;
- courses, units, academic periods, placements, and lecturer assignments;
- intakes, applications, students, and enrollments;
- course-academic-period migration and removal of the legacy curriculum/semester model;
- fee structures, payments, and production ledger rules;
- unit registration, results, and exam cards;
- seed reference and staff data;
- removal of cached finance balances;
- standardized finance receipt numbers and business/audit dates.

`V31__seed_initial_reference_and_staff_data.sql` is intended for initializing development or controlled demonstration environments. Its shared credentials must not be retained in production.

To reset a development database, recreate the database/schema and allow Flyway to run from V1. Do not edit an already-applied migration merely to make a reset behave differently.

### Fresh production baseline

New production installations use the squashed migrations under `src/main/resources/db/production` by activating the `production` Spring profile. This baseline represents the final validated schema after historical migrations V1–V40 but exposes only nine clean, category-oriented migrations.

```powershell
$env:SPRING_PROFILES_ACTIVE="production"
.\mvnw.cmd spring-boot:run
```

The production profile changes Flyway's location to `classpath:db/production`, requires database/security/provider secrets from the environment, keeps Hibernate in validation mode, and suppresses detailed public health information. It must be used only with a new empty database. Existing environments with historical Flyway records must remain on `classpath:db/migration`.

The production baseline seeds roles, permissions, and their mappings only. It does not seed departments, courses, units, academic periods, institution data, staff accounts, students, or finance records. The first administrator is created from `BOOTSTRAP_ADMIN_*` environment variables.

## Testing, monitoring, and performance

### Automated testing

The repository currently contains Spring context and dashboard integration coverage. High-risk workflows should continue to receive focused tests for:

- concurrent/double admission approval;
- duplicate payment references;
- payment and ledger atomicity;
- debit and reversal date validation;
- reversal of linked payments;
- balance calculation and backdated statement ordering;
- registration fee eligibility;
- duplicate unit registration;
- result validation and optimistic locking;
- promotion eligibility and rollback when the next fee structure is absent;
- role and ownership authorization.

### Health and metrics

- `/actuator/health` exposes application health.
- `/actuator/prometheus` exposes Prometheus-format metrics.

In production, route metrics through a private monitoring network or authentication boundary.

### Admissions load simulation

The `performance/k6` directory contains the admissions workload and its instructions. The script is intended to simulate paced application creation against a test environment. Never run load tests against production without an approved test window, isolated test identities, monitoring, and cleanup procedures.

`performance/k6/mpesa-webhooks.js` sends 1,000 concurrent-style C2B confirmations through 200 virtual users. Supply `BASE_URL`, `MPESA_CALLBACK_TOKEN`, `MPESA_SHORT_CODE`, and a test `ADMISSION_NUMBER`. Run it only against an isolated test database because every unique confirmation is real accounting test data.

```powershell
k6 run -e BASE_URL=http://localhost:8080 -e MPESA_CALLBACK_TOKEN=your-test-token `
  -e MPESA_SHORT_CODE=600000 -e ADMISSION_NUMBER=TEST/0001/2026 performance/k6/mpesa-webhooks.js
```

## Operational rules and production checklist

### Rules that must remain true

- Course progression is defined only through ordered course academic periods.
- Enrollment's current course academic period is the student's academic position.
- Units are assigned through course unit placements, not directly to a generic semester.
- Lecturer assignments target course unit placements and academic-year ranges.
- Unit registration must target the enrollment's current course period.
- Promotion requires approved passes for all registered core units.
- Admission and promotion automatically create tuition invoices.
- Every incoming credit creates a payment and a linked ledger credit.
- Financial balance comes only from ledger sums.
- Accounting mistakes are reversed, not deleted.
- Official documents obtain institution details from the institution profile and logo storage.

### Deployment checklist

1. Provision PostgreSQL, backups, and restricted database credentials.
2. Supply secrets through environment variables or a secrets manager.
3. Configure HTTPS, secure cookies, CORS, and the public base URL.
4. Review public endpoints and add gateway rate limits.
5. Run Flyway validation and migrations before application traffic is enabled.
6. Create or rotate the bootstrap administrator and remove demonstration access.
7. Configure the institution profile and upload the official logo.
8. Verify departments, academic years, roles, and permissions.
9. Verify each course's period progression before opening an intake.
10. Confirm fee structures exist for every period that admission or promotion can enter.
11. Run backend tests and a frontend production build.
12. Perform an end-to-end test: application, approval, invoice, payment, registration, marks, approval, promotion, and next invoice.
13. Verify PDF downloads and QR URLs from the public production hostname.
14. Connect health, metrics, logs, alerting, and audit retention.

## Project status

EduCue implements the full central student lifecycle from institutional setup and application through finance, registration, assessment, and academic progression. The admission-letter and welcome-email boundary is intentionally asynchronous but currently logs the intended work; connecting that boundary to a production worker/email provider remains an integration task. Security-sensitive production settings and the test coverage items listed above should be completed as part of deployment readiness.
